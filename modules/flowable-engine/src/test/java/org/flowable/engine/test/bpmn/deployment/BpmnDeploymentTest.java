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
import org.flowable.validation.validator.Problems;

/**
 * @author Joram Barrez
 * @author Erik Winlof
 */
public class BpmnDeploymentTest extends PluggableFlowableTestCase {

    @Deployment
    public void testGetBpmnXmlFileThroughService() {
        String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
        List<String> deploymentResources = repositoryService.getDeploymentResourceNames(deploymentId);

        // verify bpmn file name
        assertEquals(1, deploymentResources.size());
        String bpmnResourceName = "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testGetBpmnXmlFileThroughService.bpmn20.xml";
        assertEquals(bpmnResourceName, deploymentResources.get(0));

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        assertEquals(bpmnResourceName, processDefinition.getResourceName());
        assertNull(processDefinition.getDiagramResourceName());
        assertFalse(processDefinition.hasStartFormKey());

        ProcessDefinition readOnlyProcessDefinition = ((RepositoryServiceImpl) repositoryService).getDeployedProcessDefinition(processDefinition.getId());
        assertNull(readOnlyProcessDefinition.getDiagramResourceName());

        // verify content
        InputStream deploymentInputStream = repositoryService.getResourceAsStream(deploymentId, bpmnResourceName);
        String contentFromDeployment = readInputStreamToString(deploymentInputStream);
        assertTrue(contentFromDeployment.length() > 0);
        assertTrue(contentFromDeployment.contains("process id=\"emptyProcess\""));

        InputStream fileInputStream = ReflectUtil.getResourceAsStream(
                "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testGetBpmnXmlFileThroughService.bpmn20.xml");
        String contentFromFile = readInputStreamToString(fileInputStream);
        assertEquals(contentFromFile, contentFromDeployment);
    }

    private String readInputStreamToString(InputStream inputStream) {
        byte[] bytes = IoUtil.readInputStream(inputStream, "input stream");
        return new String(bytes);
    }

    public void testViolateBPMNIdMaximumLength() {
        try {
            repositoryService.createDeployment()
                    .addClasspathResource("org/flowable/engine/test/bpmn/deployment/definitionWithLongTargetNamespace.bpmn20.xml")
                    .deploy();
            fail();
        } catch (FlowableException e) {
            assertTextPresent(Problems.BPMN_MODEL_TARGET_NAMESPACE_TOO_LONG, e.getMessage());
        }

        // Verify that nothing is deployed
        assertEquals(0, repositoryService.createDeploymentQuery().count());
    }

    public void testViolateProcessDefinitionIdMaximumLength() {
        try {
            repositoryService.createDeployment()
                    .addClasspathResource("org/flowable/engine/test/bpmn/deployment/processWithLongId.bpmn20.xml")
                    .deploy();
            fail();
        } catch (FlowableException e) {
            assertTextPresent(Problems.PROCESS_DEFINITION_ID_TOO_LONG, e.getMessage());
        }

        // Verify that nothing is deployed
        assertEquals(0, repositoryService.createDeploymentQuery().count());
    }

    public void testViolateProcessDefinitionNameAndDescriptionMaximumLength() {
        try {
            repositoryService.createDeployment()
                    .addClasspathResource("org/flowable/engine/test/bpmn/deployment/processWithLongNameAndDescription.bpmn20.xml")
                    .deploy();
            fail();
        } catch (FlowableException e) {
            assertTextPresent(Problems.PROCESS_DEFINITION_NAME_TOO_LONG, e.getMessage());
            assertTextPresent(Problems.PROCESS_DEFINITION_DOCUMENTATION_TOO_LONG, e.getMessage());
        }

        // Verify that nothing is deployed
        assertEquals(0, repositoryService.createDeploymentQuery().count());
    }

    public void testViolateDefinitionTargetNamespaceMaximumLength() {
        try {
            repositoryService.createDeployment()
                    .addClasspathResource("org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.definitionWithLongTargetNamespace.bpmn20.xml")
                    .deploy();
            fail();
        } catch (FlowableException e) {
            assertTextPresent(Problems.BPMN_MODEL_TARGET_NAMESPACE_TOO_LONG, e.getMessage());
        }

        // Verify that nothing is deployed
        assertEquals(0, repositoryService.createDeploymentQuery().count());
    }

