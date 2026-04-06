package org.eci.ZwingBackend.shared.events.handlers;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eci.ZwingBackend.rack.application.port.in.ManageRackCase;
import org.eci.ZwingBackend.rack.domain.model.ChannelRack;
import org.eci.ZwingBackend.shared.events.commands.RackQueryCommand;
import org.eci.ZwingBackend.shared.events.results.RackErrorResult;
import org.eci.ZwingBackend.shared.events.results.RackQueryResult;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;


@Slf4j
@Component
@AllArgsConstructor
public class RackQueryCommandHandler {
    private final ManageRackCase manageRackCase;
    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    public void handleLoadRack(RackQueryCommand.LoadRack cmd) {
        try {
            ChannelRack rack = manageRackCase.getRackByProject(UUID.fromString(cmd.getProjectId()));
            eventPublisher.publishEvent(new RackQueryResult.RackStateLoaded(cmd.getProjectId(), cmd.getUserId(), rack, cmd.getUserId()));
        } catch (Exception e) {
            log.error("[EventBus] LoadRack failed: {}", e.getMessage());
            eventPublisher.publishEvent(new RackErrorResult(cmd.getProjectId(), cmd.getUserId(), e.getMessage(), cmd.getUserId()));
        }
    }
}
