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
import org.flowable.dmn.api.DecisionTable;
import org.flowable.dmn.api.DecisionTableQuery;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.engine.impl.interceptor.Command;
import org.flowable.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.repository.ProcessDefinition;

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

  public List<DecisionTable> execute(CommandContext commandContext) {
    ProcessDefinition processDefinition = ProcessDefinitionUtil.getProcessDefinition(processDefinitionId);

    if (processDefinition == null) {
      throw new FlowableObjectNotFoundException("Cannot find process definition for id: " + processDefinitionId, ProcessDefinition.class);
    }

    BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(processDefinitionId);

    if (bpmnModel == null) {
      throw new FlowableObjectNotFoundException("Cannot find bpmn model for process definition id: " + processDefinitionId, BpmnModel.class);
    }

    if (commandContext.getProcessEngineConfiguration().isDmnEngineInitialized() == false) {
      throw new FlowableException("DMN Engine is not initialized");
    } else {
      if (commandContext.getProcessEngineConfiguration().getDmnEngineRepositoryService() == null) {
        throw new FlowableException("DMN repository service is not available");
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
