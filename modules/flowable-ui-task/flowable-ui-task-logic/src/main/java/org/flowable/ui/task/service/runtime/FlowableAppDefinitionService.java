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
import org.flowable.editor.language.json.converter.util.CollectionUtils;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.app.AppModel;
import org.flowable.engine.repository.Deployment;
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
    protected RepositoryService repositoryService;

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
        Map<String, Deployment> deploymentMap = new HashMap<>();
        List<Deployment> deployments = repositoryService.createDeploymentQuery().list();
        for (Deployment deployment : deployments) {
            if (deployment.getKey() != null) {
                if (!deploymentMap.containsKey(deployment.getKey())) {
                    deploymentMap.put(deployment.getKey(), deployment);
                } else {
                    Deployment compareDeployment = deploymentMap.get(deployment.getKey());
                    if (deployment.getDerivedFrom() == null && compareDeployment.getDeploymentTime().before(deployment.getDeploymentTime())) {
                        deploymentMap.put(deployment.getKey(), deployment);
                    }
                }
            }
        }

        boolean appDefinitionHaveAccessControl = false;
        for (Deployment deployment : deploymentMap.values()) {
            AppDefinitionRepresentation appDefinition = createRepresentation(deployment);
            if (CollectionUtils.isNotEmpty(appDefinition.getUsersAccess()) || CollectionUtils.isNotEmpty(appDefinition.getGroupsAccess())) {
                appDefinitionHaveAccessControl = true;
            }

            resultList.add(appDefinition);
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

    public AppDefinitionRepresentation getAppDefinition(String deploymentKey) {
        Deployment deployment = repositoryService.createDeploymentQuery().deploymentKey(deploymentKey).latest().singleResult();

        if (deployment == null) {
            throw new NotFoundException("No app definition is found with key: " + deploymentKey);
        }

        return createRepresentation(deployment);
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

    protected AppDefinitionRepresentation createRepresentation(Deployment deployment) {
        AppDefinitionRepresentation resultAppDef = new AppDefinitionRepresentation();
        resultAppDef.setDeploymentId(deployment.getId());
        resultAppDef.setDeploymentKey(deployment.getKey());
        resultAppDef.setName(deployment.getName());
        AppModel appModel = repositoryService.getAppResourceModel(deployment.getId());
        resultAppDef.setTheme(appModel.getTheme());
        resultAppDef.setIcon(appModel.getIcon());
        resultAppDef.setDescription(appModel.getDescription());
        if (StringUtils.isNotEmpty(appModel.getUsersAccess())) {
            resultAppDef.setUsersAccess(convertToList(appModel.getUsersAccess()));
        }

        if (StringUtils.isNotEmpty(appModel.getGroupsAccess())) {
            resultAppDef.setGroupsAccess(convertToList(appModel.getGroupsAccess()));
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
