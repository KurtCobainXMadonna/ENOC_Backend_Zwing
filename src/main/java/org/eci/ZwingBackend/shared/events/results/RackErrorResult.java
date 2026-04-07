package org.eci.ZwingBackend.shared.events.results;

import lombok.Getter;

@Getter
public class RackErrorResult extends RackResult {
    private final String errorMessage;
    private final String targetUserId;

    public RackErrorResult(String projectId, String triggeredBy, String errorMessage, String targetUserId) {
        super(projectId, triggeredBy);
        this.errorMessage = errorMessage;
        this.targetUserId = targetUserId;
    }
}
