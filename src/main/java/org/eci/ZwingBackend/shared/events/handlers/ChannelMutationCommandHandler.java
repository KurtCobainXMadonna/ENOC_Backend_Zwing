package org.eci.ZwingBackend.shared.events.handlers;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eci.ZwingBackend.rack.application.port.in.ManageChannelCase;
import org.eci.ZwingBackend.rack.domain.model.Channel;
import org.eci.ZwingBackend.shared.events.commands.ChannelMutationCommand;
import org.eci.ZwingBackend.shared.events.results.ChannelMutationResult;
import org.eci.ZwingBackend.shared.events.results.RackErrorResult;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@AllArgsConstructor
public class ChannelMutationCommandHandler {
    private final ManageChannelCase manageChannelCase;
    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    public void handleAddChannel(ChannelMutationCommand.AddChannel cmd) {
        try {
            Channel channel = manageChannelCase.addChannel(UUID.fromString(cmd.getProjectId()), cmd.getName(), UUID.fromString(cmd.getSoundId()), UUID.fromString(cmd.getUserId()));
            eventPublisher.publishEvent(new ChannelMutationResult.ChannelAdded(cmd.getProjectId(), cmd.getUserId(), channel));
        } catch (Exception e) {
            log.error("[EventBus] AddChannel failed: {}", e.getMessage());
            eventPublisher.publishEvent(new RackErrorResult(cmd.getProjectId(), cmd.getUserId(), e.getMessage(), cmd.getUserId()));
        }
    }

    @EventListener
    public void handleRemoveChannel(ChannelMutationCommand.RemoveChannel cmd) {
        try {
            manageChannelCase.removeChannel(UUID.fromString(cmd.getProjectId()), UUID.fromString(cmd.getChannelId()), UUID.fromString(cmd.getUserId()));
            eventPublisher.publishEvent(new ChannelMutationResult.ChannelRemoved(cmd.getProjectId(), cmd.getUserId(), cmd.getChannelId()));
        } catch (Exception e) {
            log.error("[EventBus] RemoveChannel failed: {}", e.getMessage());
            eventPublisher.publishEvent(new RackErrorResult(cmd.getProjectId(), cmd.getUserId(), e.getMessage(), cmd.getUserId()));
        }
    }

    @EventListener
    public void handleUpdateChannel(ChannelMutationCommand.UpdateChannel cmd) {
        try {
            Channel updated = manageChannelCase.updateChannel(UUID.fromString(cmd.getProjectId()), UUID.fromString(cmd.getChannelId()), cmd.getName(),
                    cmd.getSoundId() != null ? UUID.fromString(cmd.getSoundId()) : null,
                    cmd.getVolume(),
                    cmd.isActive(),
                    UUID.fromString(cmd.getUserId()));
            eventPublisher.publishEvent(new ChannelMutationResult.ChannelUpdated(cmd.getProjectId(), cmd.getUserId(), updated));
        } catch (Exception e) {
            log.error("[EventBus] UpdateChannel failed: {}", e.getMessage());
            eventPublisher.publishEvent(new RackErrorResult(cmd.getProjectId(), cmd.getUserId(), e.getMessage(), cmd.getUserId()));
        }
    }

    @EventListener
    public void handleToggleStep(ChannelMutationCommand.ToggleStep cmd) {
        try {
            boolean newValue = manageChannelCase.toggleStep(UUID.fromString(cmd.getProjectId()), UUID.fromString(cmd.getChannelId()), cmd.getStepIndex(), UUID.fromString(cmd.getUserId()));
            eventPublisher.publishEvent(new ChannelMutationResult.StepToggled(cmd.getProjectId(), cmd.getUserId(), cmd.getChannelId(), cmd.getStepIndex(), newValue));
        } catch (Exception e) {
            log.error("[EventBus] ToggleStep failed: {}", e.getMessage());
            eventPublisher.publishEvent(new RackErrorResult(cmd.getProjectId(), cmd.getUserId(), e.getMessage(), cmd.getUserId()));
        }
    }
}
