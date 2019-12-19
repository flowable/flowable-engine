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

package org.flowable.eventregistry.impl;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.eventregistry.api.EventDefinitionQuery;

/**
 * Contains the possible properties that can be used in a {@link EventDefinitionQuery}.
 * 
 * @author Joram Barrez
 */
public class EventDefinitionQueryProperty implements QueryProperty {

    private static final long serialVersionUID = 1L;

    private static final Map<String, EventDefinitionQueryProperty> properties = new HashMap<>();

    public static final EventDefinitionQueryProperty KEY = new EventDefinitionQueryProperty("RES.KEY_");
    public static final EventDefinitionQueryProperty CATEGORY = new EventDefinitionQueryProperty("RES.CATEGORY_");
    public static final EventDefinitionQueryProperty ID = new EventDefinitionQueryProperty("RES.ID_");
    public static final EventDefinitionQueryProperty NAME = new EventDefinitionQueryProperty("RES.NAME_");
    public static final EventDefinitionQueryProperty DEPLOYMENT_ID = new EventDefinitionQueryProperty("RES.DEPLOYMENT_ID_");
    public static final EventDefinitionQueryProperty TENANT_ID = new EventDefinitionQueryProperty("RES.TENANT_ID_");

    private String name;

    public EventDefinitionQueryProperty(String name) {
        this.name = name;
        properties.put(name, this);
    }

    @Override
    public String getName() {
        return name;
    }

    public static EventDefinitionQueryProperty findByName(String propertyName) {
        return properties.get(propertyName);
    }

}
