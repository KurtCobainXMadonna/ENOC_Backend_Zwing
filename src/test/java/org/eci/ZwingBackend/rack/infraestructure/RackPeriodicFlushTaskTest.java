package org.eci.ZwingBackend.rack.infraestructure;

import org.eci.ZwingBackend.rack.application.service.RackSessionService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.verify;

class RackPeriodicFlushTaskTest {

    @Test
    void flushDirtyRacksDelegatesToRackSessionService() {
        RackSessionService rackSessionService = Mockito.mock(RackSessionService.class);
        RackPeriodicFlushTask task = new RackPeriodicFlushTask(rackSessionService);

        task.flushDirtyRacks();

        verify(rackSessionService).flushAllDirty();
    }
}