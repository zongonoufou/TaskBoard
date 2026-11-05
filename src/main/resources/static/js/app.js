/* ============================================================
   TaskBoard — logique de l'interface (JavaScript natif, sans build)
   ============================================================ */

const API = '/api';
const TOKEN_KEY = 'taskboard.token';
const USER_KEY = 'taskboard.user';
const THEME_KEY = 'taskboard.theme';

const STATUSES = [
    { id: 'A_FAIRE', label: 'À faire', className: 'a-faire' },
    { id: 'EN_COURS', label: 'En cours', className: 'en-cours' },
    { id: 'TERMINE', label: 'Terminé', className: 'termine' }
];

const PRIORITY_LABELS = {
    BASSE: { label: 'Basse', css: 'low' },
    MOYENNE: { label: 'Moyenne', css: 'medium' },
    HAUTE: { label: 'Haute', css: 'high' },
    URGENTE: { label: 'Urgente', css: 'urgent' }
};

const state = {
    token: localStorage.getItem(TOKEN_KEY) || null,
    user: JSON.parse(localStorage.getItem(USER_KEY) || 'null'),
    boards: [],
    board: null,
    tasks: [],
    search: '',
    editingTask: null,
    editingBoard: null
};

/* ---------------------------- Utilitaires DOM ---------------------------- */
const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];

const escapeHtml = (value) => String(value ?? '').replace(/[&<>"']/g, (ch) => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
}[ch]));

function toast(message, type = 'info') {
    const el = document.createElement('div');
    el.className = `toast ${type}`;
    el.textContent = message;
    $('#toasts').appendChild(el);
    setTimeout(() => {
        el.classList.add('leaving');
        setTimeout(() => el.remove(), 220);
    }, 3200);
}

function initials(name) {
    if (!name) return '?';
    return name.trim().split(/\s+/).slice(0, 2).map((part) => part[0].toUpperCase()).join('');
}

function formatDate(iso) {
    if (!iso) return '';
    const date = new Date(`${iso}T00:00:00`);
    if (Number.isNaN(date.getTime())) return '';
    return date.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short' });
}

function dueMeta(task) {
    if (!task.dueDate) return null;
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const due = new Date(`${task.dueDate}T00:00:00`);
    const days = Math.round((due - today) / 86400000);
    let cls = '';
    if (task.status !== 'TERMINE' && days < 0) cls = 'late';
    else if (days === 0) cls = 'today';
    const text = days === 0 ? "Aujourd'hui" : days < 0 ? `Retard ${Math.abs(days)} j` : formatDate(task.dueDate);
    return { cls, text, days };
}

/* ---------------------------- Appels API ---------------------------- */
async function api(path, { method = 'GET', body, auth = true } = {}) {
    const headers = { 'Content-Type': 'application/json' };
    if (auth && state.token) headers.Authorization = `Bearer ${state.token}`;

    const response = await fetch(`${API}${path}`, {
        method,
        headers,
        body: body === undefined ? undefined : JSON.stringify(body)
    });

    if (response.status === 401 && auth) {
        logout(false);
        throw new Error('Session expirée, veuillez vous reconnecter.');
    }

    if (response.status === 204) return null;

    const text = await response.text();
    const data = text ? safeParse(text) : null;

    if (!response.ok) {
        const message = data?.message
            || (data?.validationErrors && Object.values(data.validationErrors)[0])
            || `Erreur ${response.status}`;
        throw new Error(message);
    }
    return data;
}

function safeParse(text) {
    try { return JSON.parse(text); } catch { return null; }
}

/* ---------------------------- Authentification ---------------------------- */
function saveSession(payload) {
    state.token = payload.token;
    state.user = payload.user || null;
    localStorage.setItem(TOKEN_KEY, payload.token);
    if (payload.user) localStorage.setItem(USER_KEY, JSON.stringify(payload.user));
}

function logout(notify = true) {
    state.token = null;
    state.user = null;
    state.boards = [];
    state.board = null;
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    $('#shell').classList.add('hidden');
    $('#auth-screen').classList.remove('hidden');
    if (notify) toast('Vous êtes déconnecté.', 'info');
}