    public void testDeploySameFileTwice() {
        String bpmnResourceName = "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testGetBpmnXmlFileThroughService.bpmn20.xml";
        repositoryService.createDeployment().enableDuplicateFiltering().addClasspathResource(bpmnResourceName).name("twice").deploy();

        String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
        List<String> deploymentResources = repositoryService.getDeploymentResourceNames(deploymentId);

        // verify bpmn file name
        assertEquals(1, deploymentResources.size());
        assertEquals(bpmnResourceName, deploymentResources.get(0));

        repositoryService.createDeployment().enableDuplicateFiltering().addClasspathResource(bpmnResourceName).name("twice").deploy();
        List<org.flowable.engine.repository.Deployment> deploymentList = repositoryService.createDeploymentQuery().list();
        assertEquals(1, deploymentList.size());

        repositoryService.deleteDeployment(deploymentId);
    }
    
    public void testDeploySameFileTwiceAfterInitialDeployment() {
        String bpmnResourceName = "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testProcessDiagramResource.bpmn20.xml";
        repositoryService.createDeployment().enableDuplicateFiltering().addClasspathResource(bpmnResourceName).name("twice").deploy();
        
        bpmnResourceName = "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testGetBpmnXmlFileThroughService.bpmn20.xml";
        repositoryService.createDeployment().enableDuplicateFiltering().addClasspathResource(bpmnResourceName).name("twice").deploy();

        List<org.flowable.engine.repository.Deployment> deploymentList = repositoryService.createDeploymentQuery().orderByDeploymenTime().desc().list();
        assertEquals(2, deploymentList.size());
        List<String> deploymentResources = repositoryService.getDeploymentResourceNames(deploymentList.get(0).getId());

        // verify bpmn file name
        assertEquals(1, deploymentResources.size());
        assertEquals(bpmnResourceName, deploymentResources.get(0));

        repositoryService.createDeployment().enableDuplicateFiltering().addClasspathResource(bpmnResourceName).name("twice").deploy();
        deploymentList = repositoryService.createDeploymentQuery().list();
        assertEquals(2, deploymentList.size());

        for (org.flowable.engine.repository.Deployment deployment : deploymentList) {
            repositoryService.deleteDeployment(deployment.getId());
        }
    }

    public void testDeployTwoProcessesWithDuplicateIdAtTheSameTime() {
        try {
            String bpmnResourceName = "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testGetBpmnXmlFileThroughService.bpmn20.xml";
            String bpmnResourceName2 = "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testGetBpmnXmlFileThroughService2.bpmn20.xml";
            repositoryService.createDeployment().enableDuplicateFiltering().addClasspathResource(bpmnResourceName).addClasspathResource(bpmnResourceName2).name("duplicateAtTheSameTime").deploy();
            fail();
        } catch (Exception e) {
            // Verify that nothing is deployed
            assertEquals(0, repositoryService.createDeploymentQuery().count());
        }
    }

    public void testDeployDifferentFiles() {
        String bpmnResourceName = "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testGetBpmnXmlFileThroughService.bpmn20.xml";
        repositoryService.createDeployment().enableDuplicateFiltering().addClasspathResource(bpmnResourceName).name("twice").deploy();

        String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
        List<String> deploymentResources = repositoryService.getDeploymentResourceNames(deploymentId);

        // verify bpmn file name
        assertEquals(1, deploymentResources.size());
        assertEquals(bpmnResourceName, deploymentResources.get(0));

        bpmnResourceName = "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testProcessDiagramResource.bpmn20.xml";
        repositoryService.createDeployment().enableDuplicateFiltering().addClasspathResource(bpmnResourceName).name("twice").deploy();
        List<org.flowable.engine.repository.Deployment> deploymentList = repositoryService.createDeploymentQuery().list();
        assertEquals(2, deploymentList.size());

        for (org.flowable.engine.repository.Deployment deployment : deploymentList) {
            repositoryService.deleteDeployment(deployment.getId());
        }
    }

    @Deployment
    public void testStartFormKey() {
        String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
        List<String> deploymentResources = repositoryService.getDeploymentResourceNames(deploymentId);

        // verify bpmn file name
        assertEquals(1, deploymentResources.size());
        String bpmnResourceName = "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testStartFormKey.bpmn20.xml";
        assertEquals(bpmnResourceName, deploymentResources.get(0));

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();
        assertEquals(bpmnResourceName, processDefinition.getResourceName());
        assertNull(processDefinition.getDiagramResourceName());
        assertTrue(processDefinition.hasStartFormKey());
    }

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

            assertNotNull(processDefinition);
            BpmnModel processModel = repositoryService.getBpmnModel(processDefinition.getId());
            assertEquals(14, processModel.getMainProcess().getFlowElements().size());
            assertEquals(7, processModel.getMainProcess().findFlowElementsOfType(SequenceFlow.class).size());

