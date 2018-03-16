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

package org.flowable.engine.test.bpmn.servicetask;

import java.io.Serializable;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.impl.delegate.TriggerableActivityBehavior;

public class TriggerableServiceTask implements JavaDelegate, TriggerableActivityBehavior, Serializable {

    @Override
    public void execute(DelegateExecution execution) {
        incrementCount(execution);
    }

    @Override
    public void trigger(DelegateExecution execution, String signalName, Object signalData) {
        incrementCount(execution);
    }

    public void incrementCount(DelegateExecution execution) {
        String variableName = "count";
        int count = 0;
        if (execution.hasVariable(variableName)) {
             count = (int) execution.getVariable(variableName);
        }
        count++;
        execution.setVariable(variableName, count);
    }
}
