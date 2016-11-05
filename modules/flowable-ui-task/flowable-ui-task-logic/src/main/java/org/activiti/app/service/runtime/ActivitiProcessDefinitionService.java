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
package org.activiti.app.service.runtime;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.activiti.app.model.common.ResultListDataRepresentation;
import org.activiti.app.model.runtime.ProcessDefinitionRepresentation;
import org.activiti.app.service.exception.BadRequestException;
import org.activiti.app.service.exception.InternalServerErrorException;
import org.activiti.app.service.exception.NotFoundException;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.FlowElement;
import org.activiti.bpmn.model.Process;
import org.activiti.bpmn.model.StartEvent;
import org.activiti.editor.language.json.converter.util.CollectionUtils;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.form.api.FormRepositoryService;
import org.activiti.form.model.FormField;
import org.activiti.form.model.FormModel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Tijs Rademakers
 */
@Service
public class ActivitiProcessDefinitionService {

  private static final Logger logger = LoggerFactory.getLogger(ActivitiProcessDefinitionService.class);

  @Autowired
  protected RepositoryService repositoryService;

  @Autowired
  protected FormRepositoryService formRepositoryService;

  @Autowired
  protected ObjectMapper objectMapper;

  public FormModel getProcessDefinitionStartForm(HttpServletRequest request) {

    String[] requestInfoArray = parseRequest(request);
    String processDefinitionId = getProcessDefinitionId(requestInfoArray, requestInfoArray.length - 2);

    ProcessDefinition processDefinition = repositoryService.getProcessDefinition(processDefinitionId);

    try {
      return getStartForm(processDefinition);

    } catch (ActivitiObjectNotFoundException aonfe) {
      // Process definition does not exist
      throw new NotFoundException("No process definition found with the given id: " + processDefinitionId);
    }
  }
  
  public ResultListDataRepresentation getProcessDefinitions(Boolean latest, String deploymentKey) {

    ProcessDefinitionQuery definitionQuery = repositoryService.createProcessDefinitionQuery();

    if (deploymentKey != null) {
      Deployment deployment = repositoryService.createDeploymentQuery().deploymentKey(deploymentKey).latest().singleResult();
      
      if (deployment != null) {
        definitionQuery.deploymentId(deployment.getId());
      } else {
        return new ResultListDataRepresentation(new ArrayList<ProcessDefinitionRepresentation>());
      }

    } else {

      if (latest != null && latest) {
        definitionQuery.latestVersion();
      }
    }

    List<ProcessDefinition> definitions = definitionQuery.list();
    ResultListDataRepresentation result = new ResultListDataRepresentation(convertDefinitionList(definitions));
    return result;
  }

  protected FormModel getStartForm(ProcessDefinition processDefinition) {
    FormModel formModel = null;
    BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
    Process process = bpmnModel.getProcessById(processDefinition.getKey());
    FlowElement startElement = process.getInitialFlowElement();
    if (startElement instanceof StartEvent) {
      StartEvent startEvent = (StartEvent) startElement;
      if (StringUtils.isNotEmpty(startEvent.getFormKey())) {
        formModel = formRepositoryService.getFormModelByKeyAndParentDeploymentId(startEvent.getFormKey(), 
            processDefinition.getDeploymentId(), processDefinition.getTenantId());
      }
    }

    if (formModel == null) {
      // Definition found, but no form attached
      throw new NotFoundException("Process definition does not have a form defined: " + processDefinition.getId());
    }

    return formModel;
  }

  protected ProcessDefinition getProcessDefinitionFromRequest(String[] requestInfoArray, boolean isTableRequest) {
    int paramPosition = requestInfoArray.length - 3;
    if (isTableRequest) {
      paramPosition--;
    }
    String processDefinitionId = getProcessDefinitionId(requestInfoArray, paramPosition);

    ProcessDefinition processDefinition = repositoryService.getProcessDefinition(processDefinitionId);

    return processDefinition;
  }

  protected FormField getFormFieldFromRequest(String[] requestInfoArray, ProcessDefinition processDefinition, boolean isTableRequest) {
    FormModel form = getStartForm(processDefinition);
    int paramPosition = requestInfoArray.length - 1;
    if (isTableRequest) {
      paramPosition--;
    }
    String fieldVariable = requestInfoArray[paramPosition];

    List<? extends FormField> allFields = form.listAllFields();
    FormField selectedField = null;
    if (CollectionUtils.isNotEmpty(allFields)) {
      for (FormField formFieldRepresentation : allFields) {
        if (formFieldRepresentation.getId().equalsIgnoreCase(fieldVariable)) {
          selectedField = formFieldRepresentation;
        }
      }
    }

    if (selectedField == null) {
      throw new NotFoundException("Field could not be found in start form definition " + fieldVariable);
    }

    return selectedField;
  }
  
  protected List<ProcessDefinitionRepresentation> convertDefinitionList(List<ProcessDefinition> definitions) {
    Map<String, Boolean> startFormMap = new HashMap<String, Boolean>();
    List<ProcessDefinitionRepresentation> result = new ArrayList<ProcessDefinitionRepresentation>();
    if (CollectionUtils.isNotEmpty(definitions)) {
      for (ProcessDefinition processDefinition : definitions) {
        if (startFormMap.containsKey(processDefinition.getId()) == false) {
          BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
          List<StartEvent> startEvents = bpmnModel.getMainProcess().findFlowElementsOfType(StartEvent.class, false);
          boolean hasStartForm = false;
          for (StartEvent startEvent : startEvents) {
            if (StringUtils.isNotEmpty(startEvent.getFormKey())) {
              FormModel formModel = formRepositoryService.getFormModelByKey(startEvent.getFormKey());
              if (formModel != null) {
                hasStartForm = true;
                break;
              }
            }
          }

          startFormMap.put(processDefinition.getId(), hasStartForm);
        }
        
        ProcessDefinitionRepresentation rep = new ProcessDefinitionRepresentation(processDefinition);
        rep.setHasStartForm(startFormMap.get(processDefinition.getId()));
        result.add(rep);
      }
    }
    return result;
  }

  protected String[] parseRequest(HttpServletRequest request) {
    String requestURI = request.getRequestURI();
    String[] requestInfoArray = requestURI.split("/");
    if (requestInfoArray.length < 2) {
      throw new BadRequestException("Start form request is not valid " + requestURI);
    }
    return requestInfoArray;
  }

  protected String getProcessDefinitionId(String[] requestInfoArray, int position) {
    String processDefinitionVariable = requestInfoArray[position];
    String processDefinitionId = null;
    try {
      processDefinitionId = URLDecoder.decode(processDefinitionVariable, "UTF-8");
    } catch (Exception e) {
      logger.error("Error decoding process definition " + processDefinitionVariable, e);
      throw new InternalServerErrorException("Error decoding process definition " + processDefinitionVariable);
    }
    return processDefinitionId;
  }
}
