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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.app.model.common.RemoteGroup;
import org.flowable.app.model.common.ResultListDataRepresentation;
import org.flowable.app.model.runtime.AppDefinitionRepresentation;
import org.flowable.app.security.SecurityUtils;
import org.flowable.app.service.exception.NotFoundException;
import org.flowable.app.service.idm.RemoteIdmService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.app.AppModel;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
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
public class FlowableAppDefinitionService {

    private static final Logger logger = LoggerFactory.getLogger(FlowableAppDefinitionService.class);

    @Autowired
    protected RepositoryService repositoryService;

    @Autowired
    protected ObjectMapper objectMapper;
    
    @Autowired
    protected RemoteIdmService remoteIdmService;

    protected static final AppDefinitionRepresentation taskAppDefinitionRepresentation = AppDefinitionRepresentation.createDefaultAppDefinitionRepresentation("tasks");

    public ResultListDataRepresentation getAppDefinitions() {
        List<AppDefinitionRepresentation> resultList = new ArrayList<AppDefinitionRepresentation>();

        // Default app: tasks (available for all)
        resultList.add(taskAppDefinitionRepresentation);

        // Custom apps
        Map<String, Deployment> deploymentMap = new HashMap<String, Deployment>();
        List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
        for (Deployment deployment : deployments) {
            if (deployment.getKey() != null) {
            	
            	AppDefinitionRepresentation app=getAppDefinition(deployment.getKey());
            	
            	
                if (!deploymentMap.containsKey(deployment.getKey())) {
                    deploymentMap.put(deployment.getKey(), deployment);
                } else if (deploymentMap.get(deployment.getKey()).getDeploymentTime().before(deployment.getDeploymentTime())) {
                    deploymentMap.put(deployment.getKey(), deployment);
                }
            }
        }

        for (Deployment deployment : deploymentMap.values()) {
            
            resultList.add(createRepresentation(deployment));
        }
        
        
//        ///sibok666 code
//        ///getting logged user
//        User currentUser = SecurityUtils.getCurrentUserObject();
//        String userId=currentUser.getId();
//        ///getting the groups by user
//        List<RemoteGroup> listaGruposXUsuario=remoteIdmService.getUser(userId).getGroups();
      
        
        ResultListDataRepresentation result = new ResultListDataRepresentation(resultList);
        return result;
    }

    public AppDefinitionRepresentation getAppDefinition(String deploymentKey) {
        Deployment deployment = repositoryService.createDeploymentQuery().deploymentKey(deploymentKey).latest().singleResult();

        if (deployment == null) {
            throw new NotFoundException("No app definition is found with key: " + deploymentKey);
        }

        return createRepresentation(deployment);
    }

    protected AppDefinitionRepresentation createDefaultAppDefinition(String id) {
        AppDefinitionRepresentation app = new AppDefinitionRepresentation();
        return app;
    }

    protected AppDefinitionRepresentation createRepresentation(Deployment deployment) {
        AppDefinitionRepresentation resultAppDef = new AppDefinitionRepresentation();
        resultAppDef.setDeploymentId(deployment.getId());
        resultAppDef.setDeploymentKey(deployment.getKey());
        resultAppDef.setName(deployment.getName());
        AppModel appModel = repositoryService.getAppResourceModel(deployment.getId());
        resultAppDef.setTheme(appModel.getTheme());
        resultAppDef.setIcon(appModel.getIcon());
        resultAppDef.setUsersAccess(appModel.getUsersAccess());
        resultAppDef.setGroupsAccess(appModel.getGroupsAccess());
        
        return resultAppDef;
    }
}
