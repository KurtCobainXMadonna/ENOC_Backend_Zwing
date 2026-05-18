package org.eci.ZwingBackend.rack.infraestructure;

import lombok.AllArgsConstructor;
import org.eci.ZwingBackend.rack.application.service.RackSessionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class RackPeriodicFlushTask {
    private final RackSessionService rackSessionService;

    @Scheduled(fixedDelayString = "30000", initialDelayString = "30000")
    public void flushDirtyRacks() {
        rackSessionService.flushAllDirty();
    }
}
