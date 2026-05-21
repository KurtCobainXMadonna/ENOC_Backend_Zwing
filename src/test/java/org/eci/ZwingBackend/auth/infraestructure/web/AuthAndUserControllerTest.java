package org.eci.ZwingBackend.auth.infraestructure.web;

import jakarta.servlet.http.Cookie;
import org.eci.ZwingBackend.auth.application.port.in.AuthenticateWithGoogleUseCase;
import org.eci.ZwingBackend.auth.application.port.in.LogoutUseCase;
import org.eci.ZwingBackend.auth.application.port.in.RefreshSessionUseCase;
import org.eci.ZwingBackend.auth.application.port.in.UserDeleteCase;
import org.eci.ZwingBackend.auth.application.port.out.UserRepositoryAuthOutPort;
import org.eci.ZwingBackend.auth.infraestructure.adapters.out.JwtTokenAdapter;
import org.eci.ZwingBackend.auth.domain.model.TokenPair;
import org.eci.ZwingBackend.auth.domain.model.User;
import org.eci.ZwingBackend.auth.infraestructure.web.dto.request.GoogleAuthRequest;
import org.eci.ZwingBackend.auth.infraestructure.web.dto.response.AuthResponse;
import org.eci.ZwingBackend.auth.infraestructure.web.dto.response.UserResponse;
import org.eci.ZwingBackend.shared.dto.GeneralResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthAndUserControllerTest {

    @Mock
    private AuthenticateWithGoogleUseCase authenticateUseCase;

    @Mock
    private LogoutUseCase logoutUseCase;

    @Mock
    private RefreshSessionUseCase refreshSessionUseCase;

    @Mock
    private JwtTokenAdapter accessTokenAdapter;

    @Mock
    private org.eci.ZwingBackend.auth.application.service.RefreshTokenService refreshTokenService;

    @Mock
    private UserRepositoryAuthOutPort userRepository;

    @Mock
    private UserDeleteCase userDeleteCase;

    private AuthController authController;
    private UserController userController;

    @BeforeEach
    void setUp() {
        authController = new AuthController(authenticateUseCase, logoutUseCase, refreshSessionUseCase, accessTokenAdapter, refreshTokenService);
        userController = new UserController(userRepository, userDeleteCase);
    }

    @Test
    void authenticateWithGoogleSetsCookiesAndBody() {
        GoogleAuthRequest request = new GoogleAuthRequest();
        request.setIdToken("google-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthResponse authResponse = new AuthResponse("access-token", "refresh-token", "Ada", "ada@example.com", true);
        when(authenticateUseCase.authenticate("google-token")).thenReturn(authResponse);
        when(accessTokenAdapter.getExpirationSeconds()).thenReturn(900L);
        when(refreshTokenService.ttlSeconds()).thenReturn(3600L);

        ResponseEntity<GeneralResponse<AuthResponse>> entity = authController.authenticateWithGoogle(request, response);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getMessage()).isEqualTo("User Registered Successfully");
        assertThat(entity.getBody().getData()).isEqualTo(authResponse);
        assertThat(response.getHeaders("Set-Cookie")).hasSize(2);
        assertThat(response.getHeaders("Set-Cookie").get(0)).contains("jwt_token=access-token");
        assertThat(response.getHeaders("Set-Cookie").get(1)).contains("refresh_token=refresh-token");
    }

    @Test
    void refreshReturnsUnauthorizedWhenCookieMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseEntity<GeneralResponse<Void>> entity = authController.refresh(request, response);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(entity.getBody()).isNotNull();
        assertThat(entity.getBody().getMessage()).isEqualTo("Missing refresh token");
        verify(refreshSessionUseCase, never()).refresh(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void refreshRotatesCookiesOnSuccess() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refresh_token", "raw-refresh"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(refreshSessionUseCase.refresh("raw-refresh")).thenReturn(new TokenPair("new-access", "new-refresh"));
        when(accessTokenAdapter.getExpirationSeconds()).thenReturn(900L);
        when(refreshTokenService.ttlSeconds()).thenReturn(3600L);

        ResponseEntity<GeneralResponse<Void>> entity = authController.refresh(request, response);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody().getMessage()).isEqualTo("Session refreshed");
        assertThat(response.getHeaders("Set-Cookie")).hasSize(2);
        assertThat(response.getHeaders("Set-Cookie").get(0)).contains("jwt_token=new-access");
        assertThat(response.getHeaders("Set-Cookie").get(1)).contains("refresh_token=new-refresh");
    }

    @Test
    void logoutAlwaysExpiresCookies() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("refresh_token", "raw-refresh"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        ResponseEntity<GeneralResponse<String>> entity = authController.logout(request, response);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody().getMessage()).isEqualTo("Successfully logged out");
        verify(logoutUseCase).logout("raw-refresh");
        assertThat(response.getHeaders("Set-Cookie")).hasSize(2);
    }

    @Test
    void getCurrentUserReturnsCurrentProfile() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "Ada", "ada@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        ResponseEntity<GeneralResponse<UserResponse>> entity = userController.getCurrentUser(userId);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(entity.getBody().getMessage()).isEqualTo("Session active");
        assertThat(entity.getBody().getData().getName()).isEqualTo("Ada");
        assertThat(entity.getBody().getData().getEmail()).isEqualTo("ada@example.com");
    }

    @Test
    void lookupReturnsUserIdWhenEmailExists() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(new User(userId, "Ada", "ada@example.com")));

        assertThat(userController.getUserIdByEmail("ada@example.com")).isEqualTo(userId);
    }

    @Test
    void lookupReturnsNullWhenMissing() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThat(userController.getUserIdByEmail("missing@example.com")).isNull();
    }

    @Test
    void deleteUserDelegatesToUseCase() {
        UUID userId = UUID.randomUUID();

        ResponseEntity<GeneralResponse<Void>> entity = userController.deleteUser(userId);

        assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(userDeleteCase).deleteUser(userId);
    }
}