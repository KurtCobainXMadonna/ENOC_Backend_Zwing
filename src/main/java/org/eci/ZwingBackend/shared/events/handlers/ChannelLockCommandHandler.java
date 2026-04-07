package org.eci.ZwingBackend.shared.events.handlers;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eci.ZwingBackend.rack.application.port.in.ChannelLockCase;
import org.eci.ZwingBackend.shared.events.commands.ChannelLockCommand;
import org.eci.ZwingBackend.shared.events.results.ChannelLockResult;
import org.eci.ZwingBackend.shared.events.results.RackErrorResult;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@AllArgsConstructor
public class ChannelLockCommandHandler {
    private final ChannelLockCase channelLockCase;
    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    public void handleAcquireLock(ChannelLockCommand.AcquireLock cmd) {
        try {
            boolean acquired = channelLockCase.acquireChannelLock(UUID.fromString(cmd.getProjectId()), UUID.fromString(cmd.getChannelId()), UUID.fromString(cmd.getUserId()));
            if (acquired) {
                eventPublisher.publishEvent(new ChannelLockResult.LockAcquired(cmd.getProjectId(), cmd.getUserId(), UUID.fromString(cmd.getChannelId()), cmd.getUserEmail()));
            } else {
                String holder = channelLockCase.getChannelLockHolder(UUID.fromString(cmd.getProjectId()), UUID.fromString(cmd.getChannelId()));
                eventPublisher.publishEvent(new ChannelLockResult.LockDenied(cmd.getProjectId(), cmd.getUserId(), cmd.getChannelId(), holder));
            }
        } catch (Exception e) {
            log.error("[EventBus] AcquireLock failed: {}", e.getMessage());
            eventPublisher.publishEvent(new RackErrorResult(cmd.getProjectId(), cmd.getUserId(), e.getMessage(), cmd.getUserId()));
        }
    }

    @EventListener
    public void handleReleaseLock(ChannelLockCommand.ReleaseLock cmd) {
        try {
            channelLockCase.releaseChannelLock(UUID.fromString(cmd.getProjectId()), UUID.fromString(cmd.getChannelId()), UUID.fromString(cmd.getUserId()));
            eventPublisher.publishEvent(new ChannelLockResult.LockReleased(cmd.getProjectId(), cmd.getUserId(), cmd.getChannelId()));
        } catch (Exception e) {
            log.error("[EventBus] ReleaseLock failed: {}", e.getMessage());
            eventPublisher.publishEvent(new RackErrorResult(cmd.getProjectId(), cmd.getUserId(), e.getMessage(), cmd.getUserId()));
        }
    }
}
