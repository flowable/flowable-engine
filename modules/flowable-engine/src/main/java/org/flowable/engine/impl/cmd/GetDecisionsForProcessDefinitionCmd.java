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
package org.flowable.engine.impl.cmd;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FieldExtension;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.dmn.api.DmnDecision;
import org.flowable.dmn.api.DmnDecisionQuery;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;

/**
 * @author Yvo Swillens
 */
public class GetDecisionsForProcessDefinitionCmd implements Command<List<DmnDecision>>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String processDefinitionId;
    protected DmnRepositoryService dmnRepositoryService;

    public GetDecisionsForProcessDefinitionCmd(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    @Override
    public List<DmnDecision> execute(CommandContext commandContext) {
        ProcessDefinition processDefinition = ProcessDefinitionUtil.getProcessDefinition(processDefinitionId);

        if (processDefinition == null) {
            throw new FlowableObjectNotFoundException("Cannot find process definition for id: " + processDefinitionId, ProcessDefinition.class);
        }

        BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(processDefinitionId);

        if (bpmnModel == null) {
            throw new FlowableObjectNotFoundException("Cannot find bpmn model for process definition id: " + processDefinitionId, BpmnModel.class);
        }

        if (CommandContextUtil.getDmnRepositoryService() == null) {
            throw new FlowableException("DMN repository service is not available");
        }

        dmnRepositoryService = CommandContextUtil.getDmnRepositoryService();
        List<DmnDecision> decisions = getDecisionsFromModel(bpmnModel, processDefinition);

        return decisions;
    }

    protected List<DmnDecision> getDecisionsFromModel(BpmnModel bpmnModel, ProcessDefinition processDefinition) {
        Set<String> decisionKeys = new HashSet<>();
        List<DmnDecision> decisions = new ArrayList<>();
        List<ServiceTask> serviceTasks = bpmnModel.getMainProcess().findFlowElementsOfType(ServiceTask.class, true);

        for (ServiceTask serviceTask : serviceTasks) {
            if ("dmn".equals(serviceTask.getType())) {
                if (serviceTask.getFieldExtensions() != null && serviceTask.getFieldExtensions().size() > 0) {
                    for (FieldExtension fieldExtension : serviceTask.getFieldExtensions()) {
                        if ("decisionTableReferenceKey".equals(fieldExtension.getFieldName())) {
                            String decisionReferenceKey = fieldExtension.getStringValue();
                            if (!decisionKeys.contains(decisionReferenceKey)) {
                                addDecisionToCollection(decisions, decisionReferenceKey, processDefinition);
                                decisionKeys.add(decisionReferenceKey);
                            }
                            break;
                        }
                    }
                }
            }
        }

        return decisions;
    }

    protected void addDecisionToCollection(List<DmnDecision> decisions, String decisionKey, ProcessDefinition processDefinition) {
        DmnDecisionQuery definitionQuery = dmnRepositoryService.createDecisionQuery().decisionKey(decisionKey);
        Deployment deployment = CommandContextUtil.getDeploymentEntityManager().findById(processDefinition.getDeploymentId());
        if (deployment.getParentDeploymentId() != null) {
            List<DmnDeployment> dmnDeployments = dmnRepositoryService.createDeploymentQuery().parentDeploymentId(deployment.getParentDeploymentId()).list();
            
            if (dmnDeployments != null && dmnDeployments.size() > 0) {
                definitionQuery.deploymentId(dmnDeployments.get(0).getId());
            } else {
                definitionQuery.latestVersion();
            }
            
        } else {
            definitionQuery.latestVersion();
        }
        
        DmnDecision decision = definitionQuery.singleResult();
        
        if (decision != null) {
            decisions.add(decision);
        }
    }
}
