package org.eci.ZwingBackend.shared.events.commands;

import lombok.Getter;

@Getter
public abstract class ChannelLockCommand extends RackCommand {
    private final String channelId;

    protected ChannelLockCommand(String projectId, String userId, String userEmail, String channelId) {
        super(projectId, userId, userEmail);
        this.channelId = channelId;
    }

    public static class AcquireLock extends ChannelLockCommand {
        public AcquireLock(String projectId, String userId, String userEmail, String channelId) {
            super(projectId, userId, userEmail, channelId);
        }
    }

    public static class ReleaseLock extends ChannelLockCommand {
        public ReleaseLock(String projectId, String userId, String userEmail, String channelId) {
            super(projectId, userId, userEmail, channelId);
        }
    }
}
