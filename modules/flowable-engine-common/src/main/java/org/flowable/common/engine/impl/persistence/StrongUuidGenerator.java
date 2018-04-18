package org.flowable.common.engine.impl.persistence;

import org.flowable.common.engine.impl.cfg.IdGenerator;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;

/**
 * {@link IdGenerator} implementation based on the current time and the ethernet address of the machine it is running on.
 * 
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class StrongUuidGenerator implements IdGenerator {

    // different ProcessEngines on the same classloader share one generator.
    protected static volatile TimeBasedGenerator timeBasedGenerator;

    public StrongUuidGenerator() {
        ensureGeneratorInitialized();
    }

    protected void ensureGeneratorInitialized() {
        if (timeBasedGenerator == null) {
            synchronized (StrongUuidGenerator.class) {
                if (timeBasedGenerator == null) {
                    timeBasedGenerator = Generators.timeBasedGenerator(EthernetAddress.fromInterface());
                }
            }
        }
    }

    @Override
    public String getNextId() {
        return timeBasedGenerator.generate().toString();
    }

}
