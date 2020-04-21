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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.app.api.AppRepositoryService;
import org.flowable.app.api.repository.AppDefinition;
import org.flowable.app.api.repository.AppDeployment;
import org.flowable.app.engine.impl.deployer.BaseAppModel;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.editor.language.json.converter.util.CollectionUtils;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.form.api.FormDeployment;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.idm.api.User;
import org.flowable.ui.common.model.RemoteGroup;
import org.flowable.ui.common.model.ResultListDataRepresentation;
import org.flowable.ui.common.security.SecurityUtils;
import org.flowable.ui.common.service.exception.NotFoundException;
import org.flowable.ui.common.service.idm.RemoteIdmService;
import org.flowable.ui.task.model.runtime.AppDefinitionRepresentation;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableAppDefinitionService.class);

    @Autowired
    protected AppRepositoryService appRepositoryService;
    
    @Autowired
    protected RepositoryService repositoryService;
    
    @Autowired
    protected CmmnRepositoryService cmmnRepositoryService;
    
    @Autowired
    protected DmnRepositoryService dmnRepositoryService;
    
    @Autowired
    protected FormRepositoryService formRepositoryService;

    @Autowired
    protected RemoteIdmService remoteIdmService;

    @Autowired
    protected ObjectMapper objectMapper;

    protected static final AppDefinitionRepresentation taskAppDefinitionRepresentation = AppDefinitionRepresentation.createDefaultAppDefinitionRepresentation("tasks");

    public ResultListDataRepresentation getAppDefinitions() {
        List<AppDefinitionRepresentation> resultList = new ArrayList<>();

        // Default app: tasks (available for all)
        resultList.add(taskAppDefinitionRepresentation);

        // Custom apps
        List<AppDefinition> appDefinitions = appRepositoryService.createAppDefinitionQuery().latestVersion().list();
        
        boolean appDefinitionHaveAccessControl = false;
        for (AppDefinition appDefinition : appDefinitions) {
            BaseAppModel baseAppModel = (BaseAppModel) appRepositoryService.getAppModel(appDefinition.getId());
            if (StringUtils.isNotEmpty(baseAppModel.getUsersAccess()) || StringUtils.isNotEmpty(baseAppModel.getGroupsAccess())) {
                appDefinitionHaveAccessControl = true;
            }

            resultList.add(createRepresentation(appDefinition, baseAppModel));
        }

        if (appDefinitionHaveAccessControl) {
            User currentUser = SecurityUtils.getCurrentUserObject();
            String userId = currentUser.getId();
            List<RemoteGroup> groups = getUserGroups(userId);

            List<AppDefinitionRepresentation> appDefinitionList = new ArrayList<>(resultList);
            resultList.clear();

            for (AppDefinitionRepresentation appDefinition : appDefinitionList) {
                if (hasAppAccess(appDefinition, userId, groups)) {
                    resultList.add(appDefinition);
                }
            }
        }

        ResultListDataRepresentation result = new ResultListDataRepresentation(resultList);
        return result;
    }

    public AppDefinitionRepresentation getAppDefinition(String appDefinitionKey) {
        AppDefinition appDefinition = appRepositoryService.createAppDefinitionQuery().appDefinitionKey(appDefinitionKey).latestVersion().singleResult();

        if (appDefinition == null) {
            throw new NotFoundException("No app definition is found with key: " + appDefinitionKey);
        }
        
        BaseAppModel appModel = (BaseAppModel) appRepositoryService.getAppModel(appDefinition.getId());

        return createRepresentation(appDefinition, appModel);
    }
    
    public String migrateAppDefinitions() {
        List<Deployment> deployments = new ArrayList<>();
        List<Deployment> processDeployments = repositoryService.createDeploymentQuery().orderByDeploymentTime().asc().list();
        for (Deployment deployment : processDeployments) {
            if (deployment.getKey() != null && deployment.getParentDeploymentId() == null) {
                deployments.add(deployment);
            }
        }
        
        Map<String, String> deploymentIdMap = new HashMap<>();
        for (Deployment deployment : deployments) {
            List<String> resourceNames = repositoryService.getDeploymentResourceNames(deployment.getId());
            String resourceAppName = null;
            for (String resourceName : resourceNames) {
                if (resourceName != null && resourceName.endsWith(".app")) {
                    resourceAppName = resourceName;
                    break;
                }
            }
            
            if (resourceAppName != null) {
                AppDeployment appDeployment = appRepositoryService.createDeployment().addInputStream(resourceAppName, 
                            repositoryService.getResourceAsStream(deployment.getId(), resourceAppName)).deploy();
                deploymentIdMap.put(deployment.getId(), appDeployment.getId());
            }
        }
        
        for (String oldDeploymentId : deploymentIdMap.keySet()) {
            List<CmmnDeployment> cmmnDeployments = cmmnRepositoryService.createDeploymentQuery().parentDeploymentId(oldDeploymentId).list();
            if (cmmnDeployments != null) {
                for (CmmnDeployment cmmnDeployment : cmmnDeployments) {
                    cmmnRepositoryService.changeDeploymentParentDeploymentId(cmmnDeployment.getId(), deploymentIdMap.get(oldDeploymentId));
                }
            }
            
            List<DmnDeployment> dmnDeployments = dmnRepositoryService.createDeploymentQuery().parentDeploymentId(oldDeploymentId).list();
            if (dmnDeployments != null) {
                for (DmnDeployment dmnDeployment : dmnDeployments) {
                    dmnRepositoryService.changeDeploymentParentDeploymentId(dmnDeployment.getId(), deploymentIdMap.get(oldDeploymentId));
                }
            }
            
            List<FormDeployment> formDeployments = formRepositoryService.createDeploymentQuery().parentDeploymentId(oldDeploymentId).list();
            if (formDeployments != null) {
                for (FormDeployment formDeployment : formDeployments) {
                    formRepositoryService.changeDeploymentParentDeploymentId(formDeployment.getId(), deploymentIdMap.get(oldDeploymentId));
                }
            }
            
            repositoryService.changeDeploymentParentDeploymentId(oldDeploymentId, deploymentIdMap.get(oldDeploymentId));
        }
        
        return "Migrated " + deploymentIdMap.size() + " app deployments";
    }

    protected List<RemoteGroup> getUserGroups(String userId) {
        return remoteIdmService.getUser(userId).getGroups();
    }

    protected boolean hasAppAccess(AppDefinitionRepresentation appDefinition, String userId, List<RemoteGroup> groups) {
        if (CollectionUtils.isEmpty(appDefinition.getUsersAccess()) && CollectionUtils.isEmpty(appDefinition.getGroupsAccess())) {
            return true;
        }

        if (CollectionUtils.isNotEmpty(appDefinition.getUsersAccess())) {
            if (appDefinition.getUsersAccess().contains(userId)) {
                return true;
            }
        }

        if (CollectionUtils.isNotEmpty(appDefinition.getGroupsAccess())) {
            for (String groupId : appDefinition.getGroupsAccess()) {
                for (RemoteGroup group : groups) {
                    if (group.getId().equals(groupId)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    protected AppDefinitionRepresentation createDefaultAppDefinition(String id) {
        AppDefinitionRepresentation app = new AppDefinitionRepresentation();
        return app;
    }

    protected AppDefinitionRepresentation createRepresentation(AppDefinition appDefinition, BaseAppModel baseAppModel) {
        AppDefinitionRepresentation resultAppDef = new AppDefinitionRepresentation();
        resultAppDef.setAppDefinitionId(appDefinition.getId());
        resultAppDef.setAppDefinitionKey(appDefinition.getKey());
        resultAppDef.setName(appDefinition.getName());
        resultAppDef.setTheme(baseAppModel.getTheme());
        resultAppDef.setIcon(baseAppModel.getIcon());
        resultAppDef.setDescription(baseAppModel.getDescription());
        resultAppDef.setTenantId(appDefinition.getTenantId());
        if (StringUtils.isNotEmpty(baseAppModel.getUsersAccess())) {
            resultAppDef.setUsersAccess(convertToList(baseAppModel.getUsersAccess()));
        }

        if (StringUtils.isNotEmpty(baseAppModel.getGroupsAccess())) {
            resultAppDef.setGroupsAccess(convertToList(baseAppModel.getGroupsAccess()));
        }

        return resultAppDef;
    }

    protected List<String> convertToList(String commaSeperatedString) {
        List<String> resultList = new ArrayList<>();
        String[] stringArray = commaSeperatedString.split(",");
        resultList.addAll(Arrays.asList(stringArray));

        return resultList;
    }
}
