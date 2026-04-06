package org.eci.ZwingBackend.shared.events.commands;

import lombok.Getter;

import java.util.UUID;

@Getter
public abstract class ChannelMutationCommand extends RackCommand {

    protected ChannelMutationCommand(String projectId, String userId, String userEmail) {
        super(projectId, userId, userEmail);
    }

    @Getter
    public static class AddChannel extends ChannelMutationCommand {
        private final String name;
        private final String soundId;

        public AddChannel(String projectId, String userId, String userEmail, String name, String soundId) {
            super(projectId, userId, userEmail);
            this.name = name;
            this.soundId = soundId;
        }
    }

    @Getter
    public static class RemoveChannel extends ChannelMutationCommand {
        private final String channelId;

        public RemoveChannel(String projectId, String userId, String userEmail, String channelId) {
            super(projectId, userId, userEmail);
            this.channelId = channelId;
        }
    }

    @Getter
    public static class UpdateChannel extends ChannelMutationCommand {
        private final String channelId;
        private final String name;
        private final String soundId;
        private final float volume;
        private final boolean active;

        public UpdateChannel(String projectId, String userId, String userEmail,
                             String channelId, String name, String soundId, float volume, boolean active) {
            super(projectId, userId, userEmail);
            this.channelId = channelId;
            this.name = name;
            this.soundId = soundId;
            this.volume = volume;
            this.active = active;
        }
    }

    @Getter
    public static class ToggleStep extends ChannelMutationCommand {
        private final String channelId;
        private final int stepIndex;

        public ToggleStep(String projectId, String userId, String userEmail, String channelId, int stepIndex) {
            super(projectId, userId, userEmail);
            this.channelId = channelId;
            this.stepIndex = stepIndex;
        }
    }
}
