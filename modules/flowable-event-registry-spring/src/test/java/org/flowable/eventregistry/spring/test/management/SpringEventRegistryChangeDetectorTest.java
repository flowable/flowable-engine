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
package org.flowable.eventregistry.spring.test.management;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.flowable.eventregistry.api.management.EventRegistryChangeDetectionExecutor;
import org.flowable.eventregistry.impl.EventRegistryEngine;
import org.flowable.eventregistry.impl.management.DefaultEventRegistryChangeDetectionManager;
import org.flowable.eventregistry.spring.management.DefaultSpringEventRegistryChangeDetectionExecutor;
import org.flowable.eventregistry.spring.test.FlowableEventSpringExtension;
import org.flowable.eventregistry.spring.test.jms.EventRegistryJmsConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Joram Barrez
 */
@SpringJUnitConfig(classes = EventRegistryJmsConfiguration.class)
@ExtendWith(FlowableEventSpringExtension.class)
public class SpringEventRegistryChangeDetectorTest {

    @Autowired
    private EventRegistryEngine eventRegistryEngine;

    @Test
    public void testChangeDetectionRunnableCreatedWhenNotExplicitelyInjected() {
        assertThat(eventRegistryEngine.getEventRegistryEngineConfiguration().getEventRegistryChangeDetectionManager())
            .isInstanceOf(DefaultEventRegistryChangeDetectionManager.class);

        EventRegistryChangeDetectionExecutor eventRegistryChangeDetectionExecutor = eventRegistryEngine.getEventRegistryEngineConfiguration().getEventRegistryChangeDetectionExecutor();
        assertThat(eventRegistryChangeDetectionExecutor).isInstanceOf(DefaultSpringEventRegistryChangeDetectionExecutor.class);

        DefaultSpringEventRegistryChangeDetectionExecutor executor = (DefaultSpringEventRegistryChangeDetectionExecutor) eventRegistryChangeDetectionExecutor;
        assertThat(executor.getTaskScheduler()).isInstanceOf(ThreadPoolTaskScheduler.class);
    }

}