function showShell() {
    $('#auth-screen').classList.add('hidden');
    $('#shell').classList.remove('hidden');
    const user = state.user || {};
    $('#user-name').textContent = user.name || 'Utilisateur';
    $('#user-email').textContent = user.email || '';
    $('#user-avatar').textContent = initials(user.name);
    $('#hello-name').textContent = (user.name || '').split(' ')[0] || '👋';
}

/* ---------------------------- Abonnements ---------------------------- */
function initAuthForms() {
    $$('[data-auth-tab]').forEach((tab) => {
        tab.addEventListener('click', () => {
            $$('[data-auth-tab]').forEach((t) => t.classList.toggle('active', t === tab));
            const isLogin = tab.dataset.authTab === 'login';
            $('#login-form').classList.toggle('hidden', !isLogin);
            $('#register-form').classList.toggle('hidden', isLogin);
            $('#auth-error').textContent = '';
        });
    });

    $('#login-form').addEventListener('submit', async (event) => {
        event.preventDefault();
        const form = event.currentTarget;
        const button = $('button[type=submit]', form);
        setLoading(button, true);
        $('#auth-error').textContent = '';
        try {
            const payload = await api('/auth/login', {
                method: 'POST',
                auth: false,
                body: {
                    email: form.email.value.trim(),
                    password: form.password.value
                }
            });
            saveSession(payload);
            await enterApp();
        } catch (error) {
            $('#auth-error').textContent = error.message;
        } finally {
            setLoading(button, false);
        }
    });

    $('#register-form').addEventListener('submit', async (event) => {
        event.preventDefault();
        const form = event.currentTarget;
        const button = $('button[type=submit]', form);
        setLoading(button, true);
        $('#auth-error').textContent = '';
        try {
            const payload = await api('/auth/register', {
                method: 'POST',
                auth: false,
                body: {
                    name: form.name.value.trim(),
                    email: form.email.value.trim(),
                    password: form.password.value
                }
            });
            saveSession(payload);
            toast('Bienvenue sur TaskBoard !', 'success');
            await enterApp();
        } catch (error) {
            $('#auth-error').textContent = error.message;
        } finally {
            setLoading(button, false);
        }
    });

    $('#demo-login').addEventListener('click', async () => {
        const demo = { email: 'demo@taskboard.dev', password: 'demo1234' };
        try {
            const payload = await api('/auth/login', { method: 'POST', auth: false, body: demo });
            saveSession(payload);
            await enterApp();
            toast('Compte de démonstration connecté.', 'success');
        } catch (error) {
            $('#auth-error').textContent =
                "Compte de démo indisponible : lancez l'application avec le profil dev (-Dspring-boot.run.profiles=dev).";
        }
    });

    $('#logout-btn').addEventListener('click', () => logout());
}

function setLoading(button, loading) {
    if (!button) return;
    const spinner = $('.spinner', button);
    const label = $('.btn-label', button);
    button.disabled = loading;
    spinner?.classList.toggle('hidden', !loading);
    if (label) label.style.opacity = loading ? '.6' : '1';
}

/* ---------------------------- Navigation ---------------------------- */
async function enterApp() {
    showShell();
    await loadBoards();
    if (state.boards.length) {
        openBoard(state.boards[0].id);
    } else {
        showDashboard();
    }
}

function showDashboard() {
    state.board = null;
    $('#view-dashboard').classList.remove('hidden');
    $('#view-board').classList.add('hidden');
    $('#empty-state').classList.add('hidden');
    $('#quick-add').hidden = true;
    $('#crumbs').textContent = 'Tableau de bord';
    $$('.board-link').forEach((el) => el.classList.remove('active'));
    loadDashboard();
}

async function loadDashboard() {
    try {
        const stats = await api('/dashboard/stats');
        $('#stat-boards').textContent = stats.boardCount;
        $('#stat-tasks').textContent = stats.taskCount;
        $('#stat-done').textContent = stats.completedCount;
        $('#stat-late').textContent = stats.overdueCount;
        renderStatusBars(stats.tasksByStatus || {});
        renderUpcoming(stats.upcomingTasks || []);
        renderBoardsGrid();
    } catch (error) {
        toast(error.message, 'error');
    }
}

