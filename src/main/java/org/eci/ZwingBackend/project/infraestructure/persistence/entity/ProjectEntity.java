package org.eci.ZwingBackend.project.infraestructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@Table(name = "projects")
public class ProjectEntity {
    @Id
    private UUID projectId;

    @Column(nullable = false)
    private String projectName;

    @Column(nullable = false)
    private UUID projectOwner;

    @ElementCollection
    private List<UUID> collaborators = new ArrayList<>();
}
