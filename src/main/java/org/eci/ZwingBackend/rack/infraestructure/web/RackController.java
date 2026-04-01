package org.eci.ZwingBackend.rack.infraestructure.web;

import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.rack.application.port.in.ManageChannelCase;
import org.eci.ZwingBackend.rack.application.port.in.ManageRackCase;
import org.eci.ZwingBackend.rack.domain.model.Channel;
import org.eci.ZwingBackend.rack.domain.model.ChannelRack;
import org.eci.ZwingBackend.rack.infraestructure.web.dto.request.AddChannelRequest;
import org.eci.ZwingBackend.rack.infraestructure.web.dto.request.UpdateChannelRequest;
import org.eci.ZwingBackend.rack.infraestructure.web.dto.response.RackResponse;
import org.eci.ZwingBackend.shared.dto.GeneralResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/rack")
@AllArgsConstructor
public class RackController {

    private final ManageRackCase manageRackCase;
    private final ManageChannelCase manageChannelCase;

    @GetMapping("/{projectId}")
    public ResponseEntity<GeneralResponse<RackResponse>> getRack(@PathVariable UUID projectId, @RequestHeader("X-User-Id") UUID requesterId) {
        ChannelRack rack = manageRackCase.getRackByProject(projectId);
        return ResponseEntity.ok(GeneralResponse.success(RackResponse.from(rack), "Rack loaded successfully"));
    }

    @PostMapping("/{projectId}/channels")
    public ResponseEntity<GeneralResponse<RackResponse.ChannelResponse>> addChannel(@PathVariable UUID projectId, @RequestBody AddChannelRequest request, @RequestHeader("X-User-Id") UUID requesterId) {
        Channel channel = manageChannelCase.addChannel(projectId, request.getName(), request.getSoundId(), requesterId);
        return ResponseEntity.ok(GeneralResponse.success(RackResponse.ChannelResponse.from(channel), "Channel added successfully"));
    }

    @PutMapping("/{projectId}/channels/{channelId}")
    public ResponseEntity<GeneralResponse<RackResponse.ChannelResponse>> updateChannel(@PathVariable UUID projectId, @PathVariable UUID channelId, @RequestBody UpdateChannelRequest request, @RequestHeader("X-User-Id") UUID requesterId) {
        Channel updated = manageChannelCase.updateChannel(projectId, channelId, request.getName(), request.getSoundId(), request.getVolume(), request.isActive(), requesterId);
        return ResponseEntity.ok(GeneralResponse.success(RackResponse.ChannelResponse.from(updated), "Channel updated successfully"));
    }

    @DeleteMapping("/{projectId}/channels/{channelId}")
    public ResponseEntity<GeneralResponse<Void>> removeChannel(@PathVariable UUID projectId, @PathVariable UUID channelId, @RequestHeader("X-User-Id") UUID requesterId) {
        manageChannelCase.removeChannel(projectId, channelId, requesterId);
        return ResponseEntity.ok(GeneralResponse.success(null, "Channel removed successfully"));
    }
}
