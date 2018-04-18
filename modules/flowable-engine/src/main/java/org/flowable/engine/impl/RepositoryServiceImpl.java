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

package org.flowable.engine.impl;

import java.io.InputStream;
import java.util.Date;
import java.util.List;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.service.CommonEngineServiceImpl;
import org.flowable.dmn.api.DmnDecisionTable;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.app.AppModel;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cmd.ActivateProcessDefinitionCmd;
import org.flowable.engine.impl.cmd.AddEditorSourceExtraForModelCmd;
import org.flowable.engine.impl.cmd.AddEditorSourceForModelCmd;
import org.flowable.engine.impl.cmd.AddIdentityLinkForProcessDefinitionCmd;
import org.flowable.engine.impl.cmd.ChangeDeploymentTenantIdCmd;
import org.flowable.engine.impl.cmd.CreateModelCmd;
import org.flowable.engine.impl.cmd.DeleteDeploymentCmd;
import org.flowable.engine.impl.cmd.DeleteIdentityLinkForProcessDefinitionCmd;
import org.flowable.engine.impl.cmd.DeleteModelCmd;
import org.flowable.engine.impl.cmd.DeployCmd;
import org.flowable.engine.impl.cmd.GetAppResourceModelCmd;
import org.flowable.engine.impl.cmd.GetAppResourceObjectCmd;
import org.flowable.engine.impl.cmd.GetBpmnModelCmd;
import org.flowable.engine.impl.cmd.GetDecisionTablesForProcessDefinitionCmd;
import org.flowable.engine.impl.cmd.GetDeploymentProcessDefinitionCmd;
import org.flowable.engine.impl.cmd.GetDeploymentProcessDiagramCmd;
import org.flowable.engine.impl.cmd.GetDeploymentProcessDiagramLayoutCmd;
import org.flowable.engine.impl.cmd.GetDeploymentProcessModelCmd;
import org.flowable.engine.impl.cmd.GetDeploymentResourceCmd;
import org.flowable.engine.impl.cmd.GetDeploymentResourceNamesCmd;
import org.flowable.engine.impl.cmd.GetFormDefinitionsForProcessDefinitionCmd;
import org.flowable.engine.impl.cmd.GetIdentityLinksForProcessDefinitionCmd;
import org.flowable.engine.impl.cmd.GetModelCmd;
import org.flowable.engine.impl.cmd.GetModelEditorSourceCmd;
import org.flowable.engine.impl.cmd.GetModelEditorSourceExtraCmd;
import org.flowable.engine.impl.cmd.IsFlowable5ProcessDefinitionCmd;
import org.flowable.engine.impl.cmd.IsProcessDefinitionSuspendedCmd;
import org.flowable.engine.impl.cmd.SaveModelCmd;
import org.flowable.engine.impl.cmd.SetDeploymentCategoryCmd;
import org.flowable.engine.impl.cmd.SetDeploymentKeyCmd;
import org.flowable.engine.impl.cmd.SetProcessDefinitionCategoryCmd;
import org.flowable.engine.impl.cmd.SuspendProcessDefinitionCmd;
import org.flowable.engine.impl.cmd.ValidateBpmnModelCmd;
import org.flowable.engine.impl.persistence.entity.ModelEntity;
import org.flowable.engine.impl.repository.DeploymentBuilderImpl;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.DeploymentBuilder;
import org.flowable.engine.repository.DeploymentQuery;
import org.flowable.engine.repository.DiagramLayout;
import org.flowable.engine.repository.Model;
import org.flowable.engine.repository.ModelQuery;
import org.flowable.engine.repository.NativeDeploymentQuery;
import org.flowable.engine.repository.NativeModelQuery;
import org.flowable.engine.repository.NativeProcessDefinitionQuery;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.form.api.FormDefinition;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.validation.ValidationError;

/**
 * @author Tom Baeyens
 * @author Falko Menge
 * @author Joram Barrez
 */
public class RepositoryServiceImpl extends CommonEngineServiceImpl<ProcessEngineConfigurationImpl> implements RepositoryService {

    @Override
    public DeploymentBuilder createDeployment() {
        return commandExecutor.execute(new Command<DeploymentBuilder>() {
            @Override
            public DeploymentBuilder execute(CommandContext commandContext) {
                return new DeploymentBuilderImpl(RepositoryServiceImpl.this);
            }
        });
    }

    public Deployment deploy(DeploymentBuilderImpl deploymentBuilder) {
        return commandExecutor.execute(new DeployCmd<Deployment>(deploymentBuilder));
    }

    @Override
    public void deleteDeployment(String deploymentId) {
        commandExecutor.execute(new DeleteDeploymentCmd(deploymentId, false));
    }

    public void deleteDeploymentCascade(String deploymentId) {
        commandExecutor.execute(new DeleteDeploymentCmd(deploymentId, true));
    }

    @Override
    public void deleteDeployment(String deploymentId, boolean cascade) {
        commandExecutor.execute(new DeleteDeploymentCmd(deploymentId, cascade));
    }

