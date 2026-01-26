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

package org.flowable.assertj.process;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.ListAssert;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.history.HistoricIdentityLink;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstance;

/**
 * @author martin.grofcik
 */
public class HistoricProcessInstanceAssert extends AbstractAssert<HistoricProcessInstanceAssert, HistoricProcessInstance> {

    protected final ProcessServicesProvider processServicesProvider;

    protected HistoricProcessInstanceAssert(ProcessEngine processEngine, HistoricProcessInstance historicProcessInstance) {
        super(historicProcessInstance, HistoricProcessInstanceAssert.class);
        processServicesProvider = ProcessServicesProvider.of(processEngine);
    }

    protected HistoricProcessInstanceAssert(HistoricProcessInstance historicProcessInstance) {
        this(Utils.getProcessEngine(), historicProcessInstance);
    }

    /**
     * Assert <b>historic</b> activities ordered by activity instance start time.
     *
     * @return Assertion of {@link HistoricActivityInstance} list.
     */
    public ListAssert<HistoricActivityInstance> activities() {
        processExistsInHistory();

        return assertThat(processServicesProvider.getHistoryService().createHistoricActivityInstanceQuery().processInstanceId(actual.getId())
                .orderByHistoricActivityInstanceStartTime().desc().list());
    }

    /**
     * Assert <b>historic</b> process instance exists in the history and is finished.
     *
     * @return Historic process instance assertion.
     */
    public HistoricProcessInstanceAssert isFinished() {
        processExistsInHistory();

        if (processServicesProvider.getHistoryService().createHistoricProcessInstanceQuery().finished().processInstanceId(actual.getId()).count() != 1) {
            failWithMessage(Utils.getProcessDescription(actual) + " to be finished, but is running in history.");
        }

        return this;
    }

    public ListAssert<HistoricVariableInstance> variables() {
        processExistsInHistory();

        return assertThat(processServicesProvider.getHistoryService().createHistoricVariableInstanceQuery().processInstanceId(actual.getId()).orderByVariableName().asc().list());
    }

    /**
     * Assert that process instance has variable in <b>history</b>.
     *
     * @param variableName variable to check.
     * @return Historic process instance assertion
     */

    public HistoricProcessInstanceAssert hasVariable(String variableName) {
        processExistsInHistory();

        if (processServicesProvider.getHistoryService().createHistoricProcessInstanceQuery().processInstanceId(actual.getId()).variableExists(variableName).count() != 1) {
            failWithMessage(Utils.getProcessDescription(actual) + " has variable <%s> but variable does not exist in history.", variableName);
        }

        return this;
    }

    /**
     * Assert that process instance does not have variable in <b>history</b>.
     * @param variableName variable to check
     * @return Historic process instance assertion
     */
    public HistoricProcessInstanceAssert doesNotHaveVariable(String variableName) {
        processExistsInHistory();

        if (processServicesProvider.getHistoryService().createHistoricProcessInstanceQuery().processInstanceId(actual.getId()).variableExists(variableName).count() != 0) {
            failWithMessage(Utils.getProcessDescription(actual)+" does not have variable <%s> but variable exists in history.", variableName);
        }

        return this;
    }

    /**
     * Assert that process instance has variable in <b>history</b> with value equals to expectedValue.
     *
     * @param variableName variable to check.
     * @param expectedValue expected variable value.
     * @return Historic process instance assertion
     */
    public HistoricProcessInstanceAssert hasVariableWithValue(String variableName, Object expectedValue) {
        processExistsInHistory();
        hasVariable(variableName);

        HistoricVariableInstance actualVariable = processServicesProvider.getHistoryService().createHistoricVariableInstanceQuery().processInstanceId(actual.getId()).variableName(variableName).singleResult();
        assertThat(actualVariable.getValue()).isEqualTo(expectedValue);
        return this;
    }

    /**
     * Assert list of <b>historic</b> identity links without ordering.
     *
     * @return Assertion of #{@link IdentityLink} list.
     */
    public ListAssert<HistoricIdentityLink> identityLinks() {
        processExistsInHistory();

        return assertThat(processServicesProvider.getHistoryService().getHistoricIdentityLinksForProcessInstance(actual.getId()));
    }

    /**
     * Assert list of user tasks in the <b>history</b> ordered by the task name ascending.
     * Process, Task variables and identityLinks are included.
     *
     * @return Assertion of {@link HistoricTaskInstance} list.
     */
    public ListAssert<HistoricTaskInstance> userTasks() {
        processExistsInHistory();

        return assertThat(processServicesProvider.getHistoryService().createHistoricTaskInstanceQuery().processInstanceId(actual.getId()).orderByTaskName().asc()
                .includeProcessVariables().includeIdentityLinks().includeTaskLocalVariables().list());
    }

    private void processExistsInHistory() {
        isNotNull();
        isInHistory();
    }

    private void isInHistory() {
        if (processServicesProvider.getHistoryService().createHistoricProcessInstanceQuery().processInstanceId(actual.getId()).count() != 1) {
            failWithMessage(Utils.getProcessDescription(actual)+"> exists in history but process instance not found.");
        }
    }

}
