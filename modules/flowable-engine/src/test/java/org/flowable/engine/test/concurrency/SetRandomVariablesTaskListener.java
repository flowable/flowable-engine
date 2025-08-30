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

package org.flowable.engine.test.concurrency;

import java.util.Random;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.task.service.delegate.DelegateTask;

/**
 * Tasklistener that sets some random process and task-variables.
 * 
 * @author Frederik Heremans
 */
public class SetRandomVariablesTaskListener implements TaskListener {

    private static final long serialVersionUID = 1L;

    @Override
    public void notify(DelegateTask delegateTask) {
        String varName;
        for (int i = 0; i < 5; i++) {
            varName = "variable-" + new Random().nextInt(10);
            ExecutionEntity execution = CommandContextUtil.getExecutionEntityManager().findById(delegateTask.getExecutionId());
            execution.setVariable(varName, getRandomValue());
        }

        for (int i = 0; i < 5; i++) {
            varName = "task-variable-" + new Random().nextInt(10);
            delegateTask.setVariableLocal(varName, getRandomValue());
        }
    }

    protected Object getRandomValue() {
        return switch (new Random().nextInt(4)) {
            case 0 -> new Random().nextLong();
            case 1 -> new Random().nextDouble();
            case 2 -> "Activiti is a light-weight workflow and Business Process Management (BPM) Platform";
            default -> new Random().nextBoolean();
            // return "Some bytearray".getBytes();
        };
    }

}
