package org.flowable.engine.test.bpmn.event.timer;

import java.time.Instant;

/**
 * author martin.grofcik
 */
public class InstantTimeProvider {
    public Instant getInstantTime() {
        return Instant.ofEpochSecond(100);
    }
}
