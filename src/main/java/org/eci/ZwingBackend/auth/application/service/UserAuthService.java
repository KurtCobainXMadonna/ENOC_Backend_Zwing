package org.eci.ZwingBackend.auth.application.service;

import org.eci.ZwingBackend.auth.application.port.in.AuthenticateWithGoogleUseCase;
import org.eci.ZwingBackend.auth.application.port.in.UserDeleteCase;
import org.eci.ZwingBackend.auth.application.port.out.GoogleAuthPort;
import org.eci.ZwingBackend.auth.application.port.out.TokenGeneratorPort;
import org.eci.ZwingBackend.auth.application.port.out.UserRepositoryAuthOutPort;
import org.eci.ZwingBackend.auth.domain.model.GoogleUserData;
import org.eci.ZwingBackend.auth.domain.model.User;
import org.eci.ZwingBackend.auth.infraestructure.web.dto.response.AuthResponse;
import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.shared.events.UserDeletedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class UserAuthService implements AuthenticateWithGoogleUseCase, UserDeleteCase {
    private final GoogleAuthPort googleAuthAdapter;
    private final UserRepositoryAuthOutPort userRepository;
    private final TokenGeneratorPort tokenGeneratorPort;
    private ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public AuthResponse authenticate(String idToken) {
        GoogleUserData googleUser = googleAuthAdapter.verifyToken(idToken);

        Optional<User> optionalUser = userRepository.findByEmail(googleUser.getEmail());
        User user;
        boolean isNewUser = false;

        if (optionalUser.isEmpty()) {
            user = new User(UUID.randomUUID(), googleUser.getName(), googleUser.getEmail());
            user = userRepository.save(user);
            isNewUser = true;
        } else {
            user = optionalUser.get();
        }
        String internalToken = tokenGeneratorPort.generateToken(user);

        return new AuthResponse(internalToken, user.getName(), user.getEmail(), isNewUser);
    }


    @Override
    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.delete(user);

        eventPublisher.publishEvent(new UserDeletedEvent(userId));
    }
}
