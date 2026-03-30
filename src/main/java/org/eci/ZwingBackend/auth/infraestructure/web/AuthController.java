package org.eci.ZwingBackend.auth.infraestructure.web;

import org.eci.ZwingBackend.auth.application.port.in.AuthenticateWithGoogleUseCase;
import org.eci.ZwingBackend.auth.infraestructure.web.dto.request.GoogleAuthRequest;
import org.eci.ZwingBackend.auth.infraestructure.web.dto.response.AuthResponse;
import org.eci.ZwingBackend.shared.dto.GeneralResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthenticateWithGoogleUseCase service;

    public AuthController(AuthenticateWithGoogleUseCase service) {
        this.service = service;
    }

    @PostMapping("/google")
    public ResponseEntity<GeneralResponse<AuthResponse>> authenticateWithGoogle(@RequestBody GoogleAuthRequest request, HttpServletResponse response) {
        AuthResponse result = service.authenticate(request.getIdToken());

        Cookie jwtCookie = new Cookie("jwt_token", result.getToken());
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(24 * 60 * 60);

        response.addCookie(jwtCookie);
        return result.isNewUser() ? ResponseEntity.ok(GeneralResponse.success(result, "User Registered Successfully")) : ResponseEntity.ok(GeneralResponse.success(result, "Successful Log In"));
    }

    @PostMapping("/logout")
    public ResponseEntity<GeneralResponse<String>> logout(HttpServletResponse response) {
        Cookie jwtCookie = new Cookie("jwt_token", null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(false); // Change to true when using HTTPS in production
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0);

        response.addCookie(jwtCookie);
        return ResponseEntity.ok(GeneralResponse.success(null, "Successfully logged out"));
    }
}