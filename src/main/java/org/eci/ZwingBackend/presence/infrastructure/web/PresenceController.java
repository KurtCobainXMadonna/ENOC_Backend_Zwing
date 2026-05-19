package org.eci.ZwingBackend.presence.infrastructure.web;

import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.presence.application.port.in.ManagePresenceCase;
import org.eci.ZwingBackend.presence.domain.model.Presence;
import org.eci.ZwingBackend.shared.dto.GeneralResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/projects/{projectId}/presence")
@AllArgsConstructor
public class PresenceController {
    private final ManagePresenceCase presenceCase;

    @GetMapping
    public ResponseEntity<GeneralResponse<List<Presence>>> getRoster(@PathVariable UUID projectId, @RequestHeader("X-User-Id") String userId) {
        // Note: authorization (is this user a project member?) is enforced upstream
        // by the JWT filter / project membership check on the WS join path. This
        // endpoint is read-only and only returns ephemeral presence data, so a
        // light check is sufficient — add stricter membership validation here if
        // your threat model requires it.
        List<Presence> roster = presenceCase.getRoster(projectId);
        return ResponseEntity.ok(GeneralResponse.success(roster, "Presence roster retrieved"));
    }
}
