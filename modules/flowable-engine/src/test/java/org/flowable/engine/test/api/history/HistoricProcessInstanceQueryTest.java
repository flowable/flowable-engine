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
package org.flowable.engine.test.api.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class HistoricProcessInstanceQueryTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testLocalization() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("historicProcessLocalization");
        String processInstanceId = processInstance.getId();
        org.flowable.task.api.Task task = taskService.createTaskQuery().processInstanceId(processInstanceId).singleResult();
        taskService.complete(task.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            List<HistoricProcessInstance> processes = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).list();
            assertThat(processes)
                    .extracting(HistoricProcessInstance::getName, HistoricProcessInstance::getDescription)
                    .containsExactly(tuple(null, null));

            ObjectNode infoNode = dynamicBpmnService.changeLocalizationName("en-GB", "historicProcessLocalization", "Historic Process Name 'en-GB'");
            dynamicBpmnService.changeLocalizationDescription("en-GB", "historicProcessLocalization", "Historic Process Description 'en-GB'", infoNode);
            dynamicBpmnService.saveProcessDefinitionInfo(processInstance.getProcessDefinitionId(), infoNode);

            dynamicBpmnService.changeLocalizationName("en", "historicProcessLocalization", "Historic Process Name 'en'", infoNode);
            dynamicBpmnService.changeLocalizationDescription("en", "historicProcessLocalization", "Historic Process Description 'en'", infoNode);
            dynamicBpmnService.saveProcessDefinitionInfo(processInstance.getProcessDefinitionId(), infoNode);

            processes = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).list();
            assertThat(processes)
                    .extracting(HistoricProcessInstance::getName, HistoricProcessInstance::getDescription)
                    .containsExactly(tuple(null, null));

            processes = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).locale("en-GB").list();
            assertThat(processes)
                    .extracting(HistoricProcessInstance::getName, HistoricProcessInstance::getDescription)
                    .containsExactly(tuple("Historic Process Name 'en-GB'", "Historic Process Description 'en-GB'"));

            processes = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).listPage(0, 10);
            assertThat(processes)
                    .extracting(HistoricProcessInstance::getName, HistoricProcessInstance::getDescription)
                    .containsExactly(tuple(null, null));

            processes = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).locale("en-GB").listPage(0, 10);
            assertThat(processes)
                    .extracting(HistoricProcessInstance::getName, HistoricProcessInstance::getDescription)
                    .containsExactly(tuple("Historic Process Name 'en-GB'", "Historic Process Description 'en-GB'"));

            HistoricProcessInstance process = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
            assertThat(process.getName()).isNull();
            assertThat(process.getDescription()).isNull();

            process = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).locale("en-GB").singleResult();
            assertThat(processes)
                    .extracting(HistoricProcessInstance::getName, HistoricProcessInstance::getDescription)
                    .containsExactly(tuple("Historic Process Name 'en-GB'", "Historic Process Description 'en-GB'"));

            process = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).locale("en").singleResult();
            assertThat(process.getName()).isEqualTo("Historic Process Name 'en'");
            assertThat(process.getDescription()).isEqualTo("Historic Process Description 'en'");

            process = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).locale("en-AU").withLocalizationFallback()
                    .singleResult();
            assertThat(process.getName()).isEqualTo("Historic Process Name 'en'");
            assertThat(process.getDescription()).isEqualTo("Historic Process Description 'en'");
        }
    }

    @Test
    public void testQueryByDeploymentId() {
        deployOneTaskTestProcess();
        String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
        runtimeService.startProcessInstanceByKey("oneTaskProcess");
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            assertThat(historyService.createHistoricProcessInstanceQuery().deploymentId(deploymentId).singleResult()).isNotNull();
            assertThat(historyService.createHistoricProcessInstanceQuery().deploymentId(deploymentId).count()).isEqualTo(1);
        }
    }

    @Test
    public void testQueryByReferenceId() {
        deployOneTaskTestProcess();
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .referenceId("testReferenceId")
                .start();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {

            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceReferenceId("testReferenceId").list())
                    .extracting(HistoricProcessInstance::getId)
                    .contains(processInstance.getId());

            assertThat(historyService.createHistoricProcessInstanceQuery().or()
                    .processInstanceReferenceId("testReferenceId").processInstanceName("doesn't exist").list())
                    .extracting(HistoricProcessInstance::getId)
                    .contains(processInstance.getId());

            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceReferenceId("invalid").list()).isEmpty();
        }

    }
    
    @Test
    public void testQueryByInvolvedUser() {
        deployOneTaskTestProcess();
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .start();
        runtimeService.addUserIdentityLink(processInstance.getId(), "kermit", "specialLink");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {

            assertThat(historyService.createHistoricProcessInstanceQuery().involvedUser("kermit", "specialLink").count()).isEqualTo(1);
            assertThat(historyService.createHistoricProcessInstanceQuery().involvedUser("kermit", "specialLink").singleResult().getId()).isEqualTo(processInstance.getId());

            assertThat(historyService.createHistoricProcessInstanceQuery().involvedUser("kermit", "undefined").count()).isZero();

            assertThat(historyService.createHistoricProcessInstanceQuery().or().involvedUser("kermit", "specialLink").processDefinitionKey("undefined").endOr().count()).isEqualTo(1);
            assertThat(historyService.createHistoricProcessInstanceQuery().or().involvedUser("kermit", "specialLink").processDefinitionKey("undefined").endOr().singleResult().getId()).isEqualTo(processInstance.getId());

            assertThat(historyService.createHistoricProcessInstanceQuery().or().involvedUser("kermit", "specialLink").processDefinitionKey("subProcessQueryTest").endOr().count()).isEqualTo(1);
            assertThat(historyService.createHistoricProcessInstanceQuery().or().involvedUser("kermit", "specialLink").processDefinitionKey("subProcessQueryTest").endOr().singleResult().getId()).isEqualTo(processInstance.getId());

            assertThat(historyService.createHistoricProcessInstanceQuery().or().involvedUser("kermit", "undefined").processDefinitionKey("undefined").endOr().count()).isZero();
        }
    }
    
    @Test
    public void testQueryByInvolvedGroup() {
        deployOneTaskTestProcess();
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .start();
        runtimeService.addGroupIdentityLink(processInstance.getId(), "sales", "specialLink");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {

            assertThat(historyService.createHistoricProcessInstanceQuery().involvedGroup("sales", "specialLink").count()).isEqualTo(1);
            assertThat(historyService.createHistoricProcessInstanceQuery().involvedGroup("sales", "specialLink").singleResult().getId()).isEqualTo(processInstance.getId());

            assertThat(historyService.createHistoricProcessInstanceQuery().involvedGroup("sales", "undefined").count()).isZero();

            assertThat(historyService.createHistoricProcessInstanceQuery().or().involvedGroup("sales", "specialLink").processDefinitionKey("undefined").endOr().count()).isEqualTo(1);
            assertThat(historyService.createHistoricProcessInstanceQuery().or().involvedGroup("sales", "specialLink").processDefinitionKey("undefined").endOr().singleResult().getId()).isEqualTo(processInstance.getId());

            assertThat(historyService.createHistoricProcessInstanceQuery().or().involvedGroup("sales", "specialLink").processDefinitionKey("subProcessQueryTest").endOr().count()).isEqualTo(1);
            assertThat(historyService.createHistoricProcessInstanceQuery().or().involvedGroup("sales", "specialLink").processDefinitionKey("subProcessQueryTest").endOr().singleResult().getId()).isEqualTo(processInstance.getId());

            assertThat(historyService.createHistoricProcessInstanceQuery().or().involvedGroup("sales", "undefined").processDefinitionKey("undefined").endOr().count()).isZero();
        }

    }

    @Test
    public void testQueryByReferenceType() {
        deployOneTaskTestProcess();
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .referenceType("testReferenceType")
                .start();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {

            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceReferenceType("testReferenceType").list())
                    .extracting(HistoricProcessInstance::getId)
                    .contains(processInstance.getId());

            assertThat(historyService.createHistoricProcessInstanceQuery().or()
                    .processInstanceReferenceType("testReferenceType").processInstanceName("doesn't exist").list())
                    .extracting(HistoricProcessInstance::getId)
                    .contains(processInstance.getId());

            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceReferenceType("invalid").list()).isEmpty();
        }
    }

    @Test
    public void testQueryByReferenceIdAndType() {
        deployOneTaskTestProcess();

        String[] ids = new String[7];
        for (int i = 0; i < ids.length; i++) {
            ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("oneTaskProcess")
                    .referenceId("testReferenceId")
                    .referenceType("testReferenceType")
                    .start();

            ids[i] = processInstance.getId();
        }

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {

            assertThat(historyService.createHistoricProcessInstanceQuery()
                    .processInstanceReferenceId("testReferenceId")
                    .processInstanceReferenceType("testReferenceType").list())
                    .extracting(HistoricProcessInstance::getId)
                    .containsExactlyInAnyOrder(ids);
        }

    }

}
