package com.codelens.backend.service;

import com.codelens.backend.dto.AuthResponse;
import com.codelens.backend.dto.LoginRequest;
import com.codelens.backend.dto.RegisterRequest;
import com.codelens.backend.entity.User;
import com.codelens.backend.repository.UserRepository;
import com.codelens.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_createsUser_whenEmailNotTaken() {
        RegisterRequest request = new RegisterRequest("Ada Lovelace", "ada@example.com", "password123");
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(jwtService.generateToken("ada@example.com")).thenReturn("fake-jwt-token");

        AuthResponse response = authService.register(request);

        assertEquals("fake-jwt-token", response.token());
        assertEquals("ada@example.com", response.email());
        assertEquals("Ada Lovelace", response.fullName());
        verify(userRepository).save(argThat(u ->
                u.getEmail().equals("ada@example.com")
                        && u.getPasswordHash().equals("hashed-password")
                        && u.getFullName().equals("Ada Lovelace")
        ));
    }

    @Test
    void register_throws_whenEmailAlreadyRegistered() {
        RegisterRequest request = new RegisterRequest("Ada", "ada@example.com", "password123");
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(request));
        assertEquals("Email is already registered", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_returnsToken_whenCredentialsValid() {
        LoginRequest request = new LoginRequest("ada@example.com", "password123");
        User existingUser = new User(1L, "Ada Lovelace", "ada@example.com", "hashed-password", null);
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken("ada@example.com")).thenReturn("fake-jwt-token");

        AuthResponse response = authService.login(request);

        assertEquals("fake-jwt-token", response.token());
        assertEquals("ada@example.com", response.email());
    }

    @Test
    void login_throws_whenEmailNotFound() {
        LoginRequest request = new LoginRequest("nobody@example.com", "password123");
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login(request));
        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    void login_throws_whenPasswordIncorrect() {
        LoginRequest request = new LoginRequest("ada@example.com", "wrong-password");
        User existingUser = new User(1L, "Ada Lovelace", "ada@example.com", "hashed-password", null);
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login(request));
        assertEquals("Invalid email or password", ex.getMessage());
    }
}