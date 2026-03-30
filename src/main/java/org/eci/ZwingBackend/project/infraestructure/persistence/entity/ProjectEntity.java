package org.eci.ZwingBackend.project.infraestructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.eci.ZwingBackend.auth.infraestructure.persistence.entity.UserEntity;

import java.util.*;

@Entity
@Data
@Table(name = "projects")
public class ProjectEntity {
    @Id
    private UUID projectId;

    @Column(nullable = false)
    private String projectName;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity projectOwner;

    @ManyToMany
    @JoinTable(
            name = "project_collaborators",
            joinColumns = @JoinColumn(name = "project_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<UserEntity> collaborators = new HashSet<>();
}
