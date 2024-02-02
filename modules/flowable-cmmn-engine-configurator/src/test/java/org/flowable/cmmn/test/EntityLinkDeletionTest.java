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
package org.flowable.cmmn.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.entitylink.api.EntityLink;
import org.flowable.entitylink.api.EntityLinkType;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class EntityLinkDeletionTest extends AbstractProcessEngineIntegrationTest {

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/oneHumanTaskCase.cmmn")
    public void testEntityLinksDeletedOnRootProcessInstanceComplete() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/cmmn/test/nestedCallActivityProcess2.bpmn20.xml")
            .addClasspathResource("org/flowable/cmmn/test/oneCallActivityHumanTaskCaseProcess.bpmn20.xml")
            .deploy();

        // nestedCallActivityProcess
        //   - oneCallActivity (CallActivity)
        //    - oneHumanTaskCase (CaseTask)
        //      - humanTask
        //   - oneCallActivity (CallActivity)
        //    - oneHumanTaskCase (CaseTask)
        //      - humanTask

        try {
            ProcessInstance processInstance = processEngineRuntimeService.startProcessInstanceByKey("nestedCallActivity");
            assertThat(getRootEntityLinks(processInstance.getId(), ScopeTypes.BPMN)).hasSize(6);

            cmmnTaskService.complete(cmmnTaskService.createTaskQuery().singleResult().getId());
            assertThat(getRootEntityLinks(processInstance.getId(), ScopeTypes.BPMN)).hasSize(12);

            List<String> caseInstanceIds = cmmnRuntimeService.createCaseInstanceQuery().list().stream()
                .map(CaseInstance::getId)
                .collect(Collectors.toList());
            List<String> processInstanceIds = processEngineRuntimeService.createProcessInstanceQuery().list().stream()
                .map(ProcessInstance::getId)
                .collect(Collectors.toList());

            cmmnTaskService.complete(cmmnTaskService.createTaskQuery().singleResult().getId());

            Task task = cmmnTaskService.createTaskQuery().singleResult();
            assertThat(task.getName()).isEqualTo("Task before end");

            // Entity links are only cleaned up when the root instance is deleted.
            // At this point, the two child process instances and two child case instance are deleted,
            // yet the entity links still are there
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();
            assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isEqualTo(1); // the root instance
            assertThat(getRootEntityLinks(processInstance.getId(), ScopeTypes.BPMN)).hasSize(13); // +1 for the task in the root instance

            // All case/process instances still need to have entity links
            for (String caseInstanceId : caseInstanceIds) {
                assertThat(getEntityLinks(caseInstanceId, ScopeTypes.CMMN)).isNotEmpty();
            }
            for (String processInstanceId : processInstanceIds) {
                assertThat(getEntityLinks(processInstanceId, ScopeTypes.BPMN)).isNotEmpty();
            }

            // Completing the root instance, deletes all entity links
            processEngineTaskService.complete(task.getId());
            assertThat(getRootEntityLinks(processInstance.getId(), ScopeTypes.BPMN)).isEmpty();

        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    @Test
    @CmmnDeployment(resources = "org/flowable/cmmn/test/caseWithProcessAndCaseTask.cmmn")
    public void testEntityLinksDeletedOnRootCaseInstanceComplete() {
        Deployment deployment = processEngineRepositoryService.createDeployment()
            .addClasspathResource("org/flowable/cmmn/test/oneHumanTaskCase.cmmn")
            .addClasspathResource("org/flowable/cmmn/test/nestedCallActivityProcess.bpmn20.xml")
            .addClasspathResource("org/flowable/cmmn/test/oneCallActivityHumanTaskCaseProcess.bpmn20.xml")
            .deploy();

        try {
            CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("myCase").start();
            assertThat(getRootEntityLinks(caseInstance.getId(), ScopeTypes.CMMN)).hasSize(11);

            List<String> caseInstanceIds = cmmnRuntimeService.createCaseInstanceQuery().list().stream()
                .map(CaseInstance::getId)
                .collect(Collectors.toList());
            List<String> processInstanceIds = processEngineRuntimeService.createProcessInstanceQuery().list().stream()
                .map(ProcessInstance::getId)
                .collect(Collectors.toList());

            cmmnTaskService.complete(cmmnTaskService.createTaskQuery().taskName("The Task").singleResult().getId());

            // Entity links are only cleaned up when the root instance is deleted.
            // At this point, the two child process instances and two child case instance are deleted,
            // yet the entity links still are there
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isEqualTo(1);
            assertThat(processEngineRuntimeService.createProcessInstanceQuery().count()).isZero(); // the root instance
            assertThat(getRootEntityLinks(caseInstance.getId(), ScopeTypes.CMMN)).hasSize(11);

            // All case/process instances still need to have entity links
            for (String caseInstanceId : caseInstanceIds) {
                assertThat(getEntityLinks(caseInstanceId, ScopeTypes.CMMN)).isNotEmpty();
            }
            for (String processInstanceId : processInstanceIds) {
                assertThat(getEntityLinks(processInstanceId, ScopeTypes.BPMN)).isNotEmpty();
            }

            // Completing the root instance, deletes all entity links
            cmmnTaskService.complete(cmmnTaskService.createTaskQuery().singleResult().getId());
            assertThat(getRootEntityLinks(caseInstance.getId(), ScopeTypes.CMMN)).isEmpty();
            assertThat(cmmnRuntimeService.createCaseInstanceQuery().count()).isZero();

        } finally {
            processEngineRepositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

    protected List<EntityLink> getRootEntityLinks(String rootScopeId, String rootScopeType) {
        ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();
        return processEngineManagementService.executeCommand(commandContext
            -> processEngineConfiguration.getEntityLinkServiceConfiguration().getEntityLinkService()
                .findEntityLinksByRootScopeIdAndRootType(rootScopeId, rootScopeType));
    }

    protected List<EntityLink> getEntityLinks(String scopeId, String scopeType) {
        ProcessEngineConfigurationImpl processEngineConfiguration = (ProcessEngineConfigurationImpl) processEngine.getProcessEngineConfiguration();
        return processEngineManagementService.executeCommand(commandContext
            -> processEngineConfiguration.getEntityLinkServiceConfiguration().getEntityLinkService()
                .findEntityLinksByScopeIdAndType(scopeId, scopeType, EntityLinkType.CHILD));
    }

}
