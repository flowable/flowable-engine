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

package org.flowable.engine.test.api.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipInputStream;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.ParallelGateway;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.Model;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

/**
 * @author Frederik Heremans
 * @author Joram Barrez
 */
public class RepositoryServiceTest extends PluggableFlowableTestCase {

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testStartProcessInstanceById() {
        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();
        assertThat(processDefinitions)
                .extracting(ProcessDefinition::getKey)
                .containsExactly("oneTaskProcess");
        ProcessDefinition processDefinition = processDefinitions.get(0);
        assertThat(processDefinition.getId()).isNotNull();
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testFindProcessDefinitionById() {
        List<ProcessDefinition> definitions = repositoryService.createProcessDefinitionQuery().list();
        assertThat(definitions).hasSize(1);

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(definitions.get(0).getId()).singleResult();
        runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(processDefinition).isNotNull();
        assertThat(processDefinition.getKey()).isEqualTo("oneTaskProcess");
        assertThat(processDefinition.getName()).isEqualTo("The One Task Process");

        processDefinition = repositoryService.getProcessDefinition(definitions.get(0).getId());
        assertThat(processDefinition.getDescription()).isEqualTo("This is a process for testing purposes");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testDeleteDeploymentWithRunningInstances() {
        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();
        assertThat(processDefinitions).hasSize(1);
        ProcessDefinition processDefinition = processDefinitions.get(0);

        runtimeService.startProcessInstanceById(processDefinition.getId());

        // Try to delete the deployment
        assertThatThrownBy(() -> repositoryService.deleteDeployment(processDefinition.getDeploymentId()))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    public void testDeleteDeploymentNullDeploymentId() {
        assertThatThrownBy(() -> repositoryService.deleteDeployment(null))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("deploymentId is null");
    }

    @Test
    public void testDeleteDeploymentCascadeNullDeploymentId() {
        assertThatThrownBy(() -> repositoryService.deleteDeployment(null, true))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("deploymentId is null");
    }

    @Test
    public void testDeleteDeploymentNonExistentDeploymentId() {
        assertThatThrownBy(() -> repositoryService.deleteDeployment("foobar"))
                .isInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessageContaining("Could not find a deployment with id 'foobar'.");
    }

    @Test
    public void testDeleteDeploymentCascadeNonExistentDeploymentId() {
        assertThatThrownBy(() -> repositoryService.deleteDeployment("foobar", true))
                .isInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessageContaining("Could not find a deployment with id 'foobar'.");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testDeleteDeploymentCascadeWithRunningInstances() {
        List<ProcessDefinition> processDefinitions = repositoryService.createProcessDefinitionQuery().list();
        assertThat(processDefinitions).hasSize(1);
        ProcessDefinition processDefinition = processDefinitions.get(0);

        runtimeService.startProcessInstanceById(processDefinition.getId());

        // Try to delete the deployment, no exception should be thrown
        repositoryService.deleteDeployment(processDefinition.getDeploymentId(), true);
    }

    @Test
    public void testFindDeploymentResourceNamesNullDeploymentId() {
        assertThatThrownBy(() -> repositoryService.getDeploymentResourceNames(null))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("deploymentId is null");
    }

    @Test
    public void testDeploymentWithDelayedProcessDefinitionActivation() {

        Date startTime = new Date();
        processEngineConfiguration.getClock().setCurrentTime(startTime);
        Date inThreeDays = new Date(startTime.getTime() + (3 * 24 * 60 * 60 * 1000));

        // Deploy process, but activate after three days
        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml")
                .addClasspathResource("org/flowable/engine/test/api/twoTasksProcess.bpmn20.xml").activateProcessDefinitionsOn(inThreeDays).deploy();

        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(1);
        assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(2);
        assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isEqualTo(2);
        assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isZero();

        // Shouldn't be able to start a process instance
        assertThatThrownBy(() -> runtimeService.startProcessInstanceByKey("oneTaskProcess"))
                .isInstanceOf(FlowableException.class)
                .hasMessageContaining("suspended");

        // Move time four days forward, the timer will fire and the process
        // definitions will be active
        Date inFourDays = new Date(startTime.getTime() + (4 * 24 * 60 * 60 * 1000));
        processEngineConfiguration.getClock().setCurrentTime(inFourDays);
        waitForJobExecutorToProcessAllJobsAndExecutableTimerJobs(7000L, 50L);

        assertThat(repositoryService.createDeploymentQuery().count()).isEqualTo(1);
        assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(2);
        assertThat(repositoryService.createProcessDefinitionQuery().suspended().count()).isZero();
        assertThat(repositoryService.createProcessDefinitionQuery().active().count()).isEqualTo(2);

        // Should be able to start process instance
        runtimeService.startProcessInstanceByKey("oneTaskProcess");
        assertThat(runtimeService.createProcessInstanceQuery().count()).isEqualTo(1);

        // Cleanup
        repositoryService.deleteDeployment(deployment.getId(), true);
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetResourceAsStreamUnexistingResourceInExistingDeployment() {
        // Get hold of the deployment id
        org.flowable.engine.repository.Deployment deployment = repositoryService.createDeploymentQuery().singleResult();

        assertThatThrownBy(() -> repositoryService.getResourceAsStream(deployment.getId(), "org/flowable/engine/test/api/unexistingProcess.bpmn.xml"))
                .isInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessageContaining("no resource found with name");
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testGetResourceAsStreamUnexistingDeployment() {
        assertThatThrownBy(() -> repositoryService.getResourceAsStream("unexistingdeployment", "org/flowable/engine/test/api/unexistingProcess.bpmn.xml"))
                .isInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessageContaining("deployment does not exist");
    }

    @Test
    public void testGetResourceAsStreamNullArguments() {
        assertThatThrownBy(() -> repositoryService.getResourceAsStream(null, "resource"))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("deploymentId is null");

        assertThatThrownBy(() -> repositoryService.getResourceAsStream("deployment", null))
                .isInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessageContaining("resourceName is null");
    }

    @Test
    public void testNewModelPersistence() {
        Model model = repositoryService.newModel();
        assertThat(model).isNotNull();

        model.setName("Test model");
        model.setCategory("test");
        model.setMetaInfo("meta");
        repositoryService.saveModel(model);

        assertThat(model.getId()).isNotNull();
        model = repositoryService.getModel(model.getId());
        assertThat(model).isNotNull();
        assertThat(model.getName()).isEqualTo("Test model");
        assertThat(model.getCategory()).isEqualTo("test");
        assertThat(model.getMetaInfo()).isEqualTo("meta");
        assertThat(model.getCreateTime()).isNotNull();
        assertThat(model.getVersion()).isEqualTo(Integer.valueOf(1));

        repositoryService.deleteModel(model.getId());
    }

    @Test
    public void testNewModelWithSource() throws Exception {
        Model model = repositoryService.newModel();
        model.setName("Test model");
        byte[] testSource = "modelsource".getBytes(StandardCharsets.UTF_8);
        repositoryService.saveModel(model);

        assertThat(model.getId()).isNotNull();
        repositoryService.addModelEditorSource(model.getId(), testSource);

        model = repositoryService.getModel(model.getId());
        assertThat(model).isNotNull();
        assertThat(model.getName()).isEqualTo("Test model");

        byte[] editorSourceBytes = repositoryService.getModelEditorSource(model.getId());
        assertThat(new String(editorSourceBytes, StandardCharsets.UTF_8)).isEqualTo("modelsource");

        repositoryService.deleteModel(model.getId());
    }

    @Test
    public void testUpdateModelPersistence() throws Exception {
        Model model = repositoryService.newModel();
        assertThat(model).isNotNull();

        model.setName("Test model");
        model.setCategory("test");
        model.setMetaInfo("meta");
        repositoryService.saveModel(model);

        assertThat(model.getId()).isNotNull();
        model = repositoryService.getModel(model.getId());
        assertThat(model).isNotNull();

        model.setName("New name");
        model.setCategory("New category");
        model.setMetaInfo("test");
        model.setVersion(2);
        repositoryService.saveModel(model);

        assertThat(model.getId()).isNotNull();
        repositoryService.addModelEditorSource(model.getId(), "new".getBytes(StandardCharsets.UTF_8));
        repositoryService.addModelEditorSourceExtra(model.getId(), "new".getBytes(StandardCharsets.UTF_8));

        model = repositoryService.getModel(model.getId());

        assertThat(model.getName()).isEqualTo("New name");
        assertThat(model.getCategory()).isEqualTo("New category");
        assertThat(model.getMetaInfo()).isEqualTo("test");
        assertThat(model.getCreateTime()).isNotNull();
        assertThat(model.getVersion()).isEqualTo(Integer.valueOf(2));
        assertThat(new String(repositoryService.getModelEditorSource(model.getId()), StandardCharsets.UTF_8)).isEqualTo("new");
        assertThat(new String(repositoryService.getModelEditorSourceExtra(model.getId()), StandardCharsets.UTF_8)).isEqualTo("new");

        repositoryService.deleteModel(model.getId());
    }

    @Test
    @Deployment(resources = { "org/flowable/engine/test/api/oneTaskProcess.bpmn20.xml" })
    public void testProcessDefinitionEntitySerializable() {
        String procDefId = repositoryService.createProcessDefinitionQuery().singleResult().getId();
        ProcessDefinition processDefinition = repositoryService.getProcessDefinition(procDefId);

        assertThatCode(() -> {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new ObjectOutputStream(baos).writeObject(processDefinition);

            byte[] bytes = baos.toByteArray();
            assertThat(bytes).isNotEmpty();
        })
                .doesNotThrowAnyException();
    }

    @Test
    @Deployment
    public void testGetBpmnModel() {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().singleResult();

        // Some basic assertions
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        assertThat(bpmnModel).isNotNull();
        assertThat(bpmnModel.getProcesses()).hasSize(1);
        assertThat(bpmnModel.getLocationMap()).isNotEmpty();
        assertThat(bpmnModel.getFlowLocationMap()).isNotEmpty();

        // Test the flow
        org.flowable.bpmn.model.Process process = bpmnModel.getProcesses().get(0);
        List<StartEvent> startEvents = process.findFlowElementsOfType(StartEvent.class);
        assertThat(startEvents).hasSize(1);
        StartEvent startEvent = startEvents.get(0);
        assertThat(startEvent.getOutgoingFlows()).hasSize(1);
        assertThat(startEvent.getIncomingFlows()).isEmpty();

        String nextElementId = startEvent.getOutgoingFlows().get(0).getTargetRef();
        UserTask userTask = (UserTask) process.getFlowElement(nextElementId);
        assertThat(userTask.getName()).isEqualTo("First Task");

        assertThat(userTask.getOutgoingFlows()).hasSize(1);
        assertThat(userTask.getIncomingFlows()).hasSize(1);
        nextElementId = userTask.getOutgoingFlows().get(0).getTargetRef();
        ParallelGateway parallelGateway = (ParallelGateway) process.getFlowElement(nextElementId);
        assertThat(parallelGateway.getOutgoingFlows()).hasSize(2);

        nextElementId = parallelGateway.getOutgoingFlows().get(0).getTargetRef();
        assertThat(parallelGateway.getIncomingFlows()).hasSize(1);
        userTask = (UserTask) process.getFlowElement(nextElementId);
        assertThat(userTask.getOutgoingFlows()).hasSize(1);

        nextElementId = userTask.getOutgoingFlows().get(0).getTargetRef();
        parallelGateway = (ParallelGateway) process.getFlowElement(nextElementId);
        assertThat(parallelGateway.getOutgoingFlows()).hasSize(1);
        assertThat(parallelGateway.getIncomingFlows()).hasSize(2);

        nextElementId = parallelGateway.getOutgoingFlows().get(0).getTargetRef();
        EndEvent endEvent = (EndEvent) process.getFlowElement(nextElementId);
        assertThat(endEvent.getOutgoingFlows()).isEmpty();
        assertThat(endEvent.getIncomingFlows()).hasSize(1);
    }

    /**
     * This test was added due to issues with unzip of JDK 7, where the default is changed to UTF8 instead of the platform encoding (which is, in fact, good). However, some platforms do not create
     * UTF8-compatible ZIP files.
     * 
     * The tested zip file is created on OS X (non-UTF-8).
     * 
     * See https://blogs.oracle.com/xuemingshen/entry/non_utf_8_encoding_in
     */
    @Test
    public void testDeployZipFile() {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("org/flowable/engine/test/api/repository/test-processes.zip");
        assertThat(inputStream).isNotNull();
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        assertThat(zipInputStream).isNotNull();
        repositoryService.createDeployment().addZipInputStream(zipInputStream).deploy();

        assertThat(repositoryService.createProcessDefinitionQuery().count()).isEqualTo(6);

        // Delete
        for (org.flowable.engine.repository.Deployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId(), true);
        }
    }

}
