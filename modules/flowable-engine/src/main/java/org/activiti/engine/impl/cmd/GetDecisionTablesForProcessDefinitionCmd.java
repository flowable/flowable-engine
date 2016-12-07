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
package org.activiti.engine.impl.cmd;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FieldExtension;
import org.activiti.bpmn.model.ImplementationType;
import org.activiti.bpmn.model.ServiceTask;
import org.activiti.dmn.api.DecisionTable;
import org.activiti.dmn.api.DecisionTableQuery;
import org.activiti.dmn.api.DmnRepositoryService;
import org.activiti.engine.common.api.ActivitiException;
import org.activiti.engine.common.api.ActivitiObjectNotFoundException;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.util.ProcessDefinitionUtil;
import org.activiti.engine.repository.ProcessDefinition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Yvo Swillens
 */
public class GetDecisionTablesForProcessDefinitionCmd implements Command<List<DecisionTable>>, Serializable {

  private static final long serialVersionUID = 1L;
  protected String processDefinitionId;
  protected DmnRepositoryService dmnRepositoryService;

  public GetDecisionTablesForProcessDefinitionCmd(String processDefinitionId) {
    this.processDefinitionId = processDefinitionId;
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public List<DecisionTable> execute(CommandContext commandContext) {
    ProcessDefinition processDefinition = ProcessDefinitionUtil.getProcessDefinition(processDefinitionId);

    if (processDefinition == null) {
      throw new ActivitiObjectNotFoundException("Cannot find process definition for id: " + processDefinitionId, ProcessDefinition.class);
    }

    BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(processDefinitionId);

    if (bpmnModel == null) {
      throw new ActivitiObjectNotFoundException("Cannot find bpmn model for process definition id: " + processDefinitionId, BpmnModel.class);
    }

    if (commandContext.getProcessEngineConfiguration().isDmnEngineInitialized() == false) {
      throw new ActivitiException("DMN Engine is not initialized");
    } else {
      if (commandContext.getProcessEngineConfiguration().getDmnEngineRepositoryService() == null) {
        throw new ActivitiException("DMN repository service is not available");
      }
    }

    dmnRepositoryService = commandContext.getProcessEngineConfiguration().getDmnEngineRepositoryService();
    List<DecisionTable> decisionTables = getDecisionTablesFromModel(bpmnModel, processDefinition);

    return decisionTables;
  }

  protected List<DecisionTable> getDecisionTablesFromModel(BpmnModel bpmnModel, ProcessDefinition processDefinition) {
    Set<String> decisionTableKeys = new HashSet<>();
    List<DecisionTable> decisionTables = new ArrayList<>();
    List<ServiceTask> serviceTasks = bpmnModel.getMainProcess().findFlowElementsOfType(ServiceTask.class, true);

    for (ServiceTask serviceTask : serviceTasks) {
      if ("dmn".equals(serviceTask.getType())) {
        if (serviceTask.getFieldExtensions() != null && serviceTask.getFieldExtensions().size() > 0) {
          for (FieldExtension fieldExtension : serviceTask.getFieldExtensions()) {
            if ("decisionTableReferenceKey".equals(fieldExtension.getFieldName())) {
              String decisionTableReferenceKey = fieldExtension.getStringValue();
              if (!decisionTableKeys.contains(decisionTableReferenceKey)) {
                addDecisionTableToCollection(decisionTables, decisionTableReferenceKey, processDefinition);
                decisionTableKeys.add(decisionTableReferenceKey);
              }
              break;
            }
          }
        }
      }
    }

    return decisionTables;
  }

  protected void addDecisionTableToCollection(List<DecisionTable> decisionTables, String decisionTableKey, ProcessDefinition processDefinition) {
    DecisionTableQuery decisionTableQuery = dmnRepositoryService.createDecisionTableQuery();
    DecisionTable decisionTable = decisionTableQuery.decisionTableKey(decisionTableKey).parentDeploymentId(processDefinition.getDeploymentId()).singleResult();

    if (decisionTable != null) {
      decisionTables.add(decisionTable);
    }
  }
}
