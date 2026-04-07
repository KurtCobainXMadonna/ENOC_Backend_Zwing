package org.eci.ZwingBackend.rack.infraestructure.websocket;

import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.shared.events.commands.ChannelLockCommand;
import org.eci.ZwingBackend.shared.events.commands.ChannelMutationCommand;
import org.eci.ZwingBackend.shared.events.commands.PlaybackCommand;
import org.eci.ZwingBackend.shared.events.commands.RackQueryCommand;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@AllArgsConstructor
public class RackWebSocketController {
    private final ApplicationEventPublisher eventBus;

    @MessageMapping("/rack/{projectId}/load")
    public void loadRack(@DestinationVariable String projectId, SimpMessageHeaderAccessor headers) {
        String userId = getUserId(headers);
        String email = getEmail(headers);

        eventBus.publishEvent(new RackQueryCommand.LoadRack(projectId, userId, email));
    }

    @MessageMapping("/rack/{projectId}/channel/add")
    public void addChannel(@DestinationVariable String projectId, @Payload Map<String, String> payload, SimpMessageHeaderAccessor headers) {
        String userId = getUserId(headers);
        String email = getEmail(headers);

        eventBus.publishEvent(new ChannelMutationCommand.AddChannel(projectId, userId, email, payload.get("name"), payload.get("soundId")));
    }
    @MessageMapping("/rack/{projectId}/channel/{channelId}/remove")
    public void removeChannel(@DestinationVariable String projectId, @DestinationVariable String channelId, SimpMessageHeaderAccessor headers) {
        String userId = getUserId(headers);
        String email = getEmail(headers);

        eventBus.publishEvent(new ChannelMutationCommand.RemoveChannel(projectId, userId, email, channelId));
    }

    @MessageMapping("/rack/{projectId}/channel/{channelId}/step")
    public void toggleStep(@DestinationVariable String projectId, @DestinationVariable String channelId, @Payload Map<String, Integer> payload, SimpMessageHeaderAccessor headers) {
        String userId = getUserId(headers);
        String email = getEmail(headers);

        eventBus.publishEvent(new ChannelMutationCommand.ToggleStep(projectId, userId, email, channelId, payload.get("stepIndex")));
    }


    @MessageMapping("/rack/{projectId}/channel/{channelId}/lock")
    public void lockChannel(@DestinationVariable String projectId, @DestinationVariable String channelId, SimpMessageHeaderAccessor headers) {
        String userId = getUserId(headers);
        String email = getEmail(headers);

        eventBus.publishEvent(new ChannelLockCommand.AcquireLock(projectId, userId, email, channelId));
    }
    @MessageMapping("/rack/{projectId}/channel/{channelId}/unlock")
    public void unlockChannel(@DestinationVariable String projectId, @DestinationVariable String channelId, SimpMessageHeaderAccessor headers) {
        String userId = getUserId(headers);
        String email = getEmail(headers);

        eventBus.publishEvent(new ChannelLockCommand.ReleaseLock(projectId, userId, email, channelId));
    }

    @MessageMapping("/rack/{projectId}/channel/{channelId}/update")
    public void updateChannel(@DestinationVariable String projectId, @DestinationVariable String channelId, @Payload Map<String, Object> payload, SimpMessageHeaderAccessor headers) {
        String userId = getUserId(headers);
        String email = getEmail(headers);

        String soundIdStr = (String) payload.get("soundId");
        eventBus.publishEvent(new ChannelMutationCommand.UpdateChannel(
                projectId, userId, email,
                channelId,
                (String) payload.get("name"),
                soundIdStr,
                payload.get("volume") != null ? ((Number) payload.get("volume")).floatValue() : 1.0f,
                payload.get("active") == null || (Boolean) payload.get("active")));
    }

    @MessageMapping("/rack/{projectId}/bpm/update")
    public void updateBpm(@DestinationVariable String projectId, @Payload Map<String, Integer> payload, SimpMessageHeaderAccessor headers) {
        String userId = getUserId(headers);
        String email = getEmail(headers);

        eventBus.publishEvent(new ChannelMutationCommand.UpdateBpm(
                projectId, userId, email,
                payload.getOrDefault("bpm", 120)));
    }

    @MessageMapping("/rack/{projectId}/playback/start")
    public void startPlayback(@DestinationVariable String projectId, SimpMessageHeaderAccessor headers) {
        String userId = getUserId(headers);
        String email = getEmail(headers);

        eventBus.publishEvent(new PlaybackCommand.StartPlayback(projectId, userId, email));
    }
    @MessageMapping("/rack/{projectId}/playback/stop")
    public void stopPlayback(@DestinationVariable String projectId, SimpMessageHeaderAccessor headers) {
        String userId = getUserId(headers);
        String email = getEmail(headers);

        eventBus.publishEvent(new PlaybackCommand.StopPlayback(projectId, userId, email));
    }


    private String getUserId(SimpMessageHeaderAccessor headers) {
        return (String) headers.getSessionAttributes().get("userId");
    }
    private String getEmail(SimpMessageHeaderAccessor headers) {
        return (String) headers.getSessionAttributes().get("email");
    }
}
