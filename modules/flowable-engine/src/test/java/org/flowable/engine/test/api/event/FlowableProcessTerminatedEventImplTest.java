/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.engine.test.api.event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.delegate.event.impl.FlowableProcessTerminatedEventImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
        assertThatThrownBy(() -> new FlowableProcessTerminatedEventImpl(execution, null))
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining("is not a processInstance");
    }
}
