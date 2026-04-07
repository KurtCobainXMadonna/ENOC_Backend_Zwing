package org.eci.ZwingBackend.shared.events.commands;

public abstract class PlaybackCommand extends RackCommand {

    protected PlaybackCommand(String projectId, String userId, String userEmail) {
        super(projectId, userId, userEmail);
    }

    public static class StartPlayback extends PlaybackCommand {
        public StartPlayback(String projectId, String userId, String userEmail) {
            super(projectId, userId, userEmail);
        }
    }

    public static class StopPlayback extends PlaybackCommand {
        public StopPlayback(String projectId, String userId, String userEmail) {
            super(projectId, userId, userEmail);
        }
    }
}
