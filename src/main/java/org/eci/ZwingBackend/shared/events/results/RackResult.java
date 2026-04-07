package org.eci.ZwingBackend.shared.events.results;

import lombok.Getter;

@Getter
public abstract class RackResult {
    private final String projectId;
    private final String triggeredBy;

    protected RackResult(String projectId, String triggeredBy) {
        this.projectId = projectId;
        this.triggeredBy = triggeredBy;
    }
}
