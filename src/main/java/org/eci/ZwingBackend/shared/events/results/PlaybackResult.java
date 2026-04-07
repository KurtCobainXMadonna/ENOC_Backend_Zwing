package org.eci.ZwingBackend.shared.events.results;

public abstract class PlaybackResult extends RackResult {

    protected PlaybackResult(String projectId, String triggeredBy) {
        super(projectId, triggeredBy);
    }

    public static class PlaybackStarted extends PlaybackResult {
        public PlaybackStarted(String projectId, String triggeredBy) {
            super(projectId, triggeredBy);
        }
    }

    public static class PlaybackStopped extends PlaybackResult {
        public PlaybackStopped(String projectId, String triggeredBy) {
            super(projectId, triggeredBy);
        }
    }
}
