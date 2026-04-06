package org.eci.ZwingBackend.shared.events.results;

import lombok.Getter;
import org.eci.ZwingBackend.rack.domain.model.Channel;

@Getter
public abstract class ChannelMutationResult extends RackResult {
    protected ChannelMutationResult(String projectId, String triggeredBy) {
        super(projectId, triggeredBy);
    }

    @Getter
    public static class ChannelAdded extends ChannelMutationResult {
        private final Channel channel;

        public ChannelAdded(String projectId, String triggeredBy, Channel channel) {
            super(projectId, triggeredBy);
            this.channel = channel;
        }
    }

    @Getter
    public static class ChannelRemoved extends ChannelMutationResult {
        private final String channelId;

        public ChannelRemoved(String projectId, String triggeredBy, String channelId) {
            super(projectId, triggeredBy);
            this.channelId = channelId;
        }
    }

    @Getter
    public static class ChannelUpdated extends ChannelMutationResult {
        private final Channel channel;

        public ChannelUpdated(String projectId, String triggeredBy, Channel channel) {
            super(projectId, triggeredBy);
            this.channel = channel;
        }
    }

    @Getter
    public static class StepToggled extends ChannelMutationResult {
        private final String channelId;
        private final int stepIndex;
        private final boolean newValue;

        public StepToggled(String projectId, String triggeredBy, String channelId, int stepIndex, boolean newValue) {
            super(projectId, triggeredBy);
            this.channelId = channelId;
            this.stepIndex = stepIndex;
            this.newValue = newValue;
        }
    }
}
