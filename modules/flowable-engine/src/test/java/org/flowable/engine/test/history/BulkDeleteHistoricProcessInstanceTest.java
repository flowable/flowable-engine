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

package org.flowable.engine.test.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.persistence.entity.ByteArrayEntity;
import org.flowable.engine.impl.persistence.entity.HistoricDetailVariableInstanceUpdateEntity;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.entitylink.api.EntityLinkType;
import org.flowable.entitylink.api.history.HistoricEntityLink;
import org.flowable.task.api.Task;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.junit.jupiter.api.Test;

public class BulkDeleteHistoricProcessInstanceTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/oneTask.bpmn20.xml" })
    public void oneTaskTestWithVariables() {
        HistoryLevel historyLevel = processEngineConfiguration.getHistoryLevel();
        try {
            processEngineConfiguration.setHistoryLevel(HistoryLevel.FULL);
            ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("startToEnd")
                    .variable("testString", "test")
                    .variable("testNumber", 123)
                    .variable("testSerializable", Collections.singletonMap("key", "value"))
                    .start();
            
            runtimeService.addParticipantUser(processInstance.getId(), "johndoe");
            runtimeService.addParticipantGroup(processInstance.getId(), "sales");
            
            taskService.addComment(null, processInstance.getId(), "test");
            
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            taskService.complete(task.getId());
            waitForHistoryJobExecutorToProcessAllJobs(10000, 400);
            
            assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).list()).hasSize(3);
            assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).list()).hasSize(3);
            
            HistoricVariableInstanceEntity variableInstance = (HistoricVariableInstanceEntity) historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).variableName("testSerializable").singleResult();
            assertThat(variableInstance.getByteArrayRef()).isNotNull();
            
            ByteArrayEntity byteArrayEntity = managementService.executeCommand(new Command<ByteArrayEntity>() {

                @Override
                public ByteArrayEntity execute(CommandContext commandContext) {
                    return CommandContextUtil.getByteArrayEntityManager(commandContext).findById(variableInstance.getByteArrayRef().getId());
                }
            });
            
            assertThat(byteArrayEntity.getId()).isEqualTo(variableInstance.getByteArrayRef().getId());
            
            HistoricDetailVariableInstanceUpdateEntity historicDetailEntity = (HistoricDetailVariableInstanceUpdateEntity) historyService.createHistoricDetailQuery().processInstanceId(processInstance.getId()).orderByVariableName().asc().list().get(1);
            assertThat(historicDetailEntity.getByteArrayRef()).isNotNull();
            
            ByteArrayEntity historicDetailByteArrayEntity = managementService.executeCommand(new Command<ByteArrayEntity>() {

                @Override
                public ByteArrayEntity execute(CommandContext commandContext) {
                    return CommandContextUtil.getByteArrayEntityManager(commandContext).findById(historicDetailEntity.getByteArrayRef().getId());
                }
            });
            
            assertThat(historicDetailByteArrayEntity.getId()).isEqualTo(historicDetailEntity.getByteArrayRef().getId());
            assertThat(historicDetailByteArrayEntity.getId()).isNotEqualTo(byteArrayEntity.getId());
            
            assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstance.getId()).list()).hasSize(3);
            assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstance.getId())).hasSize(3);
            assertThat(historyService.getHistoricEntityLinkChildrenForProcessInstance(processInstance.getId())).hasSize(1);
            assertThat(taskService.getProcessInstanceComments(processInstance.getId())).hasSize(3);
            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
            assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
            
            ProcessInstance processInstance2 = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("startToEnd")
                    .variable("testString", "test")
                    .variable("testNumber", 123)
                    .variable("testSerializable", Collections.singletonMap("key", "value"))
                    .start();
            
            ProcessInstance processInstance3 = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("startToEnd")
                    .variable("testString", "test")
                    .variable("testNumber", 123)
                    .variable("testSerializable", Collections.singletonMap("key", "value"))
                    .start();
            
            List<String> processInstanceIds = new ArrayList<>();
            processInstanceIds.add(processInstance.getId());
            processInstanceIds.add(processInstance2.getId());
            processInstanceIds.add(processInstance3.getId());
            historyService.bulkDeleteHistoricProcessInstances(processInstanceIds);
            
            validateEmptyHistoricDataForProcessInstance(processInstance.getId());
            
            ByteArrayEntity byteArrayEntityAfterDelete = managementService.executeCommand(new Command<ByteArrayEntity>() {

                @Override
                public ByteArrayEntity execute(CommandContext commandContext) {
                    return CommandContextUtil.getByteArrayEntityManager(commandContext).findById(byteArrayEntity.getId());
                }
            });
            
            assertThat(byteArrayEntityAfterDelete).isNull();
            
            ByteArrayEntity historicDetailByteArrayEntityAfterDelete = managementService.executeCommand(new Command<ByteArrayEntity>() {

                @Override
                public ByteArrayEntity execute(CommandContext commandContext) {
                    return CommandContextUtil.getByteArrayEntityManager(commandContext).findById(historicDetailByteArrayEntity.getId());
                }
            });
            
            assertThat(historicDetailByteArrayEntityAfterDelete).isNull();
            
            validateEmptyHistoricDataForProcessInstance(processInstance2.getId());
            validateEmptyHistoricDataForProcessInstance(processInstance3.getId());

        } finally {
            processEngineConfiguration.setHistoryLevel(historyLevel);
        }
    }
    
    @Test
    @Deployment(resources = { "org/flowable/engine/test/history/callActivity.bpmn20.xml",
            "org/flowable/engine/test/history/subProcess.bpmn20.xml" })
    public void callActivityTest() {
        HistoryLevel historyLevel = processEngineConfiguration.getHistoryLevel();
        try {
            processEngineConfiguration.setHistoryLevel(HistoryLevel.FULL);
            ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("callActivity")
                    .variable("testString", "test")
                    .variable("testNumber", 123)
                    .variable("testSerializable", Collections.singletonMap("key", "value"))
                    .start();
            
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            taskService.complete(task.getId());
            
            ProcessInstance subProcessInstance = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance.getId()).singleResult();
            
            runtimeService.addParticipantUser(subProcessInstance.getId(), "johndoe");
            runtimeService.addParticipantGroup(subProcessInstance.getId(), "sales");
            
            taskService.addComment(null, subProcessInstance.getId(), "test");
            
            waitForHistoryJobExecutorToProcessAllJobs(10000, 400);
            assertThat(historyService.createHistoricDetailQuery().processInstanceId(subProcessInstance.getId()).list()).hasSize(3);
            assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(subProcessInstance.getId()).list()).hasSize(3);
            
            HistoricVariableInstanceEntity variableInstance = (HistoricVariableInstanceEntity) historyService.createHistoricVariableInstanceQuery().processInstanceId(subProcessInstance.getId()).variableName("localSerializable").singleResult();
            assertThat(variableInstance.getByteArrayRef()).isNotNull();
            
            ByteArrayEntity byteArrayEntity = managementService.executeCommand(new Command<ByteArrayEntity>() {

                @Override
                public ByteArrayEntity execute(CommandContext commandContext) {
                    return CommandContextUtil.getByteArrayEntityManager(commandContext).findById(variableInstance.getByteArrayRef().getId());
                }
            });
            
            assertThat(byteArrayEntity.getId()).isEqualTo(variableInstance.getByteArrayRef().getId());
            
            HistoricDetailVariableInstanceUpdateEntity historicDetailEntity = (HistoricDetailVariableInstanceUpdateEntity) historyService.createHistoricDetailQuery().processInstanceId(subProcessInstance.getId()).orderByVariableName().asc().list().get(1);
            assertThat(historicDetailEntity.getByteArrayRef()).isNotNull();
            
            ByteArrayEntity historicDetailByteArrayEntity = managementService.executeCommand(new Command<ByteArrayEntity>() {

                @Override
                public ByteArrayEntity execute(CommandContext commandContext) {
                    return CommandContextUtil.getByteArrayEntityManager(commandContext).findById(historicDetailEntity.getByteArrayRef().getId());
                }
            });
            
            assertThat(historicDetailByteArrayEntity.getId()).isEqualTo(historicDetailEntity.getByteArrayRef().getId());
            assertThat(historicDetailByteArrayEntity.getId()).isNotEqualTo(byteArrayEntity.getId());
            
            assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(subProcessInstance.getId()).list()).hasSize(3);
            assertThat(historyService.getHistoricIdentityLinksForProcessInstance(subProcessInstance.getId())).hasSize(2);
            assertThat(historyService.getHistoricEntityLinkChildrenForProcessInstance(processInstance.getId())).hasSize(3);
            assertThat(historyService.getHistoricEntityLinkChildrenForProcessInstance(subProcessInstance.getId())).hasSize(1);
            assertThat(taskService.getProcessInstanceComments(subProcessInstance.getId())).hasSize(3);
            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(subProcessInstance.getId()).list()).hasSize(1);
            assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(subProcessInstance.getId()).list()).hasSize(1);
            
            ProcessInstance processInstance2 = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("callActivity")
                    .variable("testString", "test")
                    .variable("testNumber", 123)
                    .variable("testSerializable", Collections.singletonMap("key", "value"))
                    .start();
            
            Task mainTask = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
            taskService.complete(mainTask.getId());
            
            ProcessInstance subProcessInstance2 = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance2.getId()).singleResult();
            
            ProcessInstance processInstance3 = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("callActivity")
                    .variable("testString", "test")
                    .variable("testNumber", 123)
                    .variable("testSerializable", Collections.singletonMap("key", "value"))
                    .start();
            
            mainTask = taskService.createTaskQuery().processInstanceId(processInstance3.getId()).singleResult();
            taskService.complete(mainTask.getId());
            
            ProcessInstance subProcessInstance3 = runtimeService.createProcessInstanceQuery().superProcessInstanceId(processInstance3.getId()).singleResult();
            
            List<String> processInstanceIds = new ArrayList<>();
            processInstanceIds.add(processInstance.getId());
            processInstanceIds.add(processInstance2.getId());
            processInstanceIds.add(processInstance3.getId());
            historyService.bulkDeleteHistoricProcessInstances(processInstanceIds);
            
            validateEmptyHistoricDataForProcessInstance(processInstance.getId());
            validateEmptyHistoricDataForProcessInstance(subProcessInstance.getId());
            
            ByteArrayEntity byteArrayEntityAfterDelete = managementService.executeCommand(new Command<ByteArrayEntity>() {

                @Override
                public ByteArrayEntity execute(CommandContext commandContext) {
                    return CommandContextUtil.getByteArrayEntityManager(commandContext).findById(byteArrayEntity.getId());
                }
            });
            
            assertThat(byteArrayEntityAfterDelete).isNull();
            
            ByteArrayEntity historicDetailByteArrayEntityAfterDelete = managementService.executeCommand(new Command<ByteArrayEntity>() {

                @Override
                public ByteArrayEntity execute(CommandContext commandContext) {
                    return CommandContextUtil.getByteArrayEntityManager(commandContext).findById(historicDetailByteArrayEntity.getId());
                }
            });
            
            assertThat(historicDetailByteArrayEntityAfterDelete).isNull();
            
            validateEmptyHistoricDataForProcessInstance(processInstance2.getId());
            validateEmptyHistoricDataForProcessInstance(subProcessInstance2.getId());
            validateEmptyHistoricDataForProcessInstance(processInstance3.getId());
            validateEmptyHistoricDataForProcessInstance(subProcessInstance3.getId());

        } finally {
            processEngineConfiguration.setHistoryLevel(historyLevel);
        }
    }
    
    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/oneTask.bpmn20.xml" })
    public void oneTaskTestWithLocalVariables() {
        HistoryLevel historyLevel = processEngineConfiguration.getHistoryLevel();
        try {
            processEngineConfiguration.setHistoryLevel(HistoryLevel.FULL);
            ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("startToEnd")
                    .variable("testString", "test")
                    .start();
            
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            
            taskService.setVariableLocal(task.getId(), "testNumber", 123);
            taskService.setVariableLocal(task.getId(), "testSerializable", Collections.singletonMap("key", "value"));
            
            taskService.addCandidateUser(task.getId(), "johndoe");
            taskService.addCandidateGroup(task.getId(), "sales");
            
            taskService.addComment(task.getId(), null, "test");
            
            taskService.complete(task.getId());

            waitForHistoryJobExecutorToProcessAllJobs(10000, 400);
            
            assertThat(historyService.createHistoricDetailQuery().taskId(task.getId()).list()).hasSize(2);
            assertThat(historyService.createHistoricVariableInstanceQuery().taskId(task.getId()).list()).hasSize(2);
            
            HistoricVariableInstanceEntity variableInstance = (HistoricVariableInstanceEntity) historyService.createHistoricVariableInstanceQuery().taskId(task.getId()).variableName("testSerializable").singleResult();
            assertThat(variableInstance.getByteArrayRef()).isNotNull();
            
            ByteArrayEntity byteArrayEntity = managementService.executeCommand(new Command<ByteArrayEntity>() {

                @Override
                public ByteArrayEntity execute(CommandContext commandContext) {
                    return CommandContextUtil.getByteArrayEntityManager(commandContext).findById(variableInstance.getByteArrayRef().getId());
                }
            });
            
            assertThat(byteArrayEntity.getId()).isEqualTo(variableInstance.getByteArrayRef().getId());
            
            HistoricDetailVariableInstanceUpdateEntity historicDetailEntity = (HistoricDetailVariableInstanceUpdateEntity) historyService.createHistoricDetailQuery().taskId(task.getId()).orderByVariableName().asc().list().get(1);
            assertThat(historicDetailEntity.getByteArrayRef()).isNotNull();
            
            ByteArrayEntity historicDetailByteArrayEntity = managementService.executeCommand(new Command<ByteArrayEntity>() {

                @Override
                public ByteArrayEntity execute(CommandContext commandContext) {
                    return CommandContextUtil.getByteArrayEntityManager(commandContext).findById(historicDetailEntity.getByteArrayRef().getId());
                }
            });
            
            assertThat(historicDetailByteArrayEntity.getId()).isEqualTo(historicDetailEntity.getByteArrayRef().getId());
            assertThat(historicDetailByteArrayEntity.getId()).isNotEqualTo(byteArrayEntity.getId());
            
            assertThat(historyService.getHistoricIdentityLinksForTask(task.getId())).hasSize(3);
            assertThat(historyService.getHistoricEntityLinkParentsForTask(task.getId())).hasSize(1);
            assertThat(taskService.getTaskComments(task.getId())).hasSize(1);
            assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstance.getId()).list()).hasSize(1);
            assertThat(historyService.createHistoricTaskInstanceQuery().taskId(task.getId()).list()).hasSize(1);
            
            ProcessInstance processInstance2 = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("startToEnd")
                    .variable("testString", "test")
                    .variable("testNumber", 123)
                    .variable("testSerializable", Collections.singletonMap("key", "value"))
                    .start();
            
            Task task2 = taskService.createTaskQuery().processInstanceId(processInstance2.getId()).singleResult();
            
            taskService.setVariableLocal(task2.getId(), "testNumber", 123);
            taskService.setVariableLocal(task2.getId(), "testSerializable", Collections.singletonMap("key", "value"));
            
            taskService.addCandidateUser(task2.getId(), "johndoe");
            taskService.addCandidateGroup(task2.getId(), "sales");
            
            taskService.addComment(task2.getId(), null, "test");
            
            taskService.complete(task2.getId());
            
            ProcessInstance processInstance3 = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("startToEnd")
                    .variable("testString", "test")
                    .variable("testNumber", 123)
                    .variable("testSerializable", Collections.singletonMap("key", "value"))
                    .start();
            
            Task task3 = taskService.createTaskQuery().processInstanceId(processInstance3.getId()).singleResult();
            
            taskService.setVariableLocal(task3.getId(), "testNumber", 123);
            taskService.setVariableLocal(task3.getId(), "testSerializable", Collections.singletonMap("key", "value"));
            
            taskService.addCandidateUser(task3.getId(), "johndoe");
            taskService.addCandidateGroup(task3.getId(), "sales");
            
            taskService.addComment(task3.getId(), null, "test");
            
            taskService.complete(task3.getId());

            waitForHistoryJobExecutorToProcessAllJobs(10000, 400);
            
            List<String> processInstanceIds = new ArrayList<>();
            processInstanceIds.add(processInstance.getId());
            processInstanceIds.add(processInstance2.getId());
            processInstanceIds.add(processInstance3.getId());
            
            List<String> taskIds = new ArrayList<>();
            taskIds.add(task.getId());
            taskIds.add(task2.getId());
            taskIds.add(task3.getId());
            
            historyService.bulkDeleteHistoricProcessInstances(processInstanceIds);
            
            validateEmptyHistoricDataForProcessInstance(processInstance.getId());
            validateEmptyHistoricDataForTask(task.getId());
            
            ByteArrayEntity byteArrayEntityAfterDelete = managementService.executeCommand(new Command<ByteArrayEntity>() {

                @Override
                public ByteArrayEntity execute(CommandContext commandContext) {
                    return CommandContextUtil.getByteArrayEntityManager(commandContext).findById(byteArrayEntity.getId());
                }
            });
            
            assertThat(byteArrayEntityAfterDelete).isNull();
            
            ByteArrayEntity historicDetailByteArrayEntityAfterDelete = managementService.executeCommand(new Command<ByteArrayEntity>() {

                @Override
                public ByteArrayEntity execute(CommandContext commandContext) {
                    return CommandContextUtil.getByteArrayEntityManager(commandContext).findById(historicDetailByteArrayEntity.getId());
                }
            });
            
            assertThat(historicDetailByteArrayEntityAfterDelete).isNull();
            
            validateEmptyHistoricDataForProcessInstance(processInstance2.getId());
            validateEmptyHistoricDataForTask(task2.getId());
            validateEmptyHistoricDataForProcessInstance(processInstance3.getId());
            validateEmptyHistoricDataForTask(task3.getId());

        } finally {
            processEngineConfiguration.setHistoryLevel(historyLevel);
        }
    }
    
    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/oneTask.bpmn20.xml" })
    public void bulkQueryOnEntityLinksTest() {
        HistoryLevel historyLevel = processEngineConfiguration.getHistoryLevel();
        try {
            processEngineConfiguration.setHistoryLevel(HistoryLevel.FULL);
            ProcessInstance processInstance = runtimeService.createProcessInstanceBuilder()
                    .processDefinitionKey("startToEnd")
                    .variable("testString", "test")
                    .start();
            
            Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            waitForHistoryJobExecutorToProcessAllJobs(10000, 400);
            
            List<String> processInstanceIds = new ArrayList<>();
            processInstanceIds.add(processInstance.getId());
            List<HistoricEntityLink> historicEntityLinks = managementService.executeCommand(new Command<List<HistoricEntityLink>>() {

                @Override
                public List<HistoricEntityLink> execute(CommandContext commandContext) {
                    return CommandContextUtil.getProcessEngineConfiguration(commandContext).getEntityLinkServiceConfiguration().getHistoricEntityLinkEntityManager()
                            .findHistoricEntityLinksWithSameRootScopeForScopeIdsAndScopeType(processInstanceIds, ScopeTypes.BPMN, EntityLinkType.CHILD);
                }
                
            });
            
            assertThat(historicEntityLinks).hasSize(1);
            assertThat(historicEntityLinks.get(0).getReferenceScopeId()).isEqualTo(task.getId());
            assertThat(historicEntityLinks.get(0).getScopeId()).isEqualTo(processInstance.getId());
            
        } finally {
            processEngineConfiguration.setHistoryLevel(historyLevel);
        }
    }

    protected void validateEmptyHistoricDataForProcessInstance(String processInstanceId) {
        assertThat(historyService.createHistoricDetailQuery().processInstanceId(processInstanceId).list()).hasSize(0);
        assertThat(historyService.createHistoricVariableInstanceQuery().processInstanceId(processInstanceId).list()).hasSize(0);
        assertThat(historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).list()).hasSize(0);
        assertThat(historyService.getHistoricIdentityLinksForProcessInstance(processInstanceId)).hasSize(0);
        assertThat(historyService.getHistoricEntityLinkChildrenForProcessInstance(processInstanceId)).hasSize(0);
        assertThat(taskService.getProcessInstanceComments(processInstanceId)).hasSize(0);
        assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).list()).hasSize(0);
        assertThat(historyService.createHistoricTaskInstanceQuery().processInstanceId(processInstanceId).list()).hasSize(0);
    }
    
    protected void validateEmptyHistoricDataForTask(String taskId) {
        assertThat(historyService.createHistoricDetailQuery().taskId(taskId).list()).hasSize(0);
        assertThat(historyService.createHistoricVariableInstanceQuery().taskId(taskId).list()).hasSize(0);
        assertThatThrownBy(() -> {
            historyService.getHistoricIdentityLinksForTask(taskId);
        }).isInstanceOf(FlowableObjectNotFoundException.class);

        assertThat(historyService.getHistoricEntityLinkParentsForTask(taskId)).hasSize(0);
        assertThat(taskService.getTaskComments(taskId)).hasSize(0);
        assertThat(historyService.createHistoricTaskInstanceQuery().taskId(taskId).list()).hasSize(0);
    }
}
