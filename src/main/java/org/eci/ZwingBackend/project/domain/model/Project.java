package org.eci.ZwingBackend.project.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
public class Project {
    private UUID projectId;
    private String projectName;
    private List<UUID> collaborators;
    private UUID projectOwner;

    public Project(String projectName) {
        this.projectName = projectName;
        this.collaborators = new ArrayList<>();
    }

    public void addCollaborator(UUID collaboratorId){
        if(!collaborators.contains(collaboratorId)){
            collaborators.add(collaboratorId);
        }
    }
    public void removeCollaborator(UUID collaboratorId){
        collaborators.remove(collaboratorId);
    }
}
