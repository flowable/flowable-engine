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
package org.flowable.cmmn.test.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.persistence.entity.ByteArrayEntity;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.Task;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.junit.Test;

public class BulkCaseInstanceDeleteTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    public void oneTaskTestWithVariables() {
        HistoryLevel historyLevel = cmmnEngineConfiguration.getHistoryLevel();
        try {
            cmmnEngineConfiguration.setHistoryLevel(HistoryLevel.FULL);
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneTaskCase")
                    .variable("testVar", "testValue")
                    .variable("numVar", 43)
                    .variable("serializableVar", Collections.singletonMap("key", "value"))
                    .start();
    
            cmmnRuntimeService.addUserIdentityLink(caseInstance.getId(), "johndoe", IdentityLinkType.PARTICIPANT);
            cmmnRuntimeService.addGroupIdentityLink(caseInstance.getId(), "sales", IdentityLinkType.PARTICIPANT);
            
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            cmmnTaskService.complete(task.getId());
            
            assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(3);
            
            HistoricVariableInstanceEntity variableInstance = (HistoricVariableInstanceEntity) cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).variableName("serializableVar").singleResult();
            assertThat(variableInstance.getByteArrayRef()).isNotNull();
            
            ByteArrayEntity byteArrayEntity = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<ByteArrayEntity>() {

                @Override
                public ByteArrayEntity execute(CommandContext commandContext) {
                    return CommandContextUtil.getCmmnEngineConfiguration(commandContext).getByteArrayEntityManager().findById(variableInstance.getByteArrayRef().getId());
                }
            });
            
            assertThat(byteArrayEntity.getId()).isEqualTo(variableInstance.getByteArrayRef().getId());
            assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(3);
            assertThat(cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstance.getId())).hasSize(3);
            assertThat(cmmnHistoryService.getHistoricEntityLinkChildrenForCaseInstance(caseInstance.getId())).hasSize(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(1);
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(1);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstance.getId()).list()).hasSize(1);
    
            CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneTaskCase")
                    .variable("testVar", "testValue")
                    .variable("numVar", 43)
                    .variable("serializableVar", Collections.singletonMap("key", "value"))
                    .start();
            
            CaseInstance caseInstance3 = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneTaskCase")
                    .variable("testVar", "testValue")
                    .variable("numVar", 43)
                    .variable("serializableVar", Collections.singletonMap("key", "value"))
                    .start();
            
            List<String> caseInstanceIds = new ArrayList<>();
            caseInstanceIds.add(caseInstance.getId());
            caseInstanceIds.add(caseInstance2.getId());
            caseInstanceIds.add(caseInstance3.getId());
            cmmnHistoryService.bulkDeleteHistoricCaseInstances(caseInstanceIds);
            
            validateEmptyHistoricDataForCaseInstance(caseInstance.getId());
            
            ByteArrayEntity byteArrayEntityAfterDelete = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<ByteArrayEntity>() {

                @Override
                public ByteArrayEntity execute(CommandContext commandContext) {
                    return CommandContextUtil.getCmmnEngineConfiguration(commandContext).getByteArrayEntityManager().findById(byteArrayEntity.getId());
                }
            });
            
            assertThat(byteArrayEntityAfterDelete).isNull();
            
            validateEmptyHistoricDataForCaseInstance(caseInstance2.getId());
            validateEmptyHistoricDataForCaseInstance(caseInstance3.getId());
            
        } finally {
            cmmnEngineConfiguration.setHistoryLevel(historyLevel);
        }
    }
    
    @Test
    @CmmnDeployment(resources = {"org/flowable/cmmn/test/history/testCaseTask.cmmn", 
            "org/flowable/cmmn/test/one-human-task-model.cmmn"})
    public void subCaseInstanceTest() {
        HistoryLevel historyLevel = cmmnEngineConfiguration.getHistoryLevel();
        try {
            cmmnEngineConfiguration.setHistoryLevel(HistoryLevel.FULL);
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("myCase")
                    .variable("testVar", "testValue")
                    .variable("numVar", 43)
                    .variable("serializableVar", Collections.singletonMap("key", "value"))
                    .start();
            
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            cmmnTaskService.complete(task.getId());
            
            CaseInstance subCaseInstance = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceParentId(caseInstance.getId()).singleResult();
            
            cmmnRuntimeService.addUserIdentityLink(subCaseInstance.getId(), "johndoe", IdentityLinkType.PARTICIPANT);
            cmmnRuntimeService.addGroupIdentityLink(subCaseInstance.getId(), "sales", IdentityLinkType.PARTICIPANT);
            
            assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(subCaseInstance.getId()).list()).hasSize(3);
            
            HistoricVariableInstanceEntity variableInstance = (HistoricVariableInstanceEntity) cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(subCaseInstance.getId()).variableName("localSerializable").singleResult();
            assertThat(variableInstance.getByteArrayRef()).isNotNull();
            
            ByteArrayEntity byteArrayEntity = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<ByteArrayEntity>() {

                @Override
                public ByteArrayEntity execute(CommandContext commandContext) {
                    return CommandContextUtil.getCmmnEngineConfiguration(commandContext).getByteArrayEntityManager().findById(variableInstance.getByteArrayRef().getId());
                }
            });
            
            assertThat(byteArrayEntity.getId()).isEqualTo(variableInstance.getByteArrayRef().getId());
            assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(subCaseInstance.getId()).list()).hasSize(3);
            assertThat(cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(subCaseInstance.getId())).hasSize(3);
            assertThat(cmmnHistoryService.getHistoricEntityLinkChildrenForCaseInstance(caseInstance.getId())).hasSize(3);
            assertThat(cmmnHistoryService.getHistoricEntityLinkChildrenForCaseInstance(subCaseInstance.getId())).hasSize(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(subCaseInstance.getId()).list()).hasSize(1);
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(subCaseInstance.getId()).list()).hasSize(1);
            assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(subCaseInstance.getId()).list()).hasSize(1);
    
            CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("myCase")
                    .variable("testVar", "testValue")
                    .variable("numVar", 43)
                    .variable("serializableVar", Collections.singletonMap("key", "value"))
                    .start();
            
            Task mainTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance2.getId()).singleResult();
            cmmnTaskService.complete(mainTask.getId());
            
            CaseInstance subCaseInstance2 = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceParentId(caseInstance2.getId()).singleResult();
            
            CaseInstance caseInstance3 = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("myCase")
                    .variable("testVar", "testValue")
                    .variable("numVar", 43)
                    .variable("serializableVar", Collections.singletonMap("key", "value"))
                    .start();
            
            mainTask = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance3.getId()).singleResult();
            cmmnTaskService.complete(mainTask.getId());
            
            CaseInstance subCaseInstance3 = cmmnRuntimeService.createCaseInstanceQuery().caseInstanceParentId(caseInstance3.getId()).singleResult();
            
            List<String> caseInstanceIds = new ArrayList<>();
            caseInstanceIds.add(caseInstance.getId());
            caseInstanceIds.add(caseInstance2.getId());
            caseInstanceIds.add(caseInstance3.getId());
            cmmnHistoryService.bulkDeleteHistoricCaseInstances(caseInstanceIds);
            
            validateEmptyHistoricDataForCaseInstance(caseInstance.getId());
            validateEmptyHistoricDataForCaseInstance(subCaseInstance.getId());
            
            ByteArrayEntity byteArrayEntityAfterDelete = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<ByteArrayEntity>() {

                @Override
                public ByteArrayEntity execute(CommandContext commandContext) {
                    return CommandContextUtil.getCmmnEngineConfiguration(commandContext).getByteArrayEntityManager().findById(byteArrayEntity.getId());
                }
            });
            
            assertThat(byteArrayEntityAfterDelete).isNull();
            
            validateEmptyHistoricDataForCaseInstance(caseInstance2.getId());
            validateEmptyHistoricDataForCaseInstance(subCaseInstance2.getId());
            validateEmptyHistoricDataForCaseInstance(caseInstance3.getId());
            validateEmptyHistoricDataForCaseInstance(subCaseInstance3.getId());
            
        } finally {
            cmmnEngineConfiguration.setHistoryLevel(historyLevel);
        }
    }
    
    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/one-human-task-model.cmmn")
    public void oneTaskTestWithLocalVariables() {
        HistoryLevel historyLevel = cmmnEngineConfiguration.getHistoryLevel();
        try {
            cmmnEngineConfiguration.setHistoryLevel(HistoryLevel.FULL);
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneTaskCase")
                    .variable("testVar", "testValue")
                    .variable("numVar", 43)
                    .variable("serializableVar", Collections.singletonMap("key", "value"))
                    .start();
            
            Task task = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).singleResult();
            
            cmmnTaskService.setVariableLocal(task.getId(), "testNumber", 123);
            cmmnTaskService.setVariableLocal(task.getId(), "testSerializable", Collections.singletonMap("key", "value"));
            
            cmmnTaskService.addUserIdentityLink(task.getId(), "johndoe", IdentityLinkType.PARTICIPANT);
            cmmnTaskService.addGroupIdentityLink(task.getId(), "sales", IdentityLinkType.PARTICIPANT);
            
            cmmnTaskService.complete(task.getId());
            
            assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().taskId(task.getId()).list()).hasSize(2);
            
            HistoricVariableInstanceEntity variableInstance = (HistoricVariableInstanceEntity) cmmnHistoryService.createHistoricVariableInstanceQuery().taskId(task.getId()).variableName("testSerializable").singleResult();
            assertThat(variableInstance.getByteArrayRef()).isNotNull();
            
            ByteArrayEntity byteArrayEntity = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<ByteArrayEntity>() {

                @Override
                public ByteArrayEntity execute(CommandContext commandContext) {
                    return CommandContextUtil.getCmmnEngineConfiguration(commandContext).getByteArrayEntityManager().findById(variableInstance.getByteArrayRef().getId());
                }
            });
            
            assertThat(byteArrayEntity.getId()).isEqualTo(variableInstance.getByteArrayRef().getId());
            assertThat(cmmnHistoryService.getHistoricIdentityLinksForTask(task.getId())).hasSize(3);
            assertThat(cmmnHistoryService.getHistoricEntityLinkParentsForTask(task.getId())).hasSize(1);
            assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstance.getId()).list()).hasSize(1);
            assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().taskId(task.getId()).list()).hasSize(1);
    
            CaseInstance caseInstance2 = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneTaskCase")
                    .variable("testVar", "testValue")
                    .variable("numVar", 43)
                    .variable("serializableVar", Collections.singletonMap("key", "value"))
                    .start();
            
            Task task2 = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance2.getId()).singleResult();
            
            cmmnTaskService.setVariableLocal(task2.getId(), "testNumber", 123);
            cmmnTaskService.setVariableLocal(task2.getId(), "testSerializable", Collections.singletonMap("key", "value"));
            
            cmmnTaskService.addUserIdentityLink(task2.getId(), "johndoe", IdentityLinkType.PARTICIPANT);
            cmmnTaskService.addGroupIdentityLink(task2.getId(), "sales", IdentityLinkType.PARTICIPANT);
            
            CaseInstance caseInstance3 = cmmnRuntimeService.createCaseInstanceBuilder()
                    .caseDefinitionKey("oneTaskCase")
                    .variable("testVar", "testValue")
                    .variable("numVar", 43)
                    .variable("serializableVar", Collections.singletonMap("key", "value"))
                    .start();
            
            Task task3 = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance3.getId()).singleResult();
            
            cmmnTaskService.setVariableLocal(task3.getId(), "testNumber", 123);
            cmmnTaskService.setVariableLocal(task3.getId(), "testSerializable", Collections.singletonMap("key", "value"));
            
            cmmnTaskService.addUserIdentityLink(task3.getId(), "johndoe", IdentityLinkType.PARTICIPANT);
            cmmnTaskService.addGroupIdentityLink(task3.getId(), "sales", IdentityLinkType.PARTICIPANT);
            
            List<String> caseInstanceIds = new ArrayList<>();
            caseInstanceIds.add(caseInstance.getId());
            caseInstanceIds.add(caseInstance2.getId());
            caseInstanceIds.add(caseInstance3.getId());
            cmmnHistoryService.bulkDeleteHistoricCaseInstances(caseInstanceIds);
            
            validateEmptyHistoricDataForCaseInstance(caseInstance.getId());
            validateEmptyHistoricDataForTask(task.getId());
            
            ByteArrayEntity byteArrayEntityAfterDelete = cmmnEngineConfiguration.getCommandExecutor().execute(new Command<ByteArrayEntity>() {

                @Override
                public ByteArrayEntity execute(CommandContext commandContext) {
                    return CommandContextUtil.getCmmnEngineConfiguration(commandContext).getByteArrayEntityManager().findById(byteArrayEntity.getId());
                }
            });
            
            assertThat(byteArrayEntityAfterDelete).isNull();
            
            validateEmptyHistoricDataForCaseInstance(caseInstance2.getId());
            validateEmptyHistoricDataForTask(task2.getId());
            validateEmptyHistoricDataForCaseInstance(caseInstance3.getId());
            validateEmptyHistoricDataForTask(task3.getId());
            
        } finally {
            cmmnEngineConfiguration.setHistoryLevel(historyLevel);
        }
    }
    
    protected void validateEmptyHistoricDataForCaseInstance(String caseInstanceId) {
        assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().caseInstanceId(caseInstanceId).list()).hasSize(0);
        assertThat(cmmnHistoryService.getHistoricIdentityLinksForCaseInstance(caseInstanceId)).hasSize(0);
        assertThat(cmmnHistoryService.getHistoricEntityLinkChildrenForCaseInstance(caseInstanceId)).hasSize(0);
        assertThat(cmmnHistoryService.createHistoricCaseInstanceQuery().caseInstanceId(caseInstanceId).list()).hasSize(0);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().caseInstanceId(caseInstanceId).list()).hasSize(0);
        assertThat(cmmnHistoryService.createHistoricPlanItemInstanceQuery().planItemInstanceCaseInstanceId(caseInstanceId).list()).hasSize(0);
        assertThat(cmmnHistoryService.createHistoricMilestoneInstanceQuery().milestoneInstanceCaseInstanceId(caseInstanceId).list()).hasSize(0);
    }
    
    protected void validateEmptyHistoricDataForTask(String taskId) {
        assertThat(cmmnHistoryService.createHistoricVariableInstanceQuery().taskId(taskId).list()).hasSize(0);
        assertThatThrownBy(() -> {
            cmmnHistoryService.getHistoricIdentityLinksForTask(taskId);
        }).isInstanceOf(FlowableObjectNotFoundException.class);

        assertThat(cmmnHistoryService.getHistoricEntityLinkParentsForTask(taskId)).hasSize(0);
        assertThat(cmmnHistoryService.createHistoricTaskInstanceQuery().taskId(taskId).list()).hasSize(0);
    }
}
