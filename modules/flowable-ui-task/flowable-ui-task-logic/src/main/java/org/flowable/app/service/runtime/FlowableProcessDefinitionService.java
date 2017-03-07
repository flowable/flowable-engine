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
package org.flowable.app.service.runtime;

import java.net.URLDecoder;
import java.rmi.Remote;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.flowable.app.model.common.RemoteGroup;
import org.flowable.app.model.common.ResultListDataRepresentation;
import org.flowable.app.model.runtime.ProcessDefinitionRepresentation;
import org.flowable.app.security.SecurityUtils;
import org.flowable.app.service.api.UserCache;
import org.flowable.app.service.exception.BadRequestException;
import org.flowable.app.service.exception.InternalServerErrorException;
import org.flowable.app.service.exception.NotFoundException;
import org.flowable.app.service.idm.RemoteIdmService;
import org.flowable.app.service.idm.UserCacheImpl;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Process;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.editor.language.json.converter.util.CollectionUtils;
import org.flowable.engine.IdentityService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.common.api.FlowableObjectNotFoundException;
import org.flowable.engine.impl.ServiceImpl;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.engine.task.IdentityLink;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.model.FormField;
import org.flowable.form.model.FormModel;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.User;
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

    private static final Logger logger = LoggerFactory.getLogger(FlowableProcessDefinitionService.class);

    @Autowired
    protected RepositoryService repositoryService;

    @Autowired
    protected FormRepositoryService formRepositoryService;

    @Autowired
    protected ObjectMapper objectMapper;
    
    
    @Autowired
    protected RemoteIdmService remoteIdmService;

    public FormModel getProcessDefinitionStartForm(HttpServletRequest request) {

        String[] requestInfoArray = parseRequest(request);
        String processDefinitionId = getProcessDefinitionId(requestInfoArray, requestInfoArray.length - 2);

        ProcessDefinition processDefinition = repositoryService.getProcessDefinition(processDefinitionId);

        try {
            return getStartForm(processDefinition);

        } catch (FlowableObjectNotFoundException aonfe) {
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
        ///getting logged user
        User currentUser = SecurityUtils.getCurrentUserObject();
        String userId=currentUser.getId();
        ///getting all the processes of the app
        List<ProcessDefinition> definitions = definitionQuery.list();
        List<ProcessDefinition> definitionsFiltered=new ArrayList();
        ///getting the groups by user
        List<RemoteGroup> listaGruposXUsuario=remoteIdmService.getUser(userId).getGroups();
        
        for(ProcessDefinition pd:definitions){
         BpmnModel bpmnModel=repositoryService.getBpmnModel(pd.getId());
         List<Process> listaProcesos=bpmnModel.getProcesses();
         	for(Process p:listaProcesos){
         		///get the new properties of restriction added in the modeler app by process
         		List<String> listaGrupos=p.getCandidateStarterGroups();
         		List<String> listaUsuarios=p.getCandidateStarterUsers();
         		///checking if user is contained as potential user
         		if(listaUsuarios.contains(userId)){
         			definitionsFiltered.add(pd);
         		}
         		///comparing the groups of the user and the groups defined in the modeler app
         		//if the gruops are the same and dont exist in the current list of process so we addit to the response
         		for(String g:listaGrupos){
         			for(RemoteGroup rg: listaGruposXUsuario){
         				if(rg.getId().equals(g)){
         					if(!definitionsFiltered.contains(pd)){
         						definitionsFiltered.add(pd);
         					}
         				}
         			}
         		}
         	}
        }
       
        ResultListDataRepresentation result = new ResultListDataRepresentation(convertDefinitionList(definitionsFiltered));
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
        List<ProcessDefinitionRepresentation> result = new ArrayList<ProcessDefinitionRepresentation>();
        if (CollectionUtils.isNotEmpty(definitions)) {
            for (ProcessDefinition processDefinition : definitions) {
                ProcessDefinitionRepresentation rep = new ProcessDefinitionRepresentation(processDefinition);
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
            logger.error("Error decoding process definition {}", processDefinitionVariable, e);
            throw new InternalServerErrorException("Error decoding process definition " + processDefinitionVariable);
        }
        return processDefinitionId;
    }
}
