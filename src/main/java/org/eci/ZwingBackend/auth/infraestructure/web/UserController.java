package org.eci.ZwingBackend.auth.infraestructure.web;

import org.eci.ZwingBackend.auth.application.port.in.UserDeleteCase;
import org.eci.ZwingBackend.auth.application.port.out.UserRepositoryAuthOutPort;
import org.eci.ZwingBackend.auth.domain.model.User;
import org.eci.ZwingBackend.shared.dto.GeneralResponse;
import org.eci.ZwingBackend.auth.infraestructure.web.dto.response.UserResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserController {
    private final UserRepositoryAuthOutPort userRepository;
    private final UserDeleteCase userDeleteCase;

    @GetMapping("/me")
    public ResponseEntity<GeneralResponse<UserResponse>> getCurrentUser(@RequestHeader("X-User-Id") UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(GeneralResponse.success(new UserResponse(user.getName(), user.getEmail()), "Session active"));
    }

    @GetMapping("/lookup")
    public UUID getUserIdByEmail(@RequestParam("email") String email) {
        return userRepository.findByEmail(email)
                .map(User::getUserId)
                .orElse(null);
    }

    @DeleteMapping("/me")
    public ResponseEntity<GeneralResponse<Void>> deleteUser(@RequestHeader("X-User-Id") UUID userId) {
        userDeleteCase.deleteUser(userId);
        return ResponseEntity.ok(GeneralResponse.success(null, "User deleted successfully"));
    }
}
