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
package org.flowable.engine.test.bpmn.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

public class IntermediateNoneEventTest extends PluggableFlowableTestCase {

    private static boolean listenerExecuted;

    public static class MyExecutionListener implements ExecutionListener {
        @Override
        public void notify(DelegateExecution execution) {
            listenerExecuted = true;
        }
    }

    @Test
    @Deployment
    public void testIntermediateNoneTimerEvent() throws Exception {
        assertThat(listenerExecuted).isFalse();
        ProcessInstance pi = runtimeService.startProcessInstanceByKey("intermediateNoneEventExample");
        assertProcessEnded(pi.getProcessInstanceId());
        assertThat(listenerExecuted).isTrue();
    }

}