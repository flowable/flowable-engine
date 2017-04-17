package org.flowable.engine.delegate.event.impl;

import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * This class tests {@link FlowableProcessTerminatedEventImpl} implementation
 */
public class FlowableProcessTerminatedEventImplTest {

    @Test
    public void testNonProcessInstanceExecution() {
        // Arrange
        ExecutionEntity execution = Mockito.mock(ExecutionEntity.class);
        when(execution.isProcessInstanceType()).thenReturn(false);

        // Act
        try {
            new FlowableProcessTerminatedEventImpl(execution, null);
            fail("Expected exception was not thrown.");
        } catch(Exception e) {
            // Assert
            assertThat(e, instanceOf(RuntimeException.class));
            assertThat(e.getMessage(), containsString("is not a processInstance"));
        }
    }
}
