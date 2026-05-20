package org.eci.ZwingBackend.auth.infraestructure.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eci.ZwingBackend.auth.application.port.in.AuthenticateWithGoogleUseCase;
import org.eci.ZwingBackend.auth.application.port.in.LogoutUseCase;
import org.eci.ZwingBackend.auth.application.port.in.RefreshSessionUseCase;
import org.eci.ZwingBackend.auth.application.service.RefreshTokenService;
import org.eci.ZwingBackend.auth.domain.model.TokenPair;
import org.eci.ZwingBackend.auth.infraestructure.adapters.out.JwtTokenAdapter;
import org.eci.ZwingBackend.auth.infraestructure.web.dto.request.GoogleAuthRequest;
import org.eci.ZwingBackend.auth.infraestructure.web.dto.response.AuthResponse;
import org.eci.ZwingBackend.shared.dto.GeneralResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final String ACCESS_COOKIE = "jwt_token";
    private static final String REFRESH_COOKIE = "refresh_token";
    private static final String REFRESH_COOKIE_PATH = "/auth";

    private final AuthenticateWithGoogleUseCase authenticateUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RefreshSessionUseCase refreshSessionUseCase;
    private final JwtTokenAdapter accessTokenAdapter;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthenticateWithGoogleUseCase authenticateUseCase, LogoutUseCase logoutUseCase, RefreshSessionUseCase refreshSessionUseCase, JwtTokenAdapter accessTokenAdapter, RefreshTokenService refreshTokenService) {
        this.authenticateUseCase = authenticateUseCase;
        this.logoutUseCase = logoutUseCase;
        this.refreshSessionUseCase = refreshSessionUseCase;
        this.accessTokenAdapter = accessTokenAdapter;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/google")
    public ResponseEntity<GeneralResponse<AuthResponse>> authenticateWithGoogle(@RequestBody GoogleAuthRequest request, HttpServletResponse response) {
        AuthResponse result = authenticateUseCase.authenticate(request.getIdToken());

        response.addHeader(HttpHeaders.SET_COOKIE, buildAccessCookie(result.getToken()).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, buildRefreshCookie(result.getRefreshToken()).toString());

        String message = result.isNewUser() ? "User Registered Successfully" : "Successful Log In";
        return ResponseEntity.ok(GeneralResponse.success(result, message));
    }

    @PostMapping("/refresh")
    public ResponseEntity<GeneralResponse<Void>> refresh(HttpServletRequest request, HttpServletResponse response) {
        String rawRefresh = readCookie(request, REFRESH_COOKIE);
        if (rawRefresh == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(GeneralResponse.error("Missing refresh token"));
        }

        try {
            TokenPair pair = refreshSessionUseCase.refresh(rawRefresh);
            response.addHeader(HttpHeaders.SET_COOKIE, buildAccessCookie(pair.accessToken()).toString());
            response.addHeader(HttpHeaders.SET_COOKIE, buildRefreshCookie(pair.refreshToken()).toString());
            return ResponseEntity.ok(GeneralResponse.success(null, "Session refreshed"));
        } catch (RefreshTokenService.InvalidRefreshTokenException e) {
            response.addHeader(HttpHeaders.SET_COOKIE, expireCookie(ACCESS_COOKIE, "/").toString());
            response.addHeader(HttpHeaders.SET_COOKIE, expireCookie(REFRESH_COOKIE, REFRESH_COOKIE_PATH).toString());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(GeneralResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<GeneralResponse<String>> logout(HttpServletRequest request, HttpServletResponse response) {
        String rawRefresh = readCookie(request, REFRESH_COOKIE);
        if (rawRefresh != null) {
            logoutUseCase.logout(rawRefresh);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, expireCookie(ACCESS_COOKIE, "/").toString());
        response.addHeader(HttpHeaders.SET_COOKIE, expireCookie(REFRESH_COOKIE, REFRESH_COOKIE_PATH).toString());
        return ResponseEntity.ok(GeneralResponse.success(null, "Successfully logged out"));
    }

    private ResponseCookie buildAccessCookie(String value) {
        return ResponseCookie.from(ACCESS_COOKIE, value)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(accessTokenAdapter.getExpirationSeconds())
                .sameSite("Lax")
                .build();
    }

    private ResponseCookie buildRefreshCookie(String value) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(true)
                .path(REFRESH_COOKIE_PATH)
                .maxAge(refreshTokenService.ttlSeconds())
                .sameSite("Lax")
                .build();
    }

    private ResponseCookie expireCookie(String name, String path) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(true)
                .path(path)
                .maxAge(0)
                .sameSite("Lax")
                .build();
    }

    private String readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
