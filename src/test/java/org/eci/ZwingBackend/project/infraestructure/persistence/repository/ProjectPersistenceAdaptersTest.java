package org.eci.ZwingBackend.project.infraestructure.persistence.repository;

import org.eci.ZwingBackend.auth.domain.model.User;
import org.eci.ZwingBackend.auth.infraestructure.persistence.Postgre.UserAuthRepository;
import org.eci.ZwingBackend.auth.infraestructure.persistence.entity.UserEntity;
import org.eci.ZwingBackend.auth.infraestructure.persistence.repository.mapper.UserAuthMapper;
import org.eci.ZwingBackend.project.domain.model.Project;
import org.eci.ZwingBackend.project.infraestructure.persistence.entity.ProjectEntity;
import org.eci.ZwingBackend.project.infraestructure.persistence.postgre.ProjectRepository;
import org.eci.ZwingBackend.project.infraestructure.persistence.repository.mapper.ProjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProjectPersistenceAdaptersTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserAuthRepository userAuthRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final ProjectMapper projectMapper = new ProjectMapper(new UserAuthMapper());
    private final UserAuthMapper userAuthMapper = new UserAuthMapper();

    private ProjectRepositoryAdapter projectRepositoryAdapter;
    private UserLookupAdapter userLookupAdapter;
    private RedisInviteAdapter inviteAdapter;

    @BeforeEach
    void setUp() {
        projectRepositoryAdapter = new ProjectRepositoryAdapter(projectRepository, projectMapper);
        userLookupAdapter = new UserLookupAdapter(userAuthRepository, userAuthMapper);
        inviteAdapter = new RedisInviteAdapter(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void projectRepositoryAdapterSavesAndLoadsProjects() {
        UUID projectId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID collaboratorId = UUID.randomUUID();

        Project project = new Project("Demo");
        project.setProjectId(projectId);
        project.setProjectOwner(new User(ownerId, "Owner", "owner@example.com"));
        project.setCollaborators(Set.of(new User(collaboratorId, "Collab", "collab@example.com")));

        when(projectRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        projectRepositoryAdapter.save(project);

        ProjectEntity entity = new ProjectEntity();
        entity.setProjectId(projectId);
        entity.setProjectName("Demo");
        UserEntity owner = new UserEntity();
        owner.setUserId(ownerId);
        owner.setName("Owner");
        owner.setEmail("owner@example.com");
        entity.setProjectOwner(owner);
        UserEntity collaborator = new UserEntity();
        collaborator.setUserId(collaboratorId);
        collaborator.setName("Collab");
        collaborator.setEmail("collab@example.com");
        entity.setCollaborators(Set.of(collaborator));
        entity.setChannelRackId(UUID.randomUUID());

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(entity));
        when(projectRepository.findByCollaborators_UserId(collaboratorId)).thenReturn(List.of(entity));
        when(projectRepository.findByProjectOwner_UserId(ownerId)).thenReturn(List.of(entity));

        assertThat(projectRepositoryAdapter.getProjectById(projectId))
                .usingRecursiveComparison()
                .isEqualTo(projectMapper.toDomain(entity));
        assertThat(projectRepositoryAdapter.getProjectsByCollaborator(collaboratorId)).hasSize(1);
        assertThat(projectRepositoryAdapter.getProjectsByOwner(ownerId)).hasSize(1);

        projectRepositoryAdapter.delete(project);
        verify(projectRepository).deleteById(projectId);
    }

    @Test
    void projectRepositoryAdapterThrowsWhenProjectMissing() {
        UUID projectId = UUID.randomUUID();
        when(projectRepository.findById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectRepositoryAdapter.getProjectById(projectId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Project not found");
    }

    @Test
    void userLookupAdapterFindsUsersOrReturnsNull() {
        UUID userId = UUID.randomUUID();
        UserEntity entity = new UserEntity();
        entity.setUserId(userId);
        entity.setName("Jane");
        entity.setEmail("jane@example.com");
        when(userAuthRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(entity));
        when(userAuthRepository.findById(userId)).thenReturn(Optional.of(entity));
        when(userAuthRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThat(userLookupAdapter.getUserByEmail("jane@example.com"))
                .usingRecursiveComparison()
                .isEqualTo(userAuthMapper.toDomain(entity));
        assertThat(userLookupAdapter.getUserByEmail("missing@example.com")).isNull();
        assertThat(userLookupAdapter.getUserById(userId))
                .usingRecursiveComparison()
                .isEqualTo(userAuthMapper.toDomain(entity));
    }

    @Test
    void inviteAdapterSavesReadsAndDeletesInvites() {
        UUID projectId = UUID.randomUUID();
        when(valueOperations.get("invite:token-123")).thenReturn(projectId.toString());

        inviteAdapter.saveInvite("token-123", projectId, 90);

        assertThat(inviteAdapter.findProjectIdByToken("token-123")).contains(projectId);
        inviteAdapter.deleteInvite("token-123");

        verify(valueOperations).set("invite:token-123", projectId.toString(), 90, java.util.concurrent.TimeUnit.SECONDS);
        verify(redisTemplate).delete("invite:token-123");
    }

    @Test
    void inviteAdapterReturnsEmptyWhenMissing() {
        when(valueOperations.get(anyString())).thenReturn(null);

        assertThat(inviteAdapter.findProjectIdByToken("missing")).isEmpty();
    }
}