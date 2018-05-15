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
package org.flowable.ui.task.model.runtime;

import java.util.List;

import org.flowable.ui.common.model.AbstractRepresentation;

public class AppDefinitionRepresentation extends AbstractRepresentation {

    protected String defaultAppId;
    protected String name;
    protected String description;
    protected String theme;
    protected String icon;
    protected String appDefinitionId;
    protected String appDefinitionKey;
    protected String tenantId;
    protected List<String> usersAccess;
    protected List<String> groupsAccess;

    public static AppDefinitionRepresentation createDefaultAppDefinitionRepresentation(String id) {
        AppDefinitionRepresentation appDefinitionRepresentation = new AppDefinitionRepresentation();
        appDefinitionRepresentation.setDefaultAppId(id);
        return appDefinitionRepresentation;
    }

    public String getDefaultAppId() {
        return defaultAppId;
    }

    public void setDefaultAppId(String defaultAppId) {
        this.defaultAppId = defaultAppId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getAppDefinitionId() {
        return appDefinitionId;
    }

    public void setAppDefinitionId(String appDefinitionId) {
        this.appDefinitionId = appDefinitionId;
    }

    public String getAppDefinitionKey() {
        return appDefinitionKey;
    }

    public void setAppDefinitionKey(String appDefinitionKey) {
        this.appDefinitionKey = appDefinitionKey;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public List<String> getUsersAccess() {
        return usersAccess;
    }

    public void setUsersAccess(List<String> usersAccess) {
        this.usersAccess = usersAccess;
    }

    public List<String> getGroupsAccess() {
        return groupsAccess;
    }

    public void setGroupsAccess(List<String> groupsAccess) {
        this.groupsAccess = groupsAccess;
    }
}
