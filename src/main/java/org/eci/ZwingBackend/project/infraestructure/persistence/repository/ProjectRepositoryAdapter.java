package org.eci.ZwingBackend.project.infraestructure.persistence.repository;

import org.eci.ZwingBackend.project.application.port.out.ProjectRepositoryOutPort;
import org.eci.ZwingBackend.project.domain.model.Project;
import org.eci.ZwingBackend.project.infraestructure.persistence.entity.ProjectEntity;
import org.eci.ZwingBackend.project.infraestructure.persistence.postgre.ProjectRepository;
import org.eci.ZwingBackend.project.infraestructure.persistence.repository.mapper.ProjectMapper;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Component
@AllArgsConstructor
public class ProjectRepositoryAdapter implements ProjectRepositoryOutPort {
    private ProjectRepository postgreRepository;
    private ProjectMapper mapper;

    @Override
    public void save(Project project) {
        ProjectEntity entity = mapper.toEntity(project);
        postgreRepository.save(entity);
    }

    @Override
    public void delete(Project project) {
        postgreRepository.deleteById(project.getProjectId());
    }

    @Override
    public Project getProjectById(UUID id) {
        ProjectEntity entity = postgreRepository.findById(id).orElseThrow(() -> new RuntimeException("Project not found"));
        return mapper.toDomain(entity);
    }

    @Override
    public List<Project> getProjectsByCollaborator(UUID collaboratorId) {
        List<ProjectEntity> entities = postgreRepository.findByCollaborators_UserId(collaboratorId);
        return entities.stream()
                .map(entity -> mapper.toDomain(entity))
                .collect(Collectors.toList());
    }

    @Override
    public List<Project> getProjectsByOwner(UUID ownerId) {
        List<ProjectEntity> entities = postgreRepository.findByProjectOwner_UserId(ownerId);
        return entities.stream()
                .map(entity -> mapper.toDomain(entity))
                .collect(Collectors.toList());
    }

}
