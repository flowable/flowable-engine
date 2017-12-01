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
package org.flowable.engine.impl.delegate.invocation;

import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;

/**
 * Class handling invocations of {@link TaskListener TaskListeners}
 * 
 * @author Daniel Meyer
 */
public class TaskListenerInvocation extends DelegateInvocation {

    protected final TaskListener executionListenerInstance;
    protected final DelegateTask delegateTask;

    public TaskListenerInvocation(TaskListener executionListenerInstance, DelegateTask delegateTask) {
        this.executionListenerInstance = executionListenerInstance;
        this.delegateTask = delegateTask;
    }

    @Override
    protected void invoke() {
        executionListenerInstance.notify(delegateTask);
    }

    @Override
    public Object getTarget() {
        return executionListenerInstance;
    }

}
