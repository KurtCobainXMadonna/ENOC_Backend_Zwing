package org.eci.ZwingBackend.auth.application.service;

import org.eci.ZwingBackend.auth.application.port.out.RefreshTokenStorePort;
import org.eci.ZwingBackend.auth.application.port.out.TokenGeneratorPort;
import org.eci.ZwingBackend.auth.application.port.out.UserRepositoryAuthOutPort;
import org.eci.ZwingBackend.auth.domain.model.RefreshTokenData;
import org.eci.ZwingBackend.auth.domain.model.TokenPair;
import org.eci.ZwingBackend.auth.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenStorePort store;

    @Mock
    private TokenGeneratorPort accessTokenGenerator;

    @Mock
    private UserRepositoryAuthOutPort userRepository;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(store, accessTokenGenerator, userRepository, 7200L);
    }

    @Test
    void issueStoresHashedTokenAndReturnsRawToken() {
        UUID userId = UUID.randomUUID();
        User user = user(userId, "User", "user@example.com");

        String rawToken = refreshTokenService.issue(user);

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<RefreshTokenData> dataCaptor = ArgumentCaptor.forClass(RefreshTokenData.class);
        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        verify(store).save(hashCaptor.capture(), dataCaptor.capture(), ttlCaptor.capture());

        assertThat(rawToken).isNotBlank();
        assertThat(hashCaptor.getValue()).hasSize(64);
        assertThat(dataCaptor.getValue()).isEqualTo(new RefreshTokenData(userId, emailOf(user)));
        assertThat(ttlCaptor.getValue()).isEqualTo(7200L);
    }

    @Test
    void refreshRejectsMissingToken() {
        assertThatThrownBy(() -> refreshTokenService.refresh(" "))
                .isInstanceOf(RefreshTokenService.InvalidRefreshTokenException.class)
                .hasMessage("Missing refresh token");
    }

    @Test
    void refreshRejectsUnknownToken() {
        when(store.find(org.mockito.ArgumentMatchers.anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.refresh("refresh"))
                .isInstanceOf(RefreshTokenService.InvalidRefreshTokenException.class)
                .hasMessage("Refresh token not recognized");
    }

    @Test
    void refreshRejectsWhenUserNoLongerExists() {
        String rawToken = "refresh-token";
        UUID userId = UUID.randomUUID();
        when(store.find(RefreshTokenService.hash(rawToken))).thenReturn(Optional.of(new RefreshTokenData(userId, "user@example.com")));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.refresh(rawToken))
                .isInstanceOf(RefreshTokenService.InvalidRefreshTokenException.class)
                .hasMessage("User no longer exists");

        verify(store).delete(RefreshTokenService.hash(rawToken));
        verify(accessTokenGenerator, never()).generateToken(any());
    }

    @Test
    void refreshReturnsNewTokenPair() {
        String rawToken = "refresh-token";
        UUID userId = UUID.randomUUID();
        User user = user(userId, "User", "user@example.com");

        when(store.find(RefreshTokenService.hash(rawToken))).thenReturn(Optional.of(new RefreshTokenData(userId, emailOf(user))));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accessTokenGenerator.generateToken(user)).thenReturn("new-access");

        TokenPair tokenPair = refreshTokenService.refresh(rawToken);

        assertThat(tokenPair.accessToken()).isEqualTo("new-access");
        assertThat(tokenPair.refreshToken()).isNotBlank();
        verify(store).delete(RefreshTokenService.hash(rawToken));
        verify(store).save(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(new RefreshTokenData(userId, emailOf(user))), org.mockito.ArgumentMatchers.eq(7200L));
    }

    @Test
    void revokeIgnoresBlankToken() {
        refreshTokenService.revoke("   ");

        verify(store, never()).delete(any());
    }

    @Test
    void revokeDeletesHashedToken() {
        refreshTokenService.revoke("raw");

        verify(store).delete(RefreshTokenService.hash("raw"));
    }

    @Test
    void revokeAllForUserDelegatesToStore() {
        UUID userId = UUID.randomUUID();

        refreshTokenService.revokeAllForUser(userId);

        verify(store).deleteAllForUser(userId);
    }

    @Test
    void hashIsDeterministic() {
        assertThat(RefreshTokenService.hash("same")).isEqualTo(RefreshTokenService.hash("same"));
        assertThat(RefreshTokenService.hash("same")).hasSize(64);
    }

    private static User user(UUID userId, String name, String email) {
        try {
            java.lang.reflect.Constructor<User> constructor = User.class.getDeclaredConstructor(UUID.class, String.class, String.class);
            constructor.setAccessible(true);
            return constructor.newInstance(userId, name, email);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static String emailOf(User user) {
        return (String) field(user, "email");
    }

    private static Object field(Object target, String fieldName) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

}