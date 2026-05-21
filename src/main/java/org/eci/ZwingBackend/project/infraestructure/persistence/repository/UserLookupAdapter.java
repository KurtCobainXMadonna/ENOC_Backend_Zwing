package org.eci.ZwingBackend.project.infraestructure.persistence.repository;

import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.auth.domain.model.User;
import org.eci.ZwingBackend.auth.infraestructure.persistence.Postgre.UserAuthRepository;

import org.eci.ZwingBackend.auth.infraestructure.persistence.repository.mapper.UserAuthMapper;
import org.eci.ZwingBackend.project.application.port.out.UserLookupPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@AllArgsConstructor
public class UserLookupAdapter implements UserLookupPort {
    private final UserAuthRepository authUserRepository;
    private final UserAuthMapper userAuthMapper;

    @Override
    public User getUserByEmail(String email) {
        return authUserRepository.findByEmail(email)
                .map(userAuthMapper::toDomain)
                .orElse(null);
    }

    @Override
    public User getUserById(UUID userId) {
        return authUserRepository.findById(userId)
                .map(userAuthMapper::toDomain)
                .orElse(null);
    }
}