            // Check that no diagram has been created
            List<String> resourceNames = repositoryService.getDeploymentResourceNames(processDefinition.getDeploymentId());
            assertEquals(1, resourceNames.size());

            repositoryService.deleteDeployment(repositoryService.createDeploymentQuery().singleResult().getId(), true);
        } finally {
            processEngineConfiguration.setCreateDiagramOnDeploy(true);
        }
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testProcessDiagramResource.bpmn20.xml",
            "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testProcessDiagramResource.jpg" })
    public void testProcessDiagramResource() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

        assertEquals("org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testProcessDiagramResource.bpmn20.xml", processDefinition.getResourceName());
        BpmnModel processModel = repositoryService.getBpmnModel(processDefinition.getId());
        List<StartEvent> startEvents = processModel.getMainProcess().findFlowElementsOfType(StartEvent.class);
        assertEquals(1, startEvents.size());
        assertEquals("someFormKey", startEvents.get(0).getFormKey());

        String diagramResourceName = processDefinition.getDiagramResourceName();
        assertEquals("org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testProcessDiagramResource.jpg", diagramResourceName);

        InputStream diagramStream = repositoryService.getResourceAsStream(deploymentIdFromDeploymentAnnotation,
                "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testProcessDiagramResource.jpg");
        byte[] diagramBytes = IoUtil.readInputStream(diagramStream, "diagram stream");
        assertEquals(33343, diagramBytes.length);
    }

    @Deployment(resources = { "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testMultipleDiagramResourcesProvided.bpmn20.xml",
            "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testMultipleDiagramResourcesProvided.a.jpg",
            "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testMultipleDiagramResourcesProvided.b.jpg",
            "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testMultipleDiagramResourcesProvided.c.jpg" })
    public void testMultipleDiagramResourcesProvided() {
        ProcessDefinition processA = repositoryService.createProcessDefinitionQuery().processDefinitionKey("a").singleResult();
        ProcessDefinition processB = repositoryService.createProcessDefinitionQuery().processDefinitionKey("b").singleResult();
        ProcessDefinition processC = repositoryService.createProcessDefinitionQuery().processDefinitionKey("c").singleResult();

        assertEquals("org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testMultipleDiagramResourcesProvided.a.jpg", processA.getDiagramResourceName());
        assertEquals("org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testMultipleDiagramResourcesProvided.b.jpg", processB.getDiagramResourceName());
        assertEquals("org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testMultipleDiagramResourcesProvided.c.jpg", processC.getDiagramResourceName());
    }

    @Deployment
    public void testProcessDefinitionDescription() {
        String id = repositoryService.createProcessDefinitionQuery().singleResult().getId();
        ProcessDefinition processDefinition = ((RepositoryServiceImpl) repositoryService).getDeployedProcessDefinition(id);
        assertEquals("This is really good process documentation!", processDefinition.getDescription());
    }

    public void testDeploySameFileTwiceForDifferentTenantId() {
        String bpmnResourceName = "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testGetBpmnXmlFileThroughService.bpmn20.xml";
        repositoryService.createDeployment().enableDuplicateFiltering().addClasspathResource(bpmnResourceName).name("twice").tenantId("Tenant_A").deploy();

        String deploymentId = repositoryService.createDeploymentQuery().singleResult().getId();
        List<String> deploymentResources = repositoryService.getDeploymentResourceNames(deploymentId);

        // verify bpmn file name
        assertEquals(1, deploymentResources.size());
        assertEquals(bpmnResourceName, deploymentResources.get(0));

        repositoryService.createDeployment().enableDuplicateFiltering().addClasspathResource(bpmnResourceName).name("twice").tenantId("Tenant_B").deploy();
        List<org.flowable.engine.repository.Deployment> deploymentList = repositoryService.createDeploymentQuery().list();
        // Now, we should have two deployment for same process file, one for
        // each tenant
        assertEquals(2, deploymentList.size());

        for (org.flowable.engine.repository.Deployment deployment : deploymentList) {
            repositoryService.deleteDeployment(deployment.getId());
        }
    }

    public void testV5Deployment() {
        String bpmnResourceName = "org/flowable/engine/test/bpmn/deployment/BpmnDeploymentTest.testGetBpmnXmlFileThroughService.bpmn20.xml";
        try {
            repositoryService.createDeployment()
                    .addClasspathResource(bpmnResourceName)
                    .deploymentProperty(DeploymentProperties.DEPLOY_AS_FLOWABLE5_PROCESS_DEFINITION, Boolean.TRUE)
                    .name("v5")
                    .deploy();

            fail("Expected deployment exception because v5 compatibility handler is not enabled");

        } catch (FlowableException e) {
            // expected
        }
    }

}
