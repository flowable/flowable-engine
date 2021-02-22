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
import java.util.Collection;
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
import org.flowable.ui.common.model.ResultListDataRepresentation;
import org.flowable.ui.common.security.DefaultPrivileges;
import org.flowable.ui.common.security.SecurityScope;
import org.flowable.ui.common.security.SecurityUtils;
import org.flowable.ui.common.service.exception.NotFoundException;
import org.flowable.ui.task.model.runtime.AppDefinitionRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    protected ObjectMapper objectMapper;

    protected static final AppDefinitionRepresentation taskAppDefinitionRepresentation = AppDefinitionRepresentation.createDefaultAppDefinitionRepresentation("tasks");
    protected static final AppDefinitionRepresentation adminAppDefinitionRepresentation;
    protected static final AppDefinitionRepresentation idmAppDefinitionRepresentation;
    protected static final AppDefinitionRepresentation modelerAppDefinitionRepresentation;

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

        if (ClassUtils.isPresent("org.flowable.ui.modeler.conf.ApplicationConfiguration", null)) {
            modelerAppDefinitionRepresentation = AppDefinitionRepresentation.createDefaultAppDefinitionRepresentation("modeler");
        } else {
            modelerAppDefinitionRepresentation = null;
        }
    }

    public ResultListDataRepresentation getAppDefinitions() {
        List<AppDefinitionRepresentation> resultList = new ArrayList<>();

        SecurityScope currentSecurityScope = SecurityUtils.getAuthenticatedSecurityScope();

        boolean hasAccessToTask = currentSecurityScope.hasAuthority(DefaultPrivileges.ACCESS_TASK);

        if (hasAccessToTask) {
            resultList.add(taskAppDefinitionRepresentation);
        }

        if (modelerAppDefinitionRepresentation != null && currentSecurityScope.hasAuthority(DefaultPrivileges.ACCESS_MODELER)) {
            resultList.add(modelerAppDefinitionRepresentation);
        }

        if (adminAppDefinitionRepresentation != null && currentSecurityScope.hasAuthority(DefaultPrivileges.ACCESS_ADMIN)) {
            resultList.add(adminAppDefinitionRepresentation);
        }

        if (idmAppDefinitionRepresentation != null && currentSecurityScope.hasAuthority(DefaultPrivileges.ACCESS_IDM)) {
            resultList.add(idmAppDefinitionRepresentation);
        }

        if (hasAccessToTask) {
            resultList.addAll(getTaskAppList(currentSecurityScope));
        }

        ResultListDataRepresentation result = new ResultListDataRepresentation(resultList);
        return result;
    }

    protected List<AppDefinitionRepresentation> getTaskAppList(SecurityScope currentUser) {
        List<AppDefinitionRepresentation> resultList = new ArrayList<>();

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
            String userId = currentUser.getUserId();
            Collection<String> groups = currentUser.getGroupIds();

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

    protected boolean hasAppAccess(AppDefinitionRepresentation appDefinition, String userId, Collection<String> groups) {
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
                for (String group : groups) {
                    if (group.equals(groupId)) {
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
        String[] stringArray = commaSeperatedString.split(",");
        List<String> resultList = new ArrayList<>(Arrays.asList(stringArray));

        return resultList;
    }
}
