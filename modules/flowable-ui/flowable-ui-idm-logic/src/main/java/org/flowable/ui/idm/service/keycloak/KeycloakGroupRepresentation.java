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
package org.flowable.ui.idm.service.keycloak;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author Filip Hrisafov
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeycloakGroupRepresentation {

    protected String id;
    protected String name;
    protected String path;
    protected List<KeycloakGroupRepresentation> subGroups;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<KeycloakGroupRepresentation> getSubGroups() {
        return subGroups;
    }

    public void setSubGroups(List<KeycloakGroupRepresentation> subGroups) {
        this.subGroups = subGroups;
    }
}
