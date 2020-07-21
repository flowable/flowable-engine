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
package org.flowable.eventregistry.test;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.eventregistry.api.management.EventRegistryChangeDetectionExecutor;
import org.flowable.eventregistry.impl.management.DefaultEventRegistryChangeDetectionExecutor;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
@EventConfigurationResource("flowableChangeDetector.eventregistry.cfg.xml")
public class DefaultEventRegistryDataChangeDetectorTest extends AbstractFlowableEventTest {

    @Test
    public void testExecutorServiceAndRunnableCreated() {
        assertThat(eventRegistryEngine.getEventRegistryEngineConfiguration().getEventRegistryChangeDetectionManager()).isNotNull();
        EventRegistryChangeDetectionExecutor eventRegistryChangeDetectionExecutor = eventRegistryEngine.getEventRegistryEngineConfiguration()
                .getEventRegistryChangeDetectionExecutor();
        assertThat(eventRegistryChangeDetectionExecutor).isInstanceOf(DefaultEventRegistryChangeDetectionExecutor.class);

        DefaultEventRegistryChangeDetectionExecutor executor = (DefaultEventRegistryChangeDetectionExecutor) eventRegistryChangeDetectionExecutor;
        assertThat(executor.getScheduledExecutorService()).isNotNull();
        assertThat(executor.getChangeDetectionRunnable()).isNotNull();
    }

}
