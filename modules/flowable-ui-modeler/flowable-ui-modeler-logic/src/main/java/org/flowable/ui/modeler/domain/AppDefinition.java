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
package org.flowable.ui.modeler.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * POJO representation of the app model JSON.
 * 
 * @author Tijs Rademakers
 */
@JsonInclude(Include.NON_NULL)
public class AppDefinition {

    protected List<AppModelDefinition> models;
    protected List<AppModelDefinition> cmmnModels;
    protected String name;
    protected String key;
    protected String theme;
    protected String icon;
    protected String groupsAccess;
    protected String usersAccess;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
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

    public List<AppModelDefinition> getModels() {
        return models;
    }

    public void setModels(List<AppModelDefinition> models) {
        this.models = models;
    }
    
    public List<AppModelDefinition> getCmmnModels() {
        return cmmnModels;
    }

    public void setCmmnModels(List<AppModelDefinition> cmmnModels) {
        this.cmmnModels = cmmnModels;
    }

    public String getGroupsAccess() {
        return groupsAccess;
    }

    public void setGroupsAccess(String groupsAccess) {
        this.groupsAccess = groupsAccess;
    }

    public String getUsersAccess() {
        return usersAccess;
    }

    public void setUsersAccess(String usersAccess) {
        this.usersAccess = usersAccess;
    }
}
