package org.eci.ZwingBackend.shared.events.handlers;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eci.ZwingBackend.rack.application.port.in.ChannelLockCase;
import org.eci.ZwingBackend.shared.events.commands.PlaybackCommand;
import org.eci.ZwingBackend.shared.events.results.PlaybackResult;
import org.eci.ZwingBackend.shared.events.results.RackErrorResult;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.UUID;


@Slf4j
@Component
@AllArgsConstructor
public class PlaybackCommandHandler {
    private final ChannelLockCase channelLockCase;
    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    public void handleStartPlayback(PlaybackCommand.StartPlayback cmd) {
        try {
            if (channelLockCase.isPlaybackLocked(UUID.fromString(cmd.getProjectId()))) {
                eventPublisher.publishEvent(new RackErrorResult(cmd.getProjectId(), cmd.getUserId(), "Playback is already active.", cmd.getUserId()));
                return;
            }

            channelLockCase.acquirePlaybackLock(UUID.fromString(cmd.getProjectId()), UUID.fromString(cmd.getUserId()));
            eventPublisher.publishEvent(new PlaybackResult.PlaybackStarted(cmd.getProjectId(), cmd.getUserId()));
        } catch (Exception e) {
            log.error("[EventBus] StartPlayback failed: {}", e.getMessage());
            eventPublisher.publishEvent(new RackErrorResult(cmd.getProjectId(), cmd.getUserId(), e.getMessage(), cmd.getUserId()));
        }
    }

    @EventListener
    public void handleStopPlayback(PlaybackCommand.StopPlayback cmd) {
        try {
            if (!channelLockCase.isPlaybackLocked(UUID.fromString(cmd.getProjectId()))) {
                eventPublisher.publishEvent(new RackErrorResult(cmd.getProjectId(), cmd.getUserId(), "Playback is not active.", cmd.getUserId()));
                return;
            }

            channelLockCase.releasePlaybackLock(UUID.fromString(cmd.getProjectId()));
            eventPublisher.publishEvent(new PlaybackResult.PlaybackStopped(cmd.getProjectId(), cmd.getUserId()));
        } catch (Exception e) {
            log.error("[EventBus] StopPlayback failed: {}", e.getMessage());
            eventPublisher.publishEvent(new RackErrorResult(cmd.getProjectId(), cmd.getUserId(), e.getMessage(), cmd.getUserId()));
        }
    }
}
