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
import org.flowable.common.engine.api.FlowableIllegalStateException;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.editor.language.json.converter.util.CollectionUtils;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.form.api.FormDeployment;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.api.User;
import org.flowable.ui.common.model.RemoteGroup;
import org.flowable.ui.common.model.ResultListDataRepresentation;
import org.flowable.ui.common.security.DefaultPrivileges;
import org.flowable.ui.common.security.SecurityUtils;
import org.flowable.ui.common.service.exception.NotFoundException;
import org.flowable.ui.common.service.idm.RemoteIdmService;
import org.flowable.ui.task.model.runtime.AppDefinitionRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ClassUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Tijs Rademakers
 */
@Service
@Transactional
public class FlowableAppDefinitionService implements InitializingBean {

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

    @Autowired(required = false)
    protected RemoteIdmService remoteIdmService;

    @Autowired(required = false)
    protected IdmIdentityService identityService;

    @Autowired
    protected ObjectMapper objectMapper;

    protected static final AppDefinitionRepresentation taskAppDefinitionRepresentation = AppDefinitionRepresentation.createDefaultAppDefinitionRepresentation("tasks");
    protected static final AppDefinitionRepresentation adminAppDefinitionRepresentation;
    protected static final AppDefinitionRepresentation idmAppDefinitionRepresentation;

    static {
        if (ClassUtils.isPresent("org.flowable.ui.admin.conf.ApplicationConfiguration", null)) {
            adminAppDefinitionRepresentation = AppDefinitionRepresentation.createDefaultAppDefinitionRepresentation("admin");
        } else {
            adminAppDefinitionRepresentation = null;
        }

        if (ClassUtils.isPresent("org.flowable.ui.idm.service.GroupServiceImpl", null)) {
            idmAppDefinitionRepresentation = AppDefinitionRepresentation.createDefaultAppDefinitionRepresentation("idm");
        } else {
            idmAppDefinitionRepresentation = null;
        }
    }

    @Override
    public void afterPropertiesSet() {
        if (remoteIdmService == null && identityService == null) {
            throw new FlowableIllegalStateException("No remoteIdmService or identityService have been provided");
        }
    }

    public ResultListDataRepresentation getAppDefinitions() {
        List<AppDefinitionRepresentation> resultList = new ArrayList<>();

        if (adminAppDefinitionRepresentation != null && SecurityUtils.currentUserHasCapability(DefaultPrivileges.ACCESS_ADMIN)) {
            resultList.add(adminAppDefinitionRepresentation);
        }

        if (idmAppDefinitionRepresentation != null && SecurityUtils.currentUserHasCapability(DefaultPrivileges.ACCESS_IDM)) {
            resultList.add(idmAppDefinitionRepresentation);
        }


        if (SecurityUtils.currentUserHasCapability(DefaultPrivileges.ACCESS_TASK)) {
            resultList.addAll(getTaskAppList());
        }

        ResultListDataRepresentation result = new ResultListDataRepresentation(resultList);
        return result;
    }

    protected List<AppDefinitionRepresentation> getTaskAppList() {
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
            List<? extends Group> groups = getUserGroups(userId);

            List<AppDefinitionRepresentation> appDefinitionList = new ArrayList<>(resultList);
            resultList.clear();

            for (AppDefinitionRepresentation appDefinition : appDefinitionList) {
                if (hasAppAccess(appDefinition, userId, groups)) {
                    resultList.add(appDefinition);
                }
            }
        }

        return resultList;
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

    protected List<? extends Group> getUserGroups(String userId) {
        if (remoteIdmService != null) {
            return remoteIdmService.getUser(userId).getGroups();
        } else if (identityService != null) {
            return identityService.createGroupQuery().groupMember(userId).list();
        } else {
            throw new FlowableIllegalStateException("No remoteIdmService or identityService have been provided");
        }
    }

    protected boolean hasAppAccess(AppDefinitionRepresentation appDefinition, String userId, List<? extends Group> groups) {
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
                for (Group group : groups) {
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
