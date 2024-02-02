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
package org.flowable.examples.bpmn.executionlistener;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.impl.el.FixedValue;
import org.flowable.task.service.delegate.DelegateTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaDelegateTestExecutionListener implements JavaDelegate, TaskListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaDelegateTestExecutionListener.class);

    protected FixedValue action;

    @Override
    public void notify(DelegateTask delegateTask) {
        LOGGER.info("Hello TaskListener --- {} ", action.getExpressionText());
        String myActions = delegateTask.getVariable("myActions", String.class);
        myActions = myActions != null ? myActions + ","+ action.getExpressionText()  : action.getExpressionText();
        delegateTask.setVariable("myActions", myActions);
    }

    @Override
    public void execute(DelegateExecution execution) {
        LOGGER.info("Hello ExecutionListener --- {}", action.getExpressionText());
        String myActions = execution.getVariable("myActions", String.class);
        myActions = myActions != null ? myActions + ","+ action.getExpressionText()  : action.getExpressionText();
        execution.setVariable("myActions", myActions);
    }
}
