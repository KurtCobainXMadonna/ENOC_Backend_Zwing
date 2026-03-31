package org.eci.ZwingBackend.project.infraestructure.web;

import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.project.application.port.in.InviteCollaboratorCase;
import org.eci.ZwingBackend.project.infraestructure.web.dto.request.InviteRequest;
import org.eci.ZwingBackend.shared.dto.GeneralResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/invites")
@AllArgsConstructor
public class InviteController {
    private final InviteCollaboratorCase inviteCollaboratorCase;

    @PostMapping
    public ResponseEntity<GeneralResponse<Map<String, String>>> invite(@RequestBody InviteRequest request, @RequestHeader("X-User-Id") UUID requesterId) {
        String token = inviteCollaboratorCase.inviteCollaborator(request.getProjectId(), request.getInviteeEmail(), requesterId);

        return ResponseEntity.ok(GeneralResponse.success(Map.of("inviteToken", token), "Invite created. Share this token with " + request.getInviteeEmail()));
    }

    @PostMapping("/accept")
    public ResponseEntity<GeneralResponse<Void>> accept(@RequestParam String token, @RequestHeader("X-User-Id") UUID acceptingUserId) {
        inviteCollaboratorCase.acceptInvite(token, acceptingUserId);
        return ResponseEntity.ok(GeneralResponse.success(null, "You have joined the project successfully."));
    }
}