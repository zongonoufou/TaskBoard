package com.example.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.mapper.UserMapper;
import com.example.model.UserModel;
import com.example.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Charge l'utilisateur depuis la base pour Spring Security (login + filtre JWT).
 */
@Service
@RequiredArgsConstructor
public class UserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        UserModel user = userRepository.findByEmailIgnoreCase(UserMapper.normalizeEmail(email))
                .orElseThrow(() -> new UsernameNotFoundException("Aucun compte pour l'email : " + email));
        return new UserPrincipal(user);
    }
}
