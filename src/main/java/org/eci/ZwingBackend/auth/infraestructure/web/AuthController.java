package org.eci.ZwingBackend.auth.infraestructure.web;

import jakarta.servlet.http.HttpServletRequest;
import org.eci.ZwingBackend.auth.application.port.in.AuthenticateWithGoogleUseCase;
import org.eci.ZwingBackend.auth.application.port.in.LogoutUseCase;
import org.eci.ZwingBackend.auth.infraestructure.web.dto.request.GoogleAuthRequest;
import org.eci.ZwingBackend.auth.infraestructure.web.dto.response.AuthResponse;
import org.eci.ZwingBackend.shared.dto.GeneralResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthenticateWithGoogleUseCase authenticateUseCase;
    private final LogoutUseCase logoutUseCase;

    public AuthController(AuthenticateWithGoogleUseCase authenticateUseCase, LogoutUseCase logoutUseCase) {
        this.authenticateUseCase = authenticateUseCase;
        this.logoutUseCase = logoutUseCase;
    }

    @PostMapping("/google")
    public ResponseEntity<GeneralResponse<AuthResponse>> authenticateWithGoogle(@RequestBody GoogleAuthRequest request, HttpServletResponse response) {
        AuthResponse result = authenticateUseCase.authenticate(request.getIdToken());

        ResponseCookie jwtCookie = ResponseCookie.from("jwt_token", result.getToken())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(24 * 60 * 60)
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
        String message = result.isNewUser() ? "User Registered Successfully" : "Successful Log In";
        return ResponseEntity.ok(GeneralResponse.success(result, message));
    }

    @PostMapping("/logout")
    public ResponseEntity<GeneralResponse<String>> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = null;
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("jwt_token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token != null) {
            logoutUseCase.logout(token);
        }

        ResponseCookie jwtCookie = ResponseCookie.from("jwt_token", "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)
                .sameSite("None")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, jwtCookie.toString());
        return ResponseEntity.ok(GeneralResponse.success(null, "Successfully logged out"));
    }
}