package org.eci.ZwingBackend.project.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.eci.ZwingBackend.auth.domain.model.User;

import java.util.*;

@Data
@AllArgsConstructor
public class Project {
    private UUID projectId;
    private String projectName;
    private Set<User> collaborators = new HashSet<>();
    private User projectOwner;

    public Project(String projectName) {
        this.projectName = projectName;
    }

    public void addCollaborator(User collaborator){
        this.collaborators.add(collaborator);
    }
    public void removeCollaborator(UUID collaboratorId){
        this.collaborators.removeIf(collab -> collab.getUserId().equals(collaboratorId));
    }
}
