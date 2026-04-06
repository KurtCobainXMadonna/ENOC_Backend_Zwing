package org.eci.ZwingBackend.shared.events.commands;

import lombok.Getter;

@Getter
public abstract class RackCommand {
    private final String projectId;
    private final String userId;
    private final String userEmail;

    protected RackCommand(String projectId, String userId, String userEmail) {
        this.projectId = projectId;
        this.userId = userId;
        this.userEmail = userEmail;
    }
}
