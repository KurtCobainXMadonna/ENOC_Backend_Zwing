package org.eci.ZwingBackend.shared.events.results;

import lombok.Getter;

import java.util.UUID;

@Getter
public abstract class ChannelLockResult extends RackResult {
    protected ChannelLockResult(String projectId, String triggeredBy) {
        super(projectId, triggeredBy);
    }

    @Getter
    public static class LockAcquired extends ChannelLockResult {
        private final UUID channelId;
        private final String lockedByEmail;

        public LockAcquired(String projectId, String triggeredBy, UUID channelId, String lockedByEmail) {
            super(projectId, triggeredBy);
            this.channelId = channelId;
            this.lockedByEmail = lockedByEmail;
        }
    }

    @Getter
    public static class LockReleased extends ChannelLockResult {
        private final String channelId;

        public LockReleased(String projectId, String triggeredBy, String channelId) {
            super(projectId, triggeredBy);
            this.channelId = channelId;
        }
    }

    @Getter
    public static class LockDenied extends ChannelLockResult {
        private final String channelId;
        private final String currentHolder;

        public LockDenied(String projectId, String triggeredBy, String channelId, String currentHolder) {
            super(projectId, triggeredBy);
            this.channelId = channelId;
            this.currentHolder = currentHolder;
        }
    }
}
