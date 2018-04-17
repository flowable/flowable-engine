package org.flowable.ui.common.repository;

import org.springframework.stereotype.Component;
import org.springframework.util.IdGenerator;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;

/**
 * {@link IdGenerator} implementation based on the current time and the ethernet address of the machine it is running on.
 * 
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
@Component
public class UuidIdGenerator {

    // different ProcessEngines on the same classloader share one generator.
    protected static volatile TimeBasedGenerator timeBasedGenerator;

    public UuidIdGenerator() {
        ensureGeneratorInitialized();
    }

    protected void ensureGeneratorInitialized() {
        if (timeBasedGenerator == null) {
            synchronized (UuidIdGenerator.class) {
                if (timeBasedGenerator == null) {
                    timeBasedGenerator = Generators.timeBasedGenerator(EthernetAddress.fromInterface());
                }
            }
        }
    }

    public String generateId() {
        return timeBasedGenerator.generate().toString();
    }

}
