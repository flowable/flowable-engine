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

package org.activiti.engine.test.bpmn.event.timer;

import org.activiti.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.test.Deployment;

/**
 * @author Saeid Mirzaei Test case for ACT-4066
 */

public class StartTimerEventRepeatWithoutN extends PluggableFlowableTestCase {

    protected long counter;
    protected StartEventListener startEventListener;

    class StartEventListener implements FlowableEventListener {

        @Override
        public void onEvent(FlowableEvent event) {
            if (event.getType().equals(FlowableEngineEventType.TIMER_FIRED)) {
                counter++;
            }
        }

        @Override
        public boolean isFailOnException() {
            return false;
        }
        
        @Override
        public boolean isFireOnTransactionLifecycleEvent() {
            return false;
        }

        @Override
        public String getOnTransaction() {
            return null;
        }

    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        startEventListener = new StartEventListener();
        processEngineConfiguration.getEventDispatcher().addEventListener(startEventListener);
    }

    @Override
    protected void tearDown() throws Exception {
        processEngineConfiguration.getEventDispatcher().removeEventListener(startEventListener);
        super.tearDown();
    }

    @Deployment
    public void testStartTimerEventRepeatWithoutN() {
        counter = 0;

        try {
            waitForJobExecutorToProcessAllJobs(5500, 500);
            fail("job is finished sooner than expected");
        } catch (FlowableException e) {
            assertTrue(e.getMessage().startsWith("time limit"));
            assertTrue(counter >= 2);
        }
    }

}