function renderStatusBars(byStatus) {
    const total = Object.values(byStatus).reduce((sum, value) => sum + Number(value), 0) || 1;
    $('#status-bars').innerHTML = STATUSES.map((status) => {
        const value = Number(byStatus[status.id] || 0);
        const percent = Math.round((value / total) * 100);
        return `
            <div class="bar-row">
                <div class="bar-label"><span>${status.label}</span><span>${value}</span></div>
                <div class="bar-track"><div class="bar-fill ${status.className}" style="width:${percent}%"></div></div>
            </div>`;
    }).join('');
}

function renderUpcoming(tasks) {
    if (!tasks.length) {
        $('#upcoming').innerHTML = '<li class="muted">Aucune échéance à venir. 🎉</li>';
        return;
    }
    $('#upcoming').innerHTML = tasks.map((task) => {
        const meta = dueMeta(task);
        return `<li>
            <span class="dot ${task.priority === 'URGENTE' ? 'pink' : 'indigo'}"></span>
            <span>${escapeHtml(task.title)}</span>
            <span class="when ${meta?.cls || ''}">${meta ? escapeHtml(meta.text) : ''}</span>
        </li>`;
    }).join('');
}

function renderBoardsGrid() {
    const grid = $('#boards-grid');
    if (!state.boards.length) {
        grid.innerHTML = '<p class="muted small">Aucun tableau : créez-en un pour démarrer.</p>';
        return;
    }
    grid.innerHTML = state.boards.map((board) => {
        const counts = board.taskCountByStatus || {};
        const total = STATUSES.reduce((sum, status) => sum + Number(counts[status.id] || 0), 0) || 1;
        const segments = STATUSES.map((status) => {
            const percent = (Number(counts[status.id] || 0) / total) * 100;
            const color = status.id === 'TERMINE' ? 'var(--success)'
                : status.id === 'EN_COURS' ? 'var(--primary)' : '#94a3b8';
            return `<span style="width:${percent}%;background:${color}"></span>`;
        }).join('');
        return `
            <article class="board-card" data-board-id="${board.id}" style="--accent:${escapeHtml(board.color || '#6366f1')}">
                <h4>${escapeHtml(board.title)}</h4>
                <p>${escapeHtml(board.description || 'Pas de description')}</p>
                <div class="mini-bar">${segments}</div>
                <div class="meta"><span>${board.taskCount ?? 0} tâche(s)</span>
                    <span class="spacer"></span>
                    <span>${counts.TERMINE ?? 0} terminée(s)</span>
                </div>
            </article>`;
    }).join('');
    $$('.board-card', grid).forEach((card) => {
        card.addEventListener('click', () => openBoard(Number(card.dataset.boardId)));
    });
}

async function loadBoards() {
    state.boards = await api('/boards');
    renderBoardsList();
    renderBoardsGrid();
}

function renderBoardsList() {
    const list = $('#boards-list');
    if (!state.boards.length) {
        list.innerHTML = '<p class="muted small">Aucun tableau pour l\'instant.</p>';
        return;
    }
    list.innerHTML = state.boards.map((board) => `
        <button class="board-link ${state.board?.id === board.id ? 'active' : ''}" data-board-id="${board.id}">
            <span class="swatch-dot" style="background:${escapeHtml(board.color || '#6366f1')}"></span>
            <span>${escapeHtml(board.title)}</span>
            <span class="count">${board.taskCount ?? 0}</span>
        </button>`).join('');
    $$('.board-link', list).forEach((button) => {
        button.addEventListener('click', () => {
            openBoard(Number(button.dataset.boardId));
            closeSidebar();
        });
    });
}

/* ---------------------------- Vue tableau ---------------------------- */
async function openBoard(boardId) {
    try {
        const [board, tasks] = await Promise.all([
            api(`/boards/${boardId}`),
            api(`/boards/${boardId}/tasks`)
        ]);
        state.board = board;
        state.tasks = tasks;
        $('#view-dashboard').classList.add('hidden');
        $('#empty-state').classList.add('hidden');
        $('#view-board').classList.remove('hidden');
        $('#quick-add').hidden = false;
        $('#crumbs').textContent = board.title;
        $('#board-title').textContent = board.title;
        $('#board-desc').textContent = board.description || '';
        $('#board-color').style.background = board.color || 'var(--primary)';
        $$('.board-link').forEach((el) =>
            el.classList.toggle('active', Number(el.dataset.boardId) === boardId));
        renderKanban();
    } catch (error) {
        toast(error.message, 'error');
    }
}

