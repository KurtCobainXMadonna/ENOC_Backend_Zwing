package org.eci.ZwingBackend.auth.infraestructure.persistence.repository;

import org.eci.ZwingBackend.auth.application.port.out.UserRepositoryAuthOutPort;
import org.eci.ZwingBackend.auth.domain.model.User;
import org.eci.ZwingBackend.auth.infraestructure.persistence.Postgre.UserAuthRepository;
import org.eci.ZwingBackend.auth.infraestructure.persistence.entity.UserEntity;
import org.eci.ZwingBackend.auth.infraestructure.persistence.repository.mapper.UserAuthMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@AllArgsConstructor
public class UserAuthRepositoryAdapter implements UserRepositoryAuthOutPort {
    private UserAuthRepository postgreRepository;
    private UserAuthMapper mapper;

    @Override
    public User save(User user) {
        UserEntity entity = mapper.toEntity(user);
        postgreRepository.save(entity);
        return mapper.toDomain(entity);
    }

    @Override
    public void delete(User user) {
        postgreRepository.deleteById(user.getUserId());
    }

    @Override
    public Optional<User> findById(UUID id) {
        return postgreRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return postgreRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public User update(UUID id, User user) {
        UserEntity entity = postgreRepository.findById(id).orElseThrow(() -> new RuntimeException("User not Found"));

        UserEntity updated = postgreRepository.save(entity);
        return mapper.toDomain(updated);
    }
}
