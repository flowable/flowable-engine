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

import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.test.Deployment;

/**
 * Test case for {@link FlowableEvent} thrown when a BPMNError is not caught in the process.
 * 
 * @author Frederik Heremans
 */
public class UncaughtErrorEventTest extends PluggableFlowableTestCase {

    private TestFlowableEventListener listener;

    /**
     * Test events related to error-events, thrown from within process-execution (eg. service-task).
     */
    @Deployment
    public void testUncaughtError() throws Exception {
        try {
            runtimeService.startProcessInstanceByKey("errorProcess");
            fail("Exception BPMN  error excepted due to not caught exception");
        } catch (BpmnError e) {

        }
    }

    @Override
    protected void initializeServices() {
        super.initializeServices();

        listener = new TestFlowableEventListener();
        processEngineConfiguration.getEventDispatcher().addEventListener(listener);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        if (listener != null) {
            listener.clearEventsReceived();
            processEngineConfiguration.getEventDispatcher().removeEventListener(listener);
        }
    }
}
