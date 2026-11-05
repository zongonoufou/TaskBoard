package com.example.dto;

import com.example.model.TaskPriority;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskPriorityDTO {

    @NotNull(message = "La priorité est obligatoire (BASSE, MOYENNE, HAUTE, URGENTE)")
    private TaskPriority priority;
}
