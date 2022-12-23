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

package org.flowable.engine.test.bpmn.event.error;

import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.FlowableFutureJavaDelegate;
import org.flowable.engine.impl.delegate.TriggerableActivityBehavior;

/**
 * @author martin.grofcik
 */
public class TriggerBpmnErrorFutureDelegate implements TriggerableActivityBehavior, FlowableFutureJavaDelegate<String, Object> {

    @Override
    public String prepareExecutionData(DelegateExecution execution) {
        return "";
    }

    @Override
    public Object execute(String inputData) {
        return inputData;
    }

    @Override
    public void afterExecution(DelegateExecution execution, Object executionData) {

    }

    @Override
    public void trigger(DelegateExecution execution, String signalEvent, Object signalData) {
        throw new BpmnError("triggerBpmnError", "This is a business fault, which can be caught by a BPMN Error Event.");
    }

    @Override
    public void execute(DelegateExecution execution) {

    }
}
