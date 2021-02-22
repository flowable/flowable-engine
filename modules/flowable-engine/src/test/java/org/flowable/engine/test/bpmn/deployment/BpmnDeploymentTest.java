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

package org.flowable.engine.test.bpmn.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;
import java.util.List;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.util.IoUtil;
import org.flowable.common.engine.impl.util.ReflectUtil;
import org.flowable.engine.impl.RepositoryServiceImpl;
import org.flowable.engine.impl.context.Context;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.DeploymentProperties;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.test.Deployment;
import org.flowable.engine.test.DeploymentId;
import org.flowable.validation.validator.Problems;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 * @author Erik Winlof
 */
public class BpmnDeploymentTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testGetBpmnXmlFileThroughService() {
        String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
        List<String> deploymentResources = repositoryService.getDeploymentResourceNames(deploymentId);

        // verify bpmn file name
        String bpmnResourceName = "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testGetBpmnXmlFileThroughService.bpmn20.xml";
        assertThat(deploymentResources)
                .containsExactly(bpmnResourceName);

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        assertThat(processDefinition.getResourceName()).isEqualTo(bpmnResourceName);
        assertThat(processDefinition.getDiagramResourceName()).isNull();
        assertThat(processDefinition.hasStartFormKey()).isFalse();

        ProcessDefinition readOnlyProcessDefinition = ((RepositoryServiceImpl) repositoryService).getDeployedProcessDefinition(processDefinition.getId());
        assertThat(readOnlyProcessDefinition.getDiagramResourceName()).isNull();

        // verify content
        InputStream deploymentInputStream = repositoryService.getResourceAsStream(deploymentId, bpmnResourceName);
        String contentFromDeployment = readInputStreamToString(deploymentInputStream);
        assertThat(contentFromDeployment).contains("process id=\"emptyProcess\"");

