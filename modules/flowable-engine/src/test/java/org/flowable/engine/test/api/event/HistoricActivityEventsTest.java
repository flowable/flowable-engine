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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test case for all {@link FlowableEvent}s related to activities.
 *
 * @author Frederik Heremans
 * @author Joram Barrez
 */
public class HistoricActivityEventsTest extends PluggableFlowableTestCase {

    private TestHistoricActivityEventListener listener;

    @BeforeEach
    protected void setUp() throws Exception {

        this.listener = new TestHistoricActivityEventListener();
        processEngineConfiguration.getEventDispatcher().addEventListener(listener);
    }

    @AfterEach
    protected void tearDown() throws Exception {

        if (listener != null) {
            listener.clearEventsReceived();
            processEngineConfiguration.getEventDispatcher().removeEventListener(listener);
        }
    }

    /**
     * Test added to assert the historic activity instance event
     */
    @Test
    @Deployment
    public void testHistoricActivityEventDispatched() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {

            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("TestActivityEvents");
            assertThat(processInstance).isNotNull();

            for (int i = 0; i < 2; i++) {
                taskService.complete(taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult().getId());
            }

            List<FlowableEvent> events = listener.getEventsReceived();

            waitForHistoryJobExecutorToProcessAllJobs(7000, 100);

            int processInstanceCreated = 0;
            int mainStartActivityStarted = 0;
            int mainStartActivityEnded = 0;
            int subProcessActivityStarted = 0;
            int subProcessActivityEnded = 0;
            int subProcessStartActivityStarted = 0;
            int subProcessStartActivityEnded = 0;
            int aActivityStarted = 0;
            int aActivityEnded = 0;
            int bActivityStarted = 0;
            int bActivityEnded = 0;
            int subProcessEndActivityStarted = 0;
            int subProcessEndActivityEnded = 0;
            int mainEndActivityStarted = 0;
            int mainEndActivityEnded = 0;
            int processInstanceEnded = 0;

            // Process instance start
            for (FlowableEvent flowableEvent : events) {
                if (FlowableEngineEventType.HISTORIC_PROCESS_INSTANCE_CREATED == flowableEvent.getType()) {
                    processInstanceCreated++;

                } else if (FlowableEngineEventType.HISTORIC_ACTIVITY_INSTANCE_CREATED == flowableEvent.getType()) {
                    FlowableEntityEvent flowableEntityEvent = (FlowableEntityEvent) flowableEvent;
                    HistoricActivityInstance historicActivityInstance = (HistoricActivityInstance) flowableEntityEvent.getEntity();
                    if ("mainStart".equals(historicActivityInstance.getActivityId())) {
                        mainStartActivityStarted++;

                    } else if ("subProcess".equals(historicActivityInstance.getActivityId())) {
                        subProcessActivityStarted++;

                    } else if ("subProcessStart".equals(historicActivityInstance.getActivityId())) {
                        subProcessStartActivityStarted++;

                    } else if ("a".equals(historicActivityInstance.getActivityId())) {
                        aActivityStarted++;

                    } else if ("b".equals(historicActivityInstance.getActivityId())) {
                        bActivityStarted++;

                    } else if ("subprocessEnd".equals(historicActivityInstance.getActivityId())) {
                        subProcessEndActivityStarted++;

                    } else if ("mainEnd".equals(historicActivityInstance.getActivityId())) {
                        mainEndActivityStarted++;
                    }

                } else if (FlowableEngineEventType.HISTORIC_ACTIVITY_INSTANCE_ENDED == flowableEvent.getType()) {
                    FlowableEntityEvent flowableEntityEvent = (FlowableEntityEvent) flowableEvent;
                    HistoricActivityInstance historicActivityInstance = (HistoricActivityInstance) flowableEntityEvent.getEntity();
                    if ("mainStart".equals(historicActivityInstance.getActivityId())) {
                        assertThat(historicActivityInstance.getEndTime()).isNotNull();
                        mainStartActivityEnded++;

                    } else if ("subProcess".equals(historicActivityInstance.getActivityId())) {
                        assertThat(historicActivityInstance.getEndTime()).isNotNull();
                        subProcessActivityEnded++;

                    } else if ("subProcessStart".equals(historicActivityInstance.getActivityId())) {
                        assertThat(historicActivityInstance.getEndTime()).isNotNull();
                        subProcessStartActivityEnded++;

                    } else if ("a".equals(historicActivityInstance.getActivityId())) {
                        assertThat(historicActivityInstance.getEndTime()).isNotNull();
                        aActivityEnded++;

                    } else if ("b".equals(historicActivityInstance.getActivityId())) {
                        assertThat(historicActivityInstance.getEndTime()).isNotNull();
                        bActivityEnded++;

                    } else if ("subprocessEnd".equals(historicActivityInstance.getActivityId())) {
                        assertThat(historicActivityInstance.getEndTime()).isNotNull();
                        subProcessEndActivityEnded++;

                    } else if ("mainEnd".equals(historicActivityInstance.getActivityId())) {
                        assertThat(historicActivityInstance.getEndTime()).isNotNull();
                        mainEndActivityEnded++;
                    }

                } else if (FlowableEngineEventType.HISTORIC_PROCESS_INSTANCE_ENDED == flowableEvent.getType()) {
                    processInstanceEnded++;
                }
            }

            assertThat(processInstanceCreated).isEqualTo(1);
            assertThat(mainStartActivityStarted).isEqualTo(1);
            assertThat(mainStartActivityEnded).isEqualTo(1);
            assertThat(subProcessActivityStarted).isEqualTo(1);
            assertThat(subProcessActivityEnded).isEqualTo(1);
            assertThat(subProcessStartActivityStarted).isEqualTo(1);
            assertThat(subProcessStartActivityEnded).isEqualTo(1);
            assertThat(aActivityStarted).isEqualTo(1);
            assertThat(aActivityEnded).isEqualTo(1);
            assertThat(bActivityStarted).isEqualTo(1);
            assertThat(bActivityEnded).isEqualTo(1);
            assertThat(subProcessEndActivityStarted).isEqualTo(1);
            assertThat(subProcessEndActivityEnded).isEqualTo(1);
            assertThat(mainEndActivityStarted).isEqualTo(1);
            assertThat(mainEndActivityEnded).isEqualTo(1);
            assertThat(processInstanceEnded).isEqualTo(1);
        }
    }

}
