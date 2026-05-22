package org.eci.ZwingBackend.auth.infraestructure.persistence.repository;

import org.eci.ZwingBackend.auth.domain.model.User;
import org.eci.ZwingBackend.auth.infraestructure.persistence.Postgre.UserAuthRepository;
import org.eci.ZwingBackend.auth.infraestructure.persistence.entity.UserEntity;
import org.eci.ZwingBackend.auth.infraestructure.persistence.repository.mapper.UserAuthMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAuthRepositoryAdapterTest {

    @Mock
    private UserAuthRepository postgreRepository;

    @Mock
    private UserAuthMapper mapper;

    private UserAuthRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new UserAuthRepositoryAdapter(postgreRepository, mapper);
    }

    @Test
    void saveMapsEntityAndReturnsDomainUser() {
        User user = new User(UUID.randomUUID(), "Ada", "ada@example.com");
        UserEntity entity = new UserEntity();
        when(mapper.toEntity(user)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(user);

        User saved = adapter.save(user);

        assertThat(saved).isEqualTo(user);
        verify(postgreRepository).save(entity);
    }

    @Test
    void deleteUsesUserId() {
        User user = new User(UUID.randomUUID(), "Ada", "ada@example.com");

        adapter.delete(user);

        verify(postgreRepository).deleteById(user.getUserId());
    }

    @Test
    void findMethodsMapEntitiesToDomainUsers() {
        UserEntity entity = new UserEntity();
        entity.setUserId(UUID.randomUUID());
        entity.setName("Ada");
        entity.setEmail("ada@example.com");
        User user = new User(entity.getUserId(), entity.getName(), entity.getEmail());

        when(postgreRepository.findById(entity.getUserId())).thenReturn(Optional.of(entity));
        when(postgreRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(user);

        assertThat(adapter.findById(entity.getUserId())).contains(user);
        assertThat(adapter.findByEmail("ada@example.com")).contains(user);
    }

    @Test
    void updateSavesExistingEntityAndReturnsMappedUser() {
        UUID userId = UUID.randomUUID();
        UserEntity entity = new UserEntity();
        entity.setUserId(userId);
        entity.setName("Ada");
        entity.setEmail("ada@example.com");
        User updatedUser = new User(userId, "Ada", "ada@example.com");

        when(postgreRepository.findById(userId)).thenReturn(Optional.of(entity));
        when(postgreRepository.save(entity)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(updatedUser);

        assertThat(adapter.update(userId, updatedUser)).isEqualTo(updatedUser);
    }

    @Test
    void updateFailsWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();

        when(postgreRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.update(userId, new User(userId, "Ada", "ada@example.com")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User not Found");
    }
}