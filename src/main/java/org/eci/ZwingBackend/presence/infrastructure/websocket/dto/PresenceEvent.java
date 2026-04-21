package org.eci.ZwingBackend.presence.infrastructure.websocket.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eci.ZwingBackend.presence.domain.model.Presence;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class PresenceEvent {
    private String type;            // "JOINED", "LEFT", "ROSTER_SNAPSHOT"
    private String changedUserId;   // null for ROSTER_SNAPSHOT
    private List<Presence> roster;
}
