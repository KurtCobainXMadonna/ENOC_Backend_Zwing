package org.eci.ZwingBackend.project.infraestructure.web;

import org.eci.ZwingBackend.project.application.port.in.ManagingCollaboratorCase;
import org.eci.ZwingBackend.project.application.port.in.ManagingProjectsCase;
import org.eci.ZwingBackend.project.domain.model.Project;
import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.project.infraestructure.web.dto.request.CreateProjectRequest;
import org.eci.ZwingBackend.shared.dto.GeneralResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
@AllArgsConstructor
public class ProjectController {
    private ManagingProjectsCase serviceMangeProject;
    private ManagingCollaboratorCase serviceManageCollaborators;

    @GetMapping
    public ResponseEntity<GeneralResponse<Map<String, List<Project>>>> getAllProjects (@RequestHeader("X-User-Id") UUID userId){
        List<Project> ownedProjects = serviceMangeProject.getOwnedProjects(userId);
        List<Project> collaboratingProjects = serviceMangeProject.getCollaboratingProjects(userId);

        Map<String, List<Project>> responseData = new HashMap<>();
        responseData.put("ownedProjects", ownedProjects);
        responseData.put("collaboratingProjects", collaboratingProjects);

        return ResponseEntity.ok(GeneralResponse.success(responseData, "Projects retrieved successfully"));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<GeneralResponse<Project>> getProjectById(@PathVariable UUID projectId, @RequestHeader("X-User-Id") UUID requesterId) {
        Project project = serviceMangeProject.getProjectById(projectId, requesterId);
        return ResponseEntity.ok(GeneralResponse.success(project, "Project retrieved successfully"));
    }


    @PostMapping
    public ResponseEntity<GeneralResponse<Project>> createProject (@RequestBody CreateProjectRequest request, @RequestHeader("X-User-Id") UUID ownerId){
        Project newProject = serviceMangeProject.createProject(request.getName(), ownerId);
        return ResponseEntity.ok(GeneralResponse.success(newProject, "Project created Successfully"));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<GeneralResponse<Void>> deleteProject(@PathVariable UUID projectId, @RequestHeader("X-User-Id") UUID requesterId){
        serviceMangeProject.deleteProject(projectId, requesterId);
        return ResponseEntity.ok(GeneralResponse.success(null, "Project deleted successfully"));
    }

    // Notice: No X-User-Id required here based on your rules!
    @PutMapping("/{projectId}/collaborators/{email}")
    public ResponseEntity<GeneralResponse<Void>> addCollaborator(@PathVariable UUID projectId, @PathVariable String email){
        serviceManageCollaborators.addCollaborator(projectId, email);
        return ResponseEntity.ok(GeneralResponse.success(null, "Collaborator added successfully"));
    }

    @DeleteMapping("/{projectId}/collaborators/{collaboratorId}")
    public ResponseEntity<GeneralResponse<Void>> deleteCollaborator(@PathVariable UUID projectId, @PathVariable UUID collaboratorId, @RequestHeader("X-User-Id") UUID requesterId){
        serviceManageCollaborators.deleteCollaborator(projectId, collaboratorId, requesterId);
        return ResponseEntity.ok(GeneralResponse.success(null, "Collaborator removed successfully"));
    }
}
