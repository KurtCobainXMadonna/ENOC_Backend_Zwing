package org.eci.ZwingBackend.shared.events.commands;

public abstract class RackQueryCommand extends RackCommand {

    protected RackQueryCommand(String projectId, String userId, String userEmail) {
        super(projectId, userId, userEmail);
    }

    public static class LoadRack extends RackQueryCommand {
        public LoadRack(String projectId, String userId, String userEmail) {
            super(projectId, userId, userEmail);
        }
    }
}