function visibleTasks() {
    const search = state.search.trim().toLowerCase();
    if (!search) return state.tasks;
    return state.tasks.filter((task) =>
        task.title.toLowerCase().includes(search)
        || (task.description || '').toLowerCase().includes(search)
        || (task.assignee || '').toLowerCase().includes(search));
}

function renderKanban() {
    const kanban = $('#kanban');
    const tasks = visibleTasks();
    kanban.innerHTML = STATUSES.map((status) => {
        const items = tasks
            .filter((task) => task.status === status.id)
            .sort((a, b) => (a.position ?? 0) - (b.position ?? 0));
        return `
            <section class="column" data-status="${status.id}">
                <header class="column-head">
                    <span class="name">${status.label}</span>
                    <span class="badge">${items.length}</span>
                    <button class="icon-btn add" data-add-task="${status.id}" title="Ajouter une tâche">+</button>
                </header>
                ${items.length ? items.map(renderCard).join('') : '<p class="column-empty">Déposez une carte ici</p>'}
            </section>`;
    }).join('');

    bindKanbanEvents();
}

function renderCard(task) {
    const priority = PRIORITY_LABELS[task.priority] || PRIORITY_LABELS.MOYENNE;
    const meta = dueMeta(task);
    return `
        <article class="card ${task.status === 'TERMINE' ? 'done' : ''}" draggable="true" data-task-id="${task.id}">
            <div class="quick-actions">
                <button class="icon-btn" data-toggle="${task.id}" title="Marquer terminé / à faire">✓</button>
                <button class="icon-btn" data-edit="${task.id}" title="Modifier">✎</button>
            </div>
            <span class="chip ${priority.css}">${priority.label}</span>
            <p class="card-title">${escapeHtml(task.title)}</p>
            ${task.description ? `<p class="card-desc">${escapeHtml(task.description)}</p>` : ''}
            <div class="card-foot">
                ${task.assignee ? `<span class="chip assignee">${escapeHtml(task.assignee)}</span>` : ''}
                ${meta ? `<span class="date ${meta.cls}">⏱ ${escapeHtml(meta.text)}</span>` : ''}
            </div>
        </article>`;
}

function bindKanbanEvents() {
    $$('.card').forEach((card) => {
        const taskId = Number(card.dataset.taskId);

        card.addEventListener('dragstart', (event) => {
            card.classList.add('dragging');
            event.dataTransfer.setData('text/plain', String(taskId));
            event.dataTransfer.effectAllowed = 'move';
        });
        card.addEventListener('dragend', () => card.classList.remove('dragging'));

        card.addEventListener('click', (event) => {
            if (event.target.closest('.quick-actions')) return;
            const task = state.tasks.find((t) => t.id === taskId);
            if (task) openTaskModal(task);
        });
    });

    $$('[data-toggle]').forEach((button) => {
        button.addEventListener('click', async (event) => {
            event.stopPropagation();
            try {
                await api(`/boards/${state.board.id}/tasks/${button.dataset.toggle}/toggle`, { method: 'PATCH' });
                await refreshTasks();
            } catch (error) { toast(error.message, 'error'); }
        });
    });

    $$('[data-edit]').forEach((button) => {
        button.addEventListener('click', (event) => {
            event.stopPropagation();
            const task = state.tasks.find((t) => t.id === Number(button.dataset.edit));
            if (task) openTaskModal(task);
        });
    });

    $$('[data-add-task]').forEach((button) => {
        button.addEventListener('click', () => openTaskModal(null, button.dataset.addTask));
    });

    $$('.column').forEach((column) => {
        column.addEventListener('dragover', (event) => {
            event.preventDefault();
            column.classList.add('drag-over');
        });
        column.addEventListener('dragleave', () => column.classList.remove('drag-over'));
        column.addEventListener('drop', async (event) => {
            event.preventDefault();
            column.classList.remove('drag-over');
            const taskId = Number(event.dataTransfer.getData('text/plain'));
            const status = column.dataset.status;
            const task = state.tasks.find((t) => t.id === taskId);
            if (!task || task.status === status) return;

            const targetPosition = state.tasks.filter((t) => t.status === status).length;
            // Mise à jour optimiste
            task.status = status;
            renderKanban();
            try {
                await api(`/boards/${state.board.id}/tasks/${taskId}/move`, {
                    method: 'PATCH',
                    body: { status, position: targetPosition }
                });
                await refreshTasks();
            } catch (error) {
                toast(error.message, 'error');
                await refreshTasks();
            }
        });
    });
}