        InputStream fileInputStream = ReflectUtil.getResourceAsStream(
                "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testGetBpmnXmlFileThroughService.bpmn20.xml");
        String contentFromFile = readInputStreamToString(fileInputStream);
        assertThat(contentFromDeployment).isEqualTo(contentFromFile);
    }

    private String readInputStreamToString(InputStream inputStream) {
        byte[] bytes = IoUtil.readInputStream(inputStream, "input stream");
        return new String(bytes);
    }

    @Test
    public void testViolateBPMNIdMaximumLength() {
        assertThatThrownBy(() ->
                repositoryService.createDeployment()
                        .addClasspathResource("org/flowable/engine/test/bpmn/deployment/definitionWithLongTargetNamespace.bpmn20.xml")
                        .deploy())
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining(Problems.BPMN_MODEL_TARGET_NAMESPACE_TOO_LONG);

        // Verify that nothing is deployed
        assertThat(repositoryService.createDeploymentQuery().count()).isZero();
    }

    @Test
    public void testViolateProcessDefinitionIdMaximumLength() {
        assertThatThrownBy(() ->
                repositoryService.createDeployment()
                        .addClasspathResource("org/flowable/engine/test/bpmn/deployment/processWithLongId.bpmn20.xml")
                        .deploy())
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining(Problems.PROCESS_DEFINITION_ID_TOO_LONG);

        // Verify that nothing is deployed
        assertThat(repositoryService.createDeploymentQuery().count()).isZero();
    }

    @Test
    public void testViolateProcessDefinitionNameAndDescriptionMaximumLength() {
        assertThatThrownBy(() ->
                repositoryService.createDeployment()
                        .addClasspathResource("org/flowable/engine/test/bpmn/deployment/processWithLongNameAndDescription.bpmn20.xml")
                        .deploy())
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining(Problems.PROCESS_DEFINITION_NAME_TOO_LONG)
                .hasMessageContaining(Problems.PROCESS_DEFINITION_DOCUMENTATION_TOO_LONG);

        // Verify that nothing is deployed
        assertThat(repositoryService.createDeploymentQuery().count()).isZero();
    }

    @Test
    public void testViolateDefinitionTargetNamespaceMaximumLength() {
        assertThatThrownBy(() ->
                repositoryService.createDeployment()
                        .addClasspathResource("org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.definitionWithLongTargetNamespace.bpmn20.xml")
                        .deploy())
                .isExactlyInstanceOf(FlowableException.class)
                .hasMessageContaining(Problems.BPMN_MODEL_TARGET_NAMESPACE_TOO_LONG);

        // Verify that nothing is deployed
        assertThat(repositoryService.createDeploymentQuery().count()).isZero();
    }

    @Test
    public void testDeploySameFileTwice() {
        String bpmnResourceName = "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testGetBpmnXmlFileThroughService.bpmn20.xml";
        repositoryService.createDeployment().enableDuplicateFiltering().addClasspathResource(bpmnResourceName).name("twice").deploy();

        String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
        List<String> deploymentResources = repositoryService.getDeploymentResourceNames(deploymentId);

        // verify bpmn file name
        assertThat(deploymentResources)
                .containsExactly(bpmnResourceName);

        repositoryService.createDeployment().enableDuplicateFiltering().addClasspathResource(bpmnResourceName).name("twice").deploy();
        List<org.flowable.engine.repository.Deployment> deploymentList = repositoryService.createDeploymentQuery().list();
        assertThat(deploymentList).hasSize(1);

        repositoryService.deleteDeployment(deploymentId);
    }
    
    @Test
    public void testDeploySameFileTwiceAfterInitialDeployment() {
        String bpmnResourceName = "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testProcessDiagramResource.bpmn20.xml";
        repositoryService.createDeployment().enableDuplicateFiltering().addClasspathResource(bpmnResourceName).name("twice").deploy();
        
        bpmnResourceName = "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testGetBpmnXmlFileThroughService.bpmn20.xml";
        repositoryService.createDeployment().enableDuplicateFiltering().addClasspathResource(bpmnResourceName).name("twice").deploy();

        List<org.flowable.engine.repository.Deployment> deploymentList = repositoryService.createDeploymentQuery().orderByDeploymentTime().desc().list();
        assertThat(deploymentList).hasSize(2);
        List<String> deploymentResources = repositoryService.getDeploymentResourceNames(deploymentList.get(0).getId());

        // verify bpmn file name
        assertThat(deploymentResources)
                .containsExactly(bpmnResourceName);

        repositoryService.createDeployment().enableDuplicateFiltering().addClasspathResource(bpmnResourceName).name("twice").deploy();
        deploymentList = repositoryService.createDeploymentQuery().list();
        assertThat(deploymentList).hasSize(2);

        for (org.flowable.engine.repository.Deployment deployment : deploymentList) {
            repositoryService.deleteDeployment(deployment.getId());
        }
    }

    @Test
    public void testDeployTwoProcessesWithDuplicateIdAtTheSameTime() {
        String bpmnResourceName = "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testGetBpmnXmlFileThroughService.bpmn20.xml";
        String bpmnResourceName2 = "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testGetBpmnXmlFileThroughService2.bpmn20.xml";
        assertThatThrownBy(() -> repositoryService.createDeployment().enableDuplicateFiltering().addClasspathResource(bpmnResourceName).addClasspathResource(bpmnResourceName2).name("duplicateAtTheSameTime").deploy());
        // Verify that nothing is deployed
        assertThat(repositoryService.createDeploymentQuery().count()).isZero();
    }

    @Test
    public void testDeployDifferentFiles() {
        String bpmnResourceName = "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testGetBpmnXmlFileThroughService.bpmn20.xml";
        repositoryService.createDeployment().enableDuplicateFiltering().addClasspathResource(bpmnResourceName).name("twice").deploy();

        String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
        List<String> deploymentResources = repositoryService.getDeploymentResourceNames(deploymentId);

        // verify bpmn file name
        assertThat(deploymentResources)
                .containsExactly(bpmnResourceName);

        bpmnResourceName = "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testProcessDiagramResource.bpmn20.xml";
        repositoryService.createDeployment().enableDuplicateFiltering().addClasspathResource(bpmnResourceName).name("twice").deploy();
        List<org.flowable.engine.repository.Deployment> deploymentList = repositoryService.createDeploymentQuery().list();
        assertThat(deploymentList).hasSize(2);

        for (org.flowable.engine.repository.Deployment deployment : deploymentList) {
            repositoryService.deleteDeployment(deployment.getId());
        }
    }

    @Test
    @Deployment
    public void testStartFormKey() {
        String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
        List<String> deploymentResources = repositoryService.getDeploymentResourceNames(deploymentId);

        // verify bpmn file name
        String bpmnResourceName = "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testStartFormKey.bpmn20.xml";
        assertThat(deploymentResources)
                .containsExactly(bpmnResourceName);

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        assertThat(processDefinition.getResourceName()).isEqualTo(bpmnResourceName);
        assertThat(processDefinition.getDiagramResourceName()).isNull();
        assertThat(processDefinition.hasStartFormKey()).isTrue();
    }

    @Test
    public void testDiagramCreationDisabled() {
        // disable diagram generation
        processEngineConfiguration.setCreateDiagramOnDeploy(false);

        try {
            repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/bpmn/parse/BpmnParseTest.testParseDiagramInterchangeElements.bpmn20.xml").deploy();

            // Graphical information is not yet exposed publicly, so we need to
            // do some plumbing
            CommandExecutor commandExecutor = processEngineConfiguration.getCommandExecutor();
            ProcessDefinition processDefinition = commandExecutor.execute(new Command<ProcessDefinition>() {
                @Override
                public ProcessDefinition execute(CommandContext commandContext) {
                    return Context.getProcessEngineConfiguration().getDeploymentManager().findDeployedLatestProcessDefinitionByKey("myProcess");
                }
            });

            assertThat(processDefinition).isNotNull();
            BpmnModel processModel = repositoryService.getBpmnModel(processDefinition.getId());
            assertThat(processModel.getMainProcess().getFlowElements()).hasSize(14);
            assertThat(processModel.getMainProcess().findFlowElementsOfType(SequenceFlow.class)).hasSize(7);

            // Check that no diagram has been created
            List<String> resourceNames = repositoryService.getDeploymentResourceNames(processDefinition.getDeploymentId());
            assertThat(resourceNames).hasSize(1);

            repositoryService.deleteDeployment(repositoryService.createDeploymentQuery().singleResult().getId(), true);
        } finally {
            processEngineConfiguration.setCreateDiagramOnDeploy(true);
        }
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testProcessDiagramResource.bpmn20.xml",
            "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testProcessDiagramResource.jpg" })
    public void testProcessDiagramResource(@DeploymentId String deploymentIdFromDeploymentAnnotation) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

        assertThat(processDefinition.getResourceName()).isEqualTo("org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testProcessDiagramResource.bpmn20.xml");
        BpmnModel processModel = repositoryService.getBpmnModel(processDefinition.getId());
        List<StartEvent> startEvents = processModel.getMainProcess().findFlowElementsOfType(StartEvent.class);
        assertThat(startEvents)
                .extracting(StartEvent::getFormKey)
                .containsExactly("someFormKey");

        String diagramResourceName = processDefinition.getDiagramResourceName();
        assertThat(diagramResourceName).isEqualTo("org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testProcessDiagramResource.jpg");

        InputStream diagramStream = repositoryService.getResourceAsStream(deploymentIdFromDeploymentAnnotation,
                "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testProcessDiagramResource.jpg");
        byte[] diagramBytes = IoUtil.readInputStream(diagramStream, "diagram stream");
        assertThat(diagramBytes).hasSize(33343);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testMultipleDiagramResourcesProvided.bpmn20.xml",
            "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testMultipleDiagramResourcesProvided.a.jpg",
            "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testMultipleDiagramResourcesProvided.b.jpg",
            "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testMultipleDiagramResourcesProvided.c.jpg" })
    public void testMultipleDiagramResourcesProvided() {
        ProcessDefinition processA = repositoryService.createProcessDefinitionQuery().processDefinitionKey("a").singleResult();
        ProcessDefinition processB = repositoryService.createProcessDefinitionQuery().processDefinitionKey("b").singleResult();
        ProcessDefinition processC = repositoryService.createProcessDefinitionQuery().processDefinitionKey("c").singleResult();

        assertThat(processA.getDiagramResourceName()).isEqualTo("org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testMultipleDiagramResourcesProvided.a.jpg");
        assertThat(processB.getDiagramResourceName()).isEqualTo("org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testMultipleDiagramResourcesProvided.b.jpg");
        assertThat(processC.getDiagramResourceName()).isEqualTo("org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testMultipleDiagramResourcesProvided.c.jpg");
    }

    @Test
    @Deployment
    public void testProcessDefinitionDescription() {
        String id = repositoryService.createProcessDefinitionQuery().singleResult().getId();
        ProcessDefinition processDefinition = ((RepositoryServiceImpl) repositoryService).getDeployedProcessDefinition(id);
        assertThat(processDefinition.getDescription()).isEqualTo("This is really good process documentation!");
    }

    @Test
    public void testDeploySameFileTwiceForDifferentTenantId() {
        String bpmnResourceName = "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testGetBpmnXmlFileThroughService.bpmn20.xml";
        repositoryService.createDeployment().enableDuplicateFiltering().addClasspathResource(bpmnResourceName).name("twice").tenantId("Tenant_A").deploy();

        String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
        List<String> deploymentResources = repositoryService.getDeploymentResourceNames(deploymentId);

        // verify bpmn file name
        assertThat(deploymentResources)
                .containsExactly(bpmnResourceName);

        repositoryService.createDeployment().enableDuplicateFiltering().addClasspathResource(bpmnResourceName).name("twice").tenantId("Tenant_B").deploy();
        List<org.flowable.engine.repository.Deployment> deploymentList = repositoryService.createDeploymentQuery().list();
        // Now, we should have two deployment for same process file, one for each tenant
        assertThat(deploymentList).hasSize(2);

        for (org.flowable.engine.repository.Deployment deployment : deploymentList) {
            repositoryService.deleteDeployment(deployment.getId());
        }
    }

    @Test
    public void testV5Deployment() {
        String bpmnResourceName = "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testGetBpmnXmlFileThroughService.bpmn20.xml";
        assertThatThrownBy(() ->
                repositoryService.createDeployment()
                        .addClasspathResource(bpmnResourceName)
                        .deploymentProperty(DeploymentProperties.DEPLOY_AS_FLOWABLE5_PROCESS_DEFINITION, Boolean.TRUE)
                        .name("v5")
                        .deploy())
                .isExactlyInstanceOf(FlowableException.class)
                .as("Expected deployment exception because v5 compatibility handler is not enabled");
    }

}
