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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

/**
 * @author Yvo Swillens
 */
public class ExecutionListenerOnTransactionTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testOnClosedExecutionListenersWithRollback() {

        CurrentActivityTransactionDependentExecutionListener.clear();

        Map<String, Object> variables = new HashMap<>();
        variables.put("serviceTask1", false);
        variables.put("serviceTask2", false);
        variables.put("serviceTask3", true);

        processEngineConfiguration.setAsyncExecutorActivate(false);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("transactionDependentExecutionListenerProcess", variables);

        // execute the only job that should be there 1 time
        assertThatThrownBy(() -> managementService.executeJob(managementService.createJobQuery().singleResult().getId()))
                .as("serviceTask3 throws exception")
                .isInstanceOf(Exception.class);

        List<CurrentActivityTransactionDependentExecutionListener.CurrentActivity> currentActivities = CurrentActivityTransactionDependentExecutionListener.getCurrentActivities();
        assertThat(currentActivities)
                .extracting(CurrentActivityTransactionDependentExecutionListener.CurrentActivity::getActivityId,
                        CurrentActivityTransactionDependentExecutionListener.CurrentActivity::getActivityName)
                .containsExactly(
                        tuple("serviceTask1", "Service Task 1")
                );
        assertThat(currentActivities.get(0).getProcessInstanceId()).isEqualTo(processInstance.getId());

        assertThat(managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).count()).isEqualTo(1);
    }

    @Test
    @Deployment
    public void testOnCloseFailureExecutionListenersWithRollback() {

        CurrentActivityTransactionDependentExecutionListener.clear();

        Map<String, Object> variables = new HashMap<>();
        variables.put("serviceTask1", false);
        variables.put("serviceTask2", false);
        variables.put("serviceTask3", true);

        processEngineConfiguration.setAsyncExecutorActivate(false);

        runtimeService.startProcessInstanceByKey("transactionDependentExecutionListenerProcess", variables);

        // execute the only job that should be there 1 time
        assertThatThrownBy(() -> managementService.executeJob(managementService.createJobQuery().singleResult().getId()))
                .as("serviceTask3 throws exception")
                .isInstanceOf(Exception.class);

        List<CurrentActivityTransactionDependentExecutionListener.CurrentActivity> currentActivities = CurrentActivityTransactionDependentExecutionListener.getCurrentActivities();
        assertThat(currentActivities)
                .extracting(CurrentActivityTransactionDependentExecutionListener.CurrentActivity::getActivityId,
                        CurrentActivityTransactionDependentExecutionListener.CurrentActivity::getActivityName)
                .containsExactly(
                        // the before commit listener
                        tuple("serviceTask1", "Service Task 1"),
                        // the before rolled-back listener
                        tuple("serviceTask3", "Service Task 3")
                );
    }

    @Test
    @Deployment
    public void testOnClosedExecutionListenersWithExecutionVariables() {

        CurrentActivityTransactionDependentExecutionListener.clear();

        runtimeService.startProcessInstanceByKey("transactionDependentExecutionListenerProcess");

        List<CurrentActivityTransactionDependentExecutionListener.CurrentActivity> currentActivities = CurrentActivityTransactionDependentExecutionListener.getCurrentActivities();
        assertThat(currentActivities)
                .extracting(CurrentActivityTransactionDependentExecutionListener.CurrentActivity::getActivityId,
                        CurrentActivityTransactionDependentExecutionListener.CurrentActivity::getActivityName)
                .containsExactly(
                        tuple("serviceTask1", "Service Task 1"),
                        tuple("serviceTask2", "Service Task 2"),
                        tuple("serviceTask3", "Service Task 3")
                );

        assertThat(currentActivities.get(0).getExecutionVariables()).isEmpty();
        assertThat(currentActivities.get(1).getExecutionVariables())
                .containsExactly(entry("injectedExecutionVariable", "test1"));
        assertThat(currentActivities.get(2).getExecutionVariables())
                .containsExactly(entry("injectedExecutionVariable", "test2"));
    }

    @Test
    @Deployment
    public void testOnCloseFailureExecutionListenersWithTransactionalOperation() {

        MyTransactionalOperationTransactionDependentExecutionListener.clear();

        ProcessInstance firstProcessInstance = runtimeService.startProcessInstanceByKey("transactionDependentExecutionListenerProcess");
        assertProcessEnded(firstProcessInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();
            assertThat(historicProcessInstances)
                    .extracting(HistoricProcessInstance::getProcessDefinitionKey)
                    .containsExactly("transactionDependentExecutionListenerProcess");
        }

        ProcessInstance secondProcessInstance = runtimeService.startProcessInstanceByKey("secondTransactionDependentExecutionListenerProcess");
        assertProcessEnded(secondProcessInstance.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            // first historic process instance was deleted by execution listener
            List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery().list();
            assertThat(historicProcessInstances)
                    .extracting(HistoricProcessInstance::getProcessDefinitionKey)
                    .containsExactly("secondTransactionDependentExecutionListenerProcess");
        }

        List<MyTransactionalOperationTransactionDependentExecutionListener.CurrentActivity> currentActivities = MyTransactionalOperationTransactionDependentExecutionListener.getCurrentActivities();
        assertThat(currentActivities)
                .extracting(MyTransactionalOperationTransactionDependentExecutionListener.CurrentActivity::getActivityId,
                        MyTransactionalOperationTransactionDependentExecutionListener.CurrentActivity::getActivityName)
                .containsExactly(
                        tuple("serviceTask1", "Service Task 1")
                );
    }

    @Test
    @Deployment
    public void testOnClosedExecutionListenersWithCustomPropertiesResolver() {

        MyTransactionalOperationTransactionDependentExecutionListener.clear();

        runtimeService.startProcessInstanceByKey("transactionDependentExecutionListenerProcess");

        List<CurrentActivityTransactionDependentExecutionListener.CurrentActivity> currentActivities = CurrentActivityTransactionDependentExecutionListener.getCurrentActivities();
        assertThat(currentActivities)
                .extracting(MyTransactionalOperationTransactionDependentExecutionListener.CurrentActivity::getActivityId,
                        MyTransactionalOperationTransactionDependentExecutionListener.CurrentActivity::getActivityName)
                .containsExactly(tuple("serviceTask1", "Service Task 1"));
        assertThat(currentActivities.get(0).getCustomPropertiesMap())
                .containsExactly(entry("customProp1", "serviceTask1"));
    }

}