    @Override
    public void setDeploymentCategory(String deploymentId, String category) {
        commandExecutor.execute(new SetDeploymentCategoryCmd(deploymentId, category));
    }

    @Override
    public void setDeploymentKey(String deploymentId, String key) {
        commandExecutor.execute(new SetDeploymentKeyCmd(deploymentId, key));
    }

    @Override
    public ProcessDefinitionQuery createProcessDefinitionQuery() {
        return new ProcessDefinitionQueryImpl(commandExecutor);
    }

    @Override
    public NativeProcessDefinitionQuery createNativeProcessDefinitionQuery() {
        return new NativeProcessDefinitionQueryImpl(commandExecutor);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getDeploymentResourceNames(String deploymentId) {
        return commandExecutor.execute(new GetDeploymentResourceNamesCmd(deploymentId));
    }

    @Override
    public InputStream getResourceAsStream(String deploymentId, String resourceName) {
        return commandExecutor.execute(new GetDeploymentResourceCmd(deploymentId, resourceName));
    }

    @Override
    public void changeDeploymentTenantId(String deploymentId, String newTenantId) {
        commandExecutor.execute(new ChangeDeploymentTenantIdCmd(deploymentId, newTenantId));
    }

    @Override
    public DeploymentQuery createDeploymentQuery() {
        return new DeploymentQueryImpl(commandExecutor);
    }

    @Override
    public NativeDeploymentQuery createNativeDeploymentQuery() {
        return new NativeDeploymentQueryImpl(commandExecutor);
    }

    @Override
    public ProcessDefinition getProcessDefinition(String processDefinitionId) {
        return commandExecutor.execute(new GetDeploymentProcessDefinitionCmd(processDefinitionId));
    }

    @Override
    public Boolean isFlowable5ProcessDefinition(String processDefinitionId) {
        return commandExecutor.execute(new IsFlowable5ProcessDefinitionCmd(processDefinitionId));
    }

    @Override
    public BpmnModel getBpmnModel(String processDefinitionId) {
        return commandExecutor.execute(new GetBpmnModelCmd(processDefinitionId));
    }

    public ProcessDefinition getDeployedProcessDefinition(String processDefinitionId) {
        return commandExecutor.execute(new GetDeploymentProcessDefinitionCmd(processDefinitionId));
    }

    @Override
    public boolean isProcessDefinitionSuspended(String processDefinitionId) {
        return commandExecutor.execute(new IsProcessDefinitionSuspendedCmd(processDefinitionId));
    }

    @Override
    public void suspendProcessDefinitionById(String processDefinitionId) {
        commandExecutor.execute(new SuspendProcessDefinitionCmd(processDefinitionId, null, false, null, null));
    }

    @Override
    public void suspendProcessDefinitionById(String processDefinitionId, boolean suspendProcessInstances, Date suspensionDate) {
        commandExecutor.execute(new SuspendProcessDefinitionCmd(processDefinitionId, null, suspendProcessInstances, suspensionDate, null));
    }

    @Override
    public void suspendProcessDefinitionByKey(String processDefinitionKey) {
        commandExecutor.execute(new SuspendProcessDefinitionCmd(null, processDefinitionKey, false, null, null));
    }

    @Override
    public void suspendProcessDefinitionByKey(String processDefinitionKey, boolean suspendProcessInstances, Date suspensionDate) {
        commandExecutor.execute(new SuspendProcessDefinitionCmd(null, processDefinitionKey, suspendProcessInstances, suspensionDate, null));
    }

    @Override
    public void suspendProcessDefinitionByKey(String processDefinitionKey, String tenantId) {
        commandExecutor.execute(new SuspendProcessDefinitionCmd(null, processDefinitionKey, false, null, tenantId));
    }

    @Override
    public void suspendProcessDefinitionByKey(String processDefinitionKey, boolean suspendProcessInstances, Date suspensionDate, String tenantId) {
        commandExecutor.execute(new SuspendProcessDefinitionCmd(null, processDefinitionKey, suspendProcessInstances, suspensionDate, tenantId));
    }

    @Override
    public void activateProcessDefinitionById(String processDefinitionId) {
        commandExecutor.execute(new ActivateProcessDefinitionCmd(processDefinitionId, null, false, null, null));
    }

    @Override
    public void activateProcessDefinitionById(String processDefinitionId, boolean activateProcessInstances, Date activationDate) {
        commandExecutor.execute(new ActivateProcessDefinitionCmd(processDefinitionId, null, activateProcessInstances, activationDate, null));
    }

    @Override
    public void activateProcessDefinitionByKey(String processDefinitionKey) {
        commandExecutor.execute(new ActivateProcessDefinitionCmd(null, processDefinitionKey, false, null, null));
    }

    @Override
    public void activateProcessDefinitionByKey(String processDefinitionKey, boolean activateProcessInstances, Date activationDate) {
        commandExecutor.execute(new ActivateProcessDefinitionCmd(null, processDefinitionKey, activateProcessInstances, activationDate, null));
    }

    @Override
    public void activateProcessDefinitionByKey(String processDefinitionKey, String tenantId) {
        commandExecutor.execute(new ActivateProcessDefinitionCmd(null, processDefinitionKey, false, null, tenantId));
    }

    @Override
    public void activateProcessDefinitionByKey(String processDefinitionKey, boolean activateProcessInstances, Date activationDate, String tenantId) {
        commandExecutor.execute(new ActivateProcessDefinitionCmd(null, processDefinitionKey, activateProcessInstances, activationDate, tenantId));
    }

    @Override
    public void setProcessDefinitionCategory(String processDefinitionId, String category) {
        commandExecutor.execute(new SetProcessDefinitionCategoryCmd(processDefinitionId, category));
    }

    @Override
    public InputStream getProcessModel(String processDefinitionId) {
        return commandExecutor.execute(new GetDeploymentProcessModelCmd(processDefinitionId));
    }

    @Override
    public InputStream getProcessDiagram(String processDefinitionId) {
        return commandExecutor.execute(new GetDeploymentProcessDiagramCmd(processDefinitionId));
    }

    @Override
    public DiagramLayout getProcessDiagramLayout(String processDefinitionId) {
        return commandExecutor.execute(new GetDeploymentProcessDiagramLayoutCmd(processDefinitionId));
    }

    @Override
    public Object getAppResourceObject(String deploymentId) {
        return commandExecutor.execute(new GetAppResourceObjectCmd(deploymentId));
    }

    @Override
    public AppModel getAppResourceModel(String deploymentId) {
        return commandExecutor.execute(new GetAppResourceModelCmd(deploymentId));
    }

    @Override
    public Model newModel() {
        return commandExecutor.execute(new CreateModelCmd());
    }

    @Override
    public void saveModel(Model model) {
        commandExecutor.execute(new SaveModelCmd((ModelEntity) model));
    }

    @Override
    public void deleteModel(String modelId) {
        commandExecutor.execute(new DeleteModelCmd(modelId));
    }

    @Override
    public void addModelEditorSource(String modelId, byte[] bytes) {
        commandExecutor.execute(new AddEditorSourceForModelCmd(modelId, bytes));
    }

    @Override
    public void addModelEditorSourceExtra(String modelId, byte[] bytes) {
        commandExecutor.execute(new AddEditorSourceExtraForModelCmd(modelId, bytes));
    }

    @Override
    public ModelQuery createModelQuery() {
        return new ModelQueryImpl(commandExecutor);
    }

    @Override
    public NativeModelQuery createNativeModelQuery() {
        return new NativeModelQueryImpl(commandExecutor);
    }

    @Override
    public Model getModel(String modelId) {
        return commandExecutor.execute(new GetModelCmd(modelId));
    }

    @Override
    public byte[] getModelEditorSource(String modelId) {
        return commandExecutor.execute(new GetModelEditorSourceCmd(modelId));
    }

    @Override
    public byte[] getModelEditorSourceExtra(String modelId) {
        return commandExecutor.execute(new GetModelEditorSourceExtraCmd(modelId));
    }

    @Override
    public void addCandidateStarterUser(String processDefinitionId, String userId) {
        commandExecutor.execute(new AddIdentityLinkForProcessDefinitionCmd(processDefinitionId, userId, null));
    }

    @Override
    public void addCandidateStarterGroup(String processDefinitionId, String groupId) {
        commandExecutor.execute(new AddIdentityLinkForProcessDefinitionCmd(processDefinitionId, null, groupId));
    }

    @Override
    public void deleteCandidateStarterGroup(String processDefinitionId, String groupId) {
        commandExecutor.execute(new DeleteIdentityLinkForProcessDefinitionCmd(processDefinitionId, null, groupId));
    }

    @Override
    public void deleteCandidateStarterUser(String processDefinitionId, String userId) {
        commandExecutor.execute(new DeleteIdentityLinkForProcessDefinitionCmd(processDefinitionId, userId, null));
    }

    @Override
    public List<IdentityLink> getIdentityLinksForProcessDefinition(String processDefinitionId) {
        return commandExecutor.execute(new GetIdentityLinksForProcessDefinitionCmd(processDefinitionId));
    }

    @Override
    public List<ValidationError> validateProcess(BpmnModel bpmnModel) {
        return commandExecutor.execute(new ValidateBpmnModelCmd(bpmnModel));
    }

    @Override
    public List<DmnDecisionTable> getDecisionTablesForProcessDefinition(String processDefinitionId) {
        return commandExecutor.execute(new GetDecisionTablesForProcessDefinitionCmd(processDefinitionId));
    }

    @Override
    public List<FormDefinition> getFormDefinitionsForProcessDefinition(String processDefinitionId) {
        return commandExecutor.execute(new GetFormDefinitionsForProcessDefinitionCmd(processDefinitionId));
    }
}
