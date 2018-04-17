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

package org.flowable.app.engine.impl.repository;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.query.QueryProperty;

/**
 * @author Joram Barrez
 */
public class AppDefinitionQueryProperty implements QueryProperty {

    private static final long serialVersionUID = 1L;

    private static final Map<String, AppDefinitionQueryProperty> properties = new HashMap<>();

    public static final AppDefinitionQueryProperty APP_DEFINITION_KEY = new AppDefinitionQueryProperty("RES.KEY_");
    public static final AppDefinitionQueryProperty APP_DEFINITION_CATEGORY = new AppDefinitionQueryProperty("RES.CATEGORY_");
    public static final AppDefinitionQueryProperty APP_DEFINITION_ID = new AppDefinitionQueryProperty("RES.ID_");
    public static final AppDefinitionQueryProperty APP_DEFINITION_VERSION = new AppDefinitionQueryProperty("RES.VERSION_");
    public static final AppDefinitionQueryProperty APP_DEFINITION_NAME = new AppDefinitionQueryProperty("RES.NAME_");
    public static final AppDefinitionQueryProperty APP_DEFINITION_DEPLOYMENT_ID = new AppDefinitionQueryProperty("RES.DEPLOYMENT_ID_");
    public static final AppDefinitionQueryProperty APP_DEFINITION_TENANT_ID = new AppDefinitionQueryProperty("RES.TENANT_ID_");

    private String name;

    public AppDefinitionQueryProperty(String name) {
        this.name = name;
        properties.put(name, this);
    }

    public String getName() {
        return name;
    }

    public static AppDefinitionQueryProperty findByName(String propertyName) {
        return properties.get(propertyName);
    }

}