async function refreshTasks() {
    if (!state.board) return;
    state.tasks = await api(`/boards/${state.board.id}/tasks`);
    renderKanban();
    await loadBoards();
}

/* ---------------------------- Modale tâche ---------------------------- */
function openTaskModal(task, presetStatus = 'A_FAIRE') {
    state.editingTask = task || null;
    const form = $('#task-form');
    form.reset();
    $('#task-modal-title').textContent = task ? 'Modifier la tâche' : 'Nouvelle tâche';
    $('#delete-task-btn').classList.toggle('hidden', !task);
    $('#task-submit').textContent = task ? 'Enregistrer' : 'Créer la tâche';

    form.title.value = task?.title || '';
    form.description.value = task?.description || '';
    form.status.value = task?.status || presetStatus;
    form.priority.value = task?.priority || 'MOYENNE';
    form.dueDate.value = task?.dueDate || '';
    form.assignee.value = task?.assignee || '';

    $('#task-modal').showModal();
    setTimeout(() => form.title.focus(), 60);
}

async function submitTask(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const payload = {
        title: form.title.value.trim(),
        description: form.description.value.trim(),
        status: form.status.value,
        priority: form.priority.value,
        dueDate: form.dueDate.value || null,
        assignee: form.assignee.value.trim() || null
    };

    try {
        if (state.editingTask) {
            await api(`/boards/${state.board.id}/tasks/${state.editingTask.id}`, { method: 'PUT', body: payload });
            toast('Tâche mise à jour.', 'success');
        } else {
            await api(`/boards/${state.board.id}/tasks`, { method: 'POST', body: payload });
            toast('Tâche créée.', 'success');
        }
        $('#task-modal').close();
        await refreshTasks();
    } catch (error) {
        toast(error.message, 'error');
    }
}

async function deleteTask() {
    if (!state.editingTask) return;
    if (!confirm('Supprimer cette tâche ?')) return;
    try {
        await api(`/boards/${state.board.id}/tasks/${state.editingTask.id}`, { method: 'DELETE' });
        $('#task-modal').close();
        toast('Tâche supprimée.', 'success');
        await refreshTasks();
    } catch (error) {
        toast(error.message, 'error');
    }
}

/* ---------------------------- Modale tableau ---------------------------- */
function openBoardModal(board = null) {
    state.editingBoard = board;
    const form = $('#board-form');
    form.reset();
    $('#board-modal-title').textContent = board ? 'Modifier le tableau' : 'Nouveau tableau';
    form.title.value = board?.title || '';
    form.description.value = board?.description || '';
    form.color.value = board?.color || '#6366f1';
    selectSwatch(form.color.value);
    $('#board-modal').showModal();
    setTimeout(() => form.title.focus(), 60);
}

function selectSwatch(color) {
    $$('.swatch').forEach((swatch) =>
        swatch.classList.toggle('active', swatch.dataset.color.toLowerCase() === String(color).toLowerCase()));
}

async function submitBoard(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const payload = {
        title: form.title.value.trim(),
        description: form.description.value.trim(),
        color: form.color.value.trim()
    };
    try {
        if (state.editingBoard) {
            await api(`/boards/${state.editingBoard.id}`, { method: 'PUT', body: payload });
            toast('Tableau mis à jour.', 'success');
            $('#board-modal').close();
            await loadBoards();
            await openBoard(state.editingBoard.id);
        } else {
            const created = await api('/boards', { method: 'POST', body: payload });
            toast('Tableau créé.', 'success');
            $('#board-modal').close();
            await loadBoards();
            await openBoard(created.id);
        }
    } catch (error) {
        toast(error.message, 'error');
    }
}

