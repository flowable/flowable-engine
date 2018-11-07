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
package org.flowable.ui.task.service.runtime;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.app.api.AppRepositoryService;
import org.flowable.app.api.repository.AppDefinition;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.editor.language.json.converter.util.CollectionUtils;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.form.api.FormInfo;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.ui.common.model.ResultListDataRepresentation;
import org.flowable.ui.common.security.SecurityUtils;
import org.flowable.ui.common.service.exception.NotFoundException;
import org.flowable.ui.task.model.runtime.ProcessDefinitionRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Tijs Rademakers
 */
@Service
@Transactional
public class FlowableProcessDefinitionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableProcessDefinitionService.class);

    @Autowired
    protected RepositoryService repositoryService;
    
    @Autowired
    protected AppRepositoryService appRepositoryService;

    @Autowired
    protected FormRepositoryService formRepositoryService;

    @Autowired
    protected PermissionService permissionService;

    @Autowired
    protected ObjectMapper objectMapper;

    public FormInfo getProcessDefinitionStartForm(String processDefinitionId) {

        ProcessDefinition processDefinition = repositoryService.getProcessDefinition(processDefinitionId);

        try {
            return getStartForm(processDefinition);

        } catch (FlowableObjectNotFoundException aonfe) {
            // Process definition does not exist
            throw new NotFoundException("No process definition found with the given id: " + processDefinitionId);
        }
    }

    public ResultListDataRepresentation getProcessDefinitions(Boolean latest, String appDefinitionKey) {

        ProcessDefinitionQuery definitionQuery = repositoryService.createProcessDefinitionQuery();

        if (appDefinitionKey != null) {
            AppDefinition appDefinition = appRepositoryService.createAppDefinitionQuery().appDefinitionKey(appDefinitionKey).latestVersion().singleResult();
            Deployment deployment = repositoryService.createDeploymentQuery().parentDeploymentId(appDefinition.getDeploymentId()).singleResult();

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

        List<ProcessDefinition> startableDefinitions = new ArrayList<>();
        for (ProcessDefinition definition : definitions) {
            if (SecurityUtils.getCurrentUserObject() == null || permissionService.canStartProcess(SecurityUtils.getCurrentUserObject(), definition)) {
                startableDefinitions.add(definition);
            }
        }

        ResultListDataRepresentation result = new ResultListDataRepresentation(convertDefinitionList(startableDefinitions));
        return result;
    }

    protected FormInfo getStartForm(ProcessDefinition processDefinition) {
        FormInfo formInfo = null;
        BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
        Process process = bpmnModel.getProcessById(processDefinition.getKey());
        FlowElement startElement = process.getInitialFlowElement();
        if (startElement instanceof StartEvent) {
            StartEvent startEvent = (StartEvent) startElement;
            if (StringUtils.isNotEmpty(startEvent.getFormKey())) {
                Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(processDefinition.getDeploymentId()).singleResult();
                formInfo = formRepositoryService.getFormModelByKeyAndParentDeploymentId(startEvent.getFormKey(),
                                deployment.getParentDeploymentId(), processDefinition.getTenantId());
            }
        }

        if (formInfo == null) {
            // Definition found, but no form attached
            throw new NotFoundException("Process definition does not have a form defined: " + processDefinition.getId());
        }

        return formInfo;
    }

    protected List<ProcessDefinitionRepresentation> convertDefinitionList(List<ProcessDefinition> definitions) {
        List<ProcessDefinitionRepresentation> result = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(definitions)) {
            for (ProcessDefinition processDefinition : definitions) {
                ProcessDefinitionRepresentation rep = new ProcessDefinitionRepresentation(processDefinition);
                result.add(rep);
            }
        }
        return result;
    }

}
