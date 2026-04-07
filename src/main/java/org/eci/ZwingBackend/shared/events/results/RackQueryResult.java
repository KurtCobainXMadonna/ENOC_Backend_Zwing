package org.eci.ZwingBackend.shared.events.results;

import lombok.Getter;
import org.eci.ZwingBackend.rack.domain.model.ChannelRack;

@Getter
public abstract class RackQueryResult extends RackResult {

    protected RackQueryResult(String projectId, String triggeredBy) {
        super(projectId, triggeredBy);
    }

    @Getter
    public static class RackStateLoaded extends RackQueryResult {
        private final ChannelRack rack;
        private final String targetUserId; // sent only to this user, not broadcast

        public RackStateLoaded(String projectId, String triggeredBy, ChannelRack rack, String targetUserId) {
            super(projectId, triggeredBy);
            this.rack = rack;
            this.targetUserId = targetUserId;
        }
    }
}
