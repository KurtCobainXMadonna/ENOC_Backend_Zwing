package org.eci.ZwingBackend.auth.application.service;

import org.eci.ZwingBackend.auth.application.port.out.GoogleAuthPort;
import org.eci.ZwingBackend.auth.application.port.out.TokenGeneratorPort;
import org.eci.ZwingBackend.auth.application.port.out.UserRepositoryAuthOutPort;
import org.eci.ZwingBackend.auth.domain.model.GoogleUserData;
import org.eci.ZwingBackend.auth.domain.model.User;
import org.eci.ZwingBackend.auth.infraestructure.web.dto.response.AuthResponse;
import org.eci.ZwingBackend.shared.events.UserDeletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAuthServiceTest {

    @Mock
    private GoogleAuthPort googleAuthPort;

    @Mock
    private UserRepositoryAuthOutPort userRepository;

    @Mock
    private TokenGeneratorPort tokenGeneratorPort;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RefreshTokenService refreshTokenService;

    private UserAuthService userAuthService;

    @BeforeEach
    void setUp() {
        userAuthService = new UserAuthService(
                googleAuthPort,
                userRepository,
                tokenGeneratorPort,
                eventPublisher,
                refreshTokenService);
    }

    @Test
    void authenticateReturnsExistingUserTokens() {
        GoogleUserData googleUserData = googleUserData("gid", "user@example.com", "User", "avatar");
        User existingUser = user(UUID.randomUUID(), "User", "user@example.com");

        when(googleAuthPort.verifyToken("token")).thenReturn(googleUserData);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existingUser));
        when(tokenGeneratorPort.generateToken(existingUser)).thenReturn("access");
        when(refreshTokenService.issue(existingUser)).thenReturn("refresh");

        AuthResponse response = userAuthService.authenticate("token");

        assertThat(booleanField(response, "isNewUser")).isFalse();
        assertThat(stringField(response, "name")).isEqualTo("User");
        assertThat(stringField(response, "email")).isEqualTo("user@example.com");
        assertThat(stringField(response, "token")).isEqualTo("access");
        assertThat(stringField(response, "refreshToken")).isEqualTo("refresh");
        verify(userRepository, never()).save(any());
    }

    @Test
    void authenticateCreatesNewUserWhenMissing() {
        GoogleUserData googleUserData = googleUserData("gid", "new@example.com", "New User", "avatar");

        when(googleAuthPort.verifyToken("token")).thenReturn(googleUserData);
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tokenGeneratorPort.generateToken(any(User.class))).thenReturn("access");
        when(refreshTokenService.issue(any(User.class))).thenReturn("refresh");

        AuthResponse response = userAuthService.authenticate("token");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User createdUser = userCaptor.getValue();

        assertThat(stringField(createdUser, "name")).isEqualTo("New User");
        assertThat(stringField(createdUser, "email")).isEqualTo("new@example.com");
        assertThat(booleanField(response, "isNewUser")).isTrue();
        assertThat(stringField(response, "name")).isEqualTo("New User");
        assertThat(stringField(response, "email")).isEqualTo("new@example.com");
    }

    @Test
    void deleteUserRemovesUserAndPublishesEvent() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, "User", "user@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userAuthService.deleteUser(userId);

        verify(userRepository).delete(user);
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(UserDeletedEvent.class);
    }

    @Test
    void deleteUserFailsWhenMissing() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userAuthService.deleteUser(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not found");

        verify(userRepository, never()).delete(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void logoutRevokesRefreshToken() {
        userAuthService.logout("refresh-token");

        verify(refreshTokenService).revoke("refresh-token");
    }

    private static User user(UUID userId, String name, String email) {
        try {
            Constructor<User> constructor = User.class.getDeclaredConstructor(UUID.class, String.class, String.class);
            constructor.setAccessible(true);
            return constructor.newInstance(userId, name, email);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static GoogleUserData googleUserData(String googleId, String email, String name, String avatarUrl) {
        try {
            Constructor<GoogleUserData> constructor = GoogleUserData.class.getDeclaredConstructor(String.class, String.class, String.class, String.class);
            constructor.setAccessible(true);
            return constructor.newInstance(googleId, email, name, avatarUrl);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static String stringField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (String) field.get(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static boolean booleanField(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.getBoolean(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

}