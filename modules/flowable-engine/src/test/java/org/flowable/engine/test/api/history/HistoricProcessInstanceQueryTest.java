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
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.runtime.callback.ProcessInstanceState;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
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
    public void testQueryByCallbackId() {
        deployOneTaskTestProcess();
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .start();
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .callbackId("callbackId")
                .start();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {

            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(2);
            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceCallbackId("callbackId").count()).isEqualTo(1);

            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceCallbackId("callbackId").list())
                    .extracting(HistoricProcessInstance::getId)
                    .contains(processInstance.getId());

            assertThat(historyService.createHistoricProcessInstanceQuery()
                    .or()
                    .processInstanceCallbackId("callbackId")
                    .processInstanceName("doesn't exist")
                    .endOr().list())
                    .extracting(HistoricProcessInstance::getId)
                    .contains(processInstance.getId());

            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceCallbackId("invalid").list()).isEmpty();
        }
    }

    @Test
    public void testQueryByCallbackType() {
        deployOneTaskTestProcess();
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .start();
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .callbackType("callbackType")
                .start();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {

            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(2);
            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceCallbackType("callbackType").count()).isEqualTo(1);

            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceCallbackType("callbackType").list())
                    .extracting(HistoricProcessInstance::getId)
                    .contains(processInstance.getId());

            assertThat(historyService.createHistoricProcessInstanceQuery()
                    .or()
                    .processInstanceCallbackType("callbackType")
                    .processInstanceName("doesn't exist")
                    .endOr().list())
                    .extracting(HistoricProcessInstance::getId)
                    .contains(processInstance.getId());

            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceCallbackType("invalid").list()).isEmpty();
        }
    }

    @Test
    public void testQueryByWithoutCallbackId() {
        deployOneTaskTestProcess();
        runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .callbackId("callbackId")
                .start();

        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .start();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {

            assertThat(historyService.createHistoricProcessInstanceQuery().count()).isEqualTo(2);
            assertThat(historyService.createHistoricProcessInstanceQuery().withoutProcessInstanceCallbackId().count()).isEqualTo(1);

            assertThat(historyService.createHistoricProcessInstanceQuery().withoutProcessInstanceCallbackId().list())
                    .extracting(HistoricProcessInstance::getId)
                    .contains(processInstance.getId());

            assertThat(historyService.createHistoricProcessInstanceQuery()
                    .or()
                    .withoutProcessInstanceCallbackId()
                    .processInstanceName("doesn't exist")
                    .endOr().list())
                    .extracting(HistoricProcessInstance::getId)
                    .contains(processInstance.getId());
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
    @Deployment(resources = { "org/flowable/engine/test/api/runtime/currentActivityTestProcess.bpmn20.xml" })
    public void testQueryByActiveActivityId() {
        ProcessInstance processInstance1 = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("currentActivityProcessTest")
                .start();
        
        Task task = taskService.createTaskQuery().processInstanceId(processInstance1.getId()).singleResult();
        taskService.complete(task.getId());
        
        ProcessInstance processInstance2 = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("currentActivityProcessTest")
                .start();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {

            assertThat(historyService.createHistoricProcessInstanceQuery().activeActivityId("task3").singleResult().getId()).isEqualTo(processInstance1.getId());
            assertThat(historyService.createHistoricProcessInstanceQuery().activeActivityId("task1").singleResult().getId()).isEqualTo(processInstance2.getId());
            assertThat(historyService.createHistoricProcessInstanceQuery().activeActivityId("task2").count()).isZero();
            
            Set<String> activityIds = new HashSet<>();
            activityIds.add("task1");
            activityIds.add("task2");
            assertThat(historyService.createHistoricProcessInstanceQuery().activeActivityIds(activityIds).singleResult().getId()).isEqualTo(processInstance2.getId());
        
            activityIds = new HashSet<>();
            activityIds.add("task1");
            activityIds.add("task3");
            assertThat(historyService.createHistoricProcessInstanceQuery().activeActivityIds(activityIds).count()).isEqualTo(2);
            
            activityIds = new HashSet<>();
            activityIds.add("task2");
            activityIds.add("task3");
            assertThat(historyService.createHistoricProcessInstanceQuery().activeActivityIds(activityIds).singleResult().getId()).isEqualTo(processInstance1.getId());
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

    @Test
    public void testQueryVariableValueEqualsAndNotEquals() {
        deployOneTaskTestProcess();
        ProcessInstance processWithStringValue = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .name("With string value")
                .variable("var", "TEST")
                .start();

        ProcessInstance processWithNullValue = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .name("With null value")
                .variable("var", null)
                .start();

        ProcessInstance processWithLongValue = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .name("With long value")
                .variable("var", 100L)
                .start();

        ProcessInstance processWithDoubleValue = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .name("With double value")
                .variable("var", 45.55)
                .start();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {

            assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("var", "TEST").list())
                    .extracting(HistoricProcessInstance::getName, HistoricProcessInstance::getId)
                    .containsExactlyInAnyOrder(
                            tuple("With null value", processWithNullValue.getId()),
                            tuple("With long value", processWithLongValue.getId()),
                            tuple("With double value", processWithDoubleValue.getId())
                    );

            assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("var", "TEST").list())
                    .extracting(HistoricProcessInstance::getName, HistoricProcessInstance::getId)
                    .containsExactlyInAnyOrder(
                            tuple("With string value", processWithStringValue.getId())
                    );

            assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("var", 100L).list())
                    .extracting(HistoricProcessInstance::getName, HistoricProcessInstance::getId)
                    .containsExactlyInAnyOrder(
                            tuple("With string value", processWithStringValue.getId()),
                            tuple("With null value", processWithNullValue.getId()),
                            tuple("With double value", processWithDoubleValue.getId())
                    );

            assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("var", 100L).list())
                    .extracting(HistoricProcessInstance::getName, HistoricProcessInstance::getId)
                    .containsExactlyInAnyOrder(
                            tuple("With long value", processWithLongValue.getId())
                    );

            assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("var", 45.55).list())
                    .extracting(HistoricProcessInstance::getName, HistoricProcessInstance::getId)
                    .containsExactlyInAnyOrder(
                            tuple("With string value", processWithStringValue.getId()),
                            tuple("With null value", processWithNullValue.getId()),
                            tuple("With long value", processWithLongValue.getId())
                    );

            assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("var", 45.55).list())
                    .extracting(HistoricProcessInstance::getName, HistoricProcessInstance::getId)
                    .containsExactlyInAnyOrder(
                            tuple("With double value", processWithDoubleValue.getId())
                    );

            assertThat(historyService.createHistoricProcessInstanceQuery().variableValueNotEquals("var", "test").list())
                    .extracting(HistoricProcessInstance::getName, HistoricProcessInstance::getId)
                    .containsExactlyInAnyOrder(
                            tuple("With string value", processWithStringValue.getId()),
                            tuple("With null value", processWithNullValue.getId()),
                            tuple("With long value", processWithLongValue.getId()),
                            tuple("With double value", processWithDoubleValue.getId())
                    );

            assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEquals("var", "test").list())
                    .extracting(HistoricProcessInstance::getName, HistoricProcessInstance::getId)
                    .isEmpty();

            assertThat(historyService.createHistoricProcessInstanceQuery().variableValueEqualsIgnoreCase("var", "test").list())
                    .extracting(HistoricProcessInstance::getName, HistoricProcessInstance::getId)
                    .containsExactlyInAnyOrder(
                            tuple("With string value", processWithStringValue.getId())
                    );
        }
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/simpleParallelCallActivity.bpmn20.xml",
            "org/flowable/engine/test/api/simpleInnerCallActivity.bpmn20.xml",
            "org/flowable/engine/test/api/simpleProcessWithUserTasks.bpmn20.xml",
            "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml"
    })
    public void testQueryByRootScopeId() {
        runtimeService.startProcessInstanceByKey("simpleParallelCallActivity");

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleParallelCallActivity");

        ActivityInstance firstLevelCallActivity1 = runtimeService.createActivityInstanceQuery()
                .processInstanceId(processInstance.getId())
                .activityId("callActivity1").singleResult();

        ActivityInstance secondLevelCallActivity1_1 = runtimeService.createActivityInstanceQuery()
                .processInstanceId(firstLevelCallActivity1.getCalledProcessInstanceId())
                .activityId("callActivity1").singleResult();

        ActivityInstance thirdLevelCallActivity1_1_1 = runtimeService.createActivityInstanceQuery()
                .processInstanceId(secondLevelCallActivity1_1.getCalledProcessInstanceId())
                .activityId("callActivity1").singleResult();

        ActivityInstance secondLevelCallActivity1_2 = runtimeService.createActivityInstanceQuery()
                .processInstanceId(firstLevelCallActivity1.getCalledProcessInstanceId())
                .activityId("callActivity2").singleResult();

        ActivityInstance firstLevelCallActivity2 = runtimeService.createActivityInstanceQuery().processInstanceId(processInstance.getId())
                .activityId("callActivity2").singleResult();

        taskService.createTaskQuery().list().forEach(task -> taskService.complete(task.getId()));
        List<HistoricProcessInstance> result = historyService.createHistoricProcessInstanceQuery().processInstanceRootScopeId(processInstance.getId()).list();

        assertThat(result)
                .extracting(HistoricProcessInstance::getId, HistoricProcessInstance::getProcessDefinitionKey)
                .containsExactlyInAnyOrder(
                        tuple(firstLevelCallActivity1.getCalledProcessInstanceId(), "simpleInnerParallelCallActivity"),
                        tuple(secondLevelCallActivity1_1.getCalledProcessInstanceId(), "simpleProcessWithUserTaskAndCallActivity"),
                        tuple(thirdLevelCallActivity1_1_1.getCalledProcessInstanceId(), "oneTaskProcess"),
                        tuple(secondLevelCallActivity1_2.getCalledProcessInstanceId(), "oneTaskProcess"),
                        tuple(firstLevelCallActivity2.getCalledProcessInstanceId(), "oneTaskProcess")
                );

        result = historyService.createHistoricProcessInstanceQuery().or().processInstanceRootScopeId(processInstance.getId()).endOr().list();

        assertThat(result)
                .extracting(HistoricProcessInstance::getId, HistoricProcessInstance::getProcessDefinitionKey)
                .containsExactlyInAnyOrder(
                        tuple(firstLevelCallActivity1.getCalledProcessInstanceId(), "simpleInnerParallelCallActivity"),
                        tuple(secondLevelCallActivity1_1.getCalledProcessInstanceId(), "simpleProcessWithUserTaskAndCallActivity"),
                        tuple(thirdLevelCallActivity1_1_1.getCalledProcessInstanceId(), "oneTaskProcess"),
                        tuple(secondLevelCallActivity1_2.getCalledProcessInstanceId(), "oneTaskProcess"),
                        tuple(firstLevelCallActivity2.getCalledProcessInstanceId(), "oneTaskProcess")
                );
    }

    @Test
    @Deployment(resources = {
            "org/flowable/engine/test/api/simpleParallelCallActivity.bpmn20.xml",
            "org/flowable/engine/test/api/simpleInnerCallActivity.bpmn20.xml",
            "org/flowable/engine/test/api/simpleProcessWithUserTasks.bpmn20.xml",
            "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml"
    })
    public void testQueryByParentScopeId() {
        runtimeService.startProcessInstanceByKey("oneTaskProcess");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("simpleParallelCallActivity");

        ActivityInstance firstLevelCallActivity1 = runtimeService.createActivityInstanceQuery().processInstanceId(processInstance.getId())
                .activityId("callActivity1").singleResult();

        ActivityInstance secondLevelCallActivity1 = runtimeService.createActivityInstanceQuery()
                .processInstanceId(firstLevelCallActivity1.getCalledProcessInstanceId())
                .activityId("callActivity1").singleResult();
        ActivityInstance secondLevelCallActivity2 = runtimeService.createActivityInstanceQuery()
                .processInstanceId(firstLevelCallActivity1.getCalledProcessInstanceId())
                .activityId("callActivity2").singleResult();

        taskService.createTaskQuery().list().forEach(task -> taskService.complete(task.getId()));

        List<HistoricProcessInstance> result = historyService.createHistoricProcessInstanceQuery().processInstanceParentScopeId(processInstance.getId()).list();
        assertThat(result).isEmpty();

        result = historyService.createHistoricProcessInstanceQuery().processInstanceParentScopeId(firstLevelCallActivity1.getCalledProcessInstanceId()).list();

        assertThat(result)
                .extracting(HistoricProcessInstance::getId, HistoricProcessInstance::getProcessDefinitionKey)
                .containsExactlyInAnyOrder(
                        tuple(secondLevelCallActivity1.getCalledProcessInstanceId(), "simpleProcessWithUserTaskAndCallActivity"),
                        tuple(secondLevelCallActivity2.getCalledProcessInstanceId(), "oneTaskProcess")
                );

        result = historyService.createHistoricProcessInstanceQuery().or().processInstanceParentScopeId(firstLevelCallActivity1.getCalledProcessInstanceId())
                .endOr().list();

        assertThat(result)
                .extracting(HistoricProcessInstance::getId, HistoricProcessInstance::getProcessDefinitionKey)
                .containsExactlyInAnyOrder(
                        tuple(secondLevelCallActivity1.getCalledProcessInstanceId(), "simpleProcessWithUserTaskAndCallActivity"),
                        tuple(secondLevelCallActivity2.getCalledProcessInstanceId(), "oneTaskProcess")
                );
    }

    @Test
    public void testIdQueryByDeploymentId() {
        deployOneTaskTestProcess();
        String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
        runtimeService.startProcessInstanceByKey("oneTaskProcess");
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().deploymentId(deploymentId).withoutSorting().returnIdsOnly().singleResult();
            assertThat(historicProcessInstance).isNotNull();
            assertThat(historicProcessInstance.getId()).isNotNull();
            assertThat(historicProcessInstance.getDeploymentId()).isNull();
            assertThat(historicProcessInstance.getProcessDefinitionId()).isNull();
            
            historicProcessInstance = historyService.createHistoricProcessInstanceQuery().deploymentId(deploymentId).withoutSorting().singleResult();
            assertThat(historicProcessInstance).isNotNull();
            assertThat(historicProcessInstance.getId()).isNotNull();
            assertThat(historicProcessInstance.getDeploymentId()).isNotNull();
            assertThat(historicProcessInstance.getProcessDefinitionId()).isNotNull();
            
            historicProcessInstance = historyService.createHistoricProcessInstanceQuery().deploymentId(deploymentId).returnIdsOnly().singleResult();
            assertThat(historicProcessInstance).isNotNull();
            assertThat(historicProcessInstance.getId()).isNotNull();
            
            historicProcessInstance = historyService.createHistoricProcessInstanceQuery().deploymentId("nonexisting").returnIdsOnly().singleResult();
            assertThat(historicProcessInstance).isNull();
        }
    }
    
    @Test
    public void testIdQueryByInvolvedUser() {
        deployOneTaskTestProcess();
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .start();
        runtimeService.addUserIdentityLink(processInstance.getId(), "kermit", "specialLink");

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {

            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().involvedUser("kermit", "specialLink").withoutSorting().returnIdsOnly().singleResult();
            assertThat(historicProcessInstance.getId()).isEqualTo(processInstance.getId());
            assertThat(historicProcessInstance.getProcessDefinitionId()).isNull();
            
            historicProcessInstance = historyService.createHistoricProcessInstanceQuery().involvedUser("kermit", "specialLink").withoutSorting().singleResult();
            assertThat(historicProcessInstance.getId()).isEqualTo(processInstance.getId());
            assertThat(historicProcessInstance.getProcessDefinitionId()).isNotNull();

            historicProcessInstance = historyService.createHistoricProcessInstanceQuery().or().involvedUser("kermit", "specialLink").processDefinitionKey("undefined").endOr().withoutSorting().returnIdsOnly().singleResult();
            assertThat(historicProcessInstance.getId()).isEqualTo(processInstance.getId());
            assertThat(historicProcessInstance.getProcessDefinitionId()).isNull();
            
            historicProcessInstance = historyService.createHistoricProcessInstanceQuery().or().involvedUser("kermit", "specialLink").processDefinitionKey("undefined").endOr().withoutSorting().singleResult();
            assertThat(historicProcessInstance.getId()).isEqualTo(processInstance.getId());
            assertThat(historicProcessInstance.getProcessDefinitionId()).isNotNull();
        }
    }

    @Test
    public void testIncludeDefinedVariables() {
        deployOneTaskTestProcess();
        runtimeService
                .createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .businessKey("testBusinessKey")
                .variable("testVar", "test value")
                .variable("intVar", 123)
                .start();

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {

            HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceBusinessKey("testBusinessKey")
                    .singleResult();
            assertThat(processInstance.getProcessVariables()).isEmpty();

            processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceBusinessKey("testBusinessKey").includeProcessVariables()
                    .singleResult();
            assertThat(processInstance.getProcessVariables())
                    .containsOnly(
                            entry("testVar", "test value"),
                            entry("intVar", 123)
                    );

            processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceBusinessKey("testBusinessKey")
                    .includeProcessVariables(List.of("testVar", "dummy")).singleResult();
            assertThat(processInstance.getProcessVariables())
                    .containsOnly(
                            entry("testVar", "test value")
                    );

            processInstance = historyService.createHistoricProcessInstanceQuery().processInstanceBusinessKey("testBusinessKey")
                    .includeProcessVariables(List.of("unknown", "dummy")).singleResult();
            assertThat(processInstance.getProcessVariables())
                    .isEmpty();
        }
    }

    @Test
    public void testQueryByState() {
        deployOneTaskTestProcess();
        ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .start();

        ProcessInstance processInstance2 = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .start();

        ProcessInstance processInstance3 = runtimeService.createProcessInstanceBuilder()
                .processDefinitionKey("oneTaskProcess")
                .start();

        runtimeService.deleteProcessInstance(processInstance.getId(), "cancel");

        Task task = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
        taskService.complete(task.getId());

        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery().state(ProcessInstanceState.CANCELLED).singleResult();
            assertThat(historicProcessInstance.getId()).isEqualTo(processInstance.getId());

            historicProcessInstance = historyService.createHistoricProcessInstanceQuery().state(ProcessInstanceState.COMPLETED).singleResult();
            assertThat(historicProcessInstance.getId()).isEqualTo(processInstance2.getId());

            historicProcessInstance = historyService.createHistoricProcessInstanceQuery().state(ProcessInstanceState.RUNNING).singleResult();
            assertThat(historicProcessInstance.getId()).isEqualTo(processInstance3.getId());
        }
    }

    @Test
    public void testQueryByFinishedBy() {
        deployOneTaskTestProcess();

        String authenticatedUserId = Authentication.getAuthenticatedUserId();
        try {
            Authentication.setAuthenticatedUserId("elmo");
            ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("oneTaskProcess")
                    .start();

            Authentication.setAuthenticatedUserId("kermit");
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            taskService.complete(task.getId());

            assertThat(historyService.createHistoricProcessInstanceQuery().finishedBy("elmo").count()).isEqualTo(0);
            assertThat(historyService.createHistoricProcessInstanceQuery().finishedBy("kermit").count()).isEqualTo(1);
            assertThat(historyService.createHistoricProcessInstanceQuery().finishedBy("kermit").list().get(0).getId()).isEqualTo(processInstance.getId());
            assertThat(historyService.createHistoricProcessInstanceQuery().finishedBy("kermit").singleResult().getId()).isEqualTo(processInstance.getId());

            assertThat(historyService.createHistoricProcessInstanceQuery()
                    .or()
                    .finishedBy("kermit")
                    .processDefinitionName("undefinedId")
                    .endOr()
                    .count())
                    .isEqualTo(1);
            assertThat(historyService.createHistoricProcessInstanceQuery()
                    .or()
                    .finishedBy("kermit")
                    .processDefinitionName("undefinedId")
                    .endOr()
                    .list().get(0).getId())
                    .isEqualTo(processInstance.getId());
            assertThat(historyService.createHistoricProcessInstanceQuery()
                    .or()
                    .finishedBy("kermit")
                    .processDefinitionName("undefined")
                    .endOr()
                    .singleResult().getId())
                    .isEqualTo(processInstance.getId());

        } finally {
            Authentication.setAuthenticatedUserId(authenticatedUserId);
        }

    }
}