async function deleteBoard() {
    if (!state.board) return;
    if (!confirm(`Supprimer le tableau « ${state.board.title} » et toutes ses tâches ?`)) return;
    try {
        await api(`/boards/${state.board.id}`, { method: 'DELETE' });
        toast('Tableau supprimé.', 'success');
        state.board = null;
        await loadBoards();
        if (state.boards.length) await openBoard(state.boards[0].id);
        else showEmptyState();
    } catch (error) {
        toast(error.message, 'error');
    }
}

function showEmptyState() {
    $('#view-board').classList.add('hidden');
    $('#view-dashboard').classList.add('hidden');
    $('#empty-state').classList.remove('hidden');
    $('#quick-add').hidden = true;
    $('#crumbs').textContent = 'Bienvenue';
}

/* ---------------------------- Thème / responsive ---------------------------- */
function applyTheme(theme) {
    document.documentElement.dataset.theme = theme;
    localStorage.setItem(THEME_KEY, theme);
    $('#theme-label').textContent = theme === 'dark' ? 'Thème clair' : 'Thème sombre';
}

function toggleTheme() {
    applyTheme(document.documentElement.dataset.theme === 'dark' ? 'light' : 'dark');
}

function closeSidebar() {
    $('#sidebar').classList.remove('open');
}

/* ---------------------------- Démarrage ---------------------------- */
function init() {
    applyTheme(localStorage.getItem(THEME_KEY) || 'light');

    initAuthForms();
    $('#theme-toggle').addEventListener('click', toggleTheme);
    $('#open-sidebar').addEventListener('click', () => $('#sidebar').classList.add('open'));
    $('#close-sidebar').addEventListener('click', closeSidebar);

    $('#new-board-btn').addEventListener('click', () => openBoardModal());
    $('#dash-new-board').addEventListener('click', () => openBoardModal());
    $('#empty-create-board').addEventListener('click', () => openBoardModal());
    $('#edit-board-btn').addEventListener('click', () => openBoardModal(state.board));
    $('#delete-board-btn').addEventListener('click', deleteBoard);
    $('#quick-add').addEventListener('click', () => openTaskModal(null, 'A_FAIRE'));

    $('#board-form').addEventListener('submit', submitBoard);
    $('#task-form').addEventListener('submit', submitTask);
    $('#delete-task-btn').addEventListener('click', deleteTask);

    $$('.swatch').forEach((swatch) => {
        swatch.addEventListener('click', () => {
            $('#board-form').color.value = swatch.dataset.color;
            selectSwatch(swatch.dataset.color);
        });
    });
    $('#board-form').color.addEventListener('input', (event) => selectSwatch(event.target.value));

    $$('[data-close]').forEach((button) => {
        button.addEventListener('click', () => button.closest('dialog').close());
    });

    $$('.nav-item[data-view="dashboard"]').forEach((button) => {
        button.addEventListener('click', () => {
            showDashboard();
            closeSidebar();
        });
    });

    let searchTimer;
    $('#search-input').addEventListener('input', (event) => {
        state.search = event.target.value;
        clearTimeout(searchTimer);
        searchTimer = setTimeout(() => {
            if (!$('#view-board').classList.contains('hidden')) renderKanban();
        }, 120);
    });

    // Revalidation du token au démarrage
    if (state.token) {
        showShell();
        api('/users/me')
            .then((user) => {
                state.user = user;
                localStorage.setItem(USER_KEY, JSON.stringify(user));
                enterApp();
            })
            .catch(() => logout(false));
    } else {
        $('#auth-screen').classList.remove('hidden');
    }

    // Raccourci clavier : "n" pour nouvelle tâche
    document.addEventListener('keydown', (event) => {
        if (event.key === 'n' && !event.target.matches('input, textarea, select')
            && !$('#view-board').classList.contains('hidden')) {
            openTaskModal(null, 'A_FAIRE');
        }
    });
}

document.addEventListener('DOMContentLoaded', init);
