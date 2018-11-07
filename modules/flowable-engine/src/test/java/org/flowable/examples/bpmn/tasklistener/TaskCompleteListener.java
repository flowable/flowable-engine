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
package org.flowable.examples.bpmn.tasklistener;

import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.ExecutionHelper;
import org.flowable.task.service.delegate.DelegateTask;

/**
 * @author Joram Barrez
 */
public class TaskCompleteListener implements TaskListener {

    private static final long serialVersionUID = 1L;
    private Expression greeter;
    private Expression shortName;

    @Override
    public void notify(DelegateTask delegateTask) {
        ExecutionEntity execution = ExecutionHelper.getExecution(delegateTask.getExecutionId());
        execution.setVariable("greeting", "Hello from " + greeter.getValue(execution));
        execution.setVariable("shortName", shortName.getValue(execution));

        delegateTask.setVariableLocal("myTaskVariable", "test");
    }

}
