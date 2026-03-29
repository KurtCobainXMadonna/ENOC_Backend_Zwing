package org.eci.ZwingBackend.project.infraestructure.persistence.repository;

import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.auth.infraestructure.persistence.Postgre.UserAuthRepository;
import org.eci.ZwingBackend.auth.infraestructure.persistence.entity.UserEntity;
import org.eci.ZwingBackend.project.application.port.out.UserLookupPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@AllArgsConstructor
public class UserLookupAdapter implements UserLookupPort {

    private final UserAuthRepository authUserRepository;

    @Override
    public UUID getUserIdByEmail(String email) {
        return authUserRepository.findByEmail(email)
                .map(UserEntity::getUserId)
                .orElse(null);
    }
}
