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
import org.flowable.eventregistry.api.ChannelDefinitionQuery;

/**
 * Contains the possible properties that can be used in a {@link ChannelDefinitionQuery}.
 */
public class ChannelDefinitionQueryProperty implements QueryProperty {

    private static final long serialVersionUID = 1L;

    private static final Map<String, ChannelDefinitionQueryProperty> properties = new HashMap<>();

    public static final ChannelDefinitionQueryProperty KEY = new ChannelDefinitionQueryProperty("RES.KEY_");
    public static final ChannelDefinitionQueryProperty CATEGORY = new ChannelDefinitionQueryProperty("RES.CATEGORY_");
    public static final ChannelDefinitionQueryProperty ID = new ChannelDefinitionQueryProperty("RES.ID_");
    public static final ChannelDefinitionQueryProperty NAME = new ChannelDefinitionQueryProperty("RES.NAME_");
    public static final ChannelDefinitionQueryProperty DEPLOYMENT_ID = new ChannelDefinitionQueryProperty("RES.DEPLOYMENT_ID_");
    public static final ChannelDefinitionQueryProperty CREATE_TIME = new ChannelDefinitionQueryProperty("RES.CREATE_TIME_");
    public static final ChannelDefinitionQueryProperty TENANT_ID = new ChannelDefinitionQueryProperty("RES.TENANT_ID_");

    private String name;

    public ChannelDefinitionQueryProperty(String name) {
        this.name = name;
        properties.put(name, this);
    }

    @Override
    public String getName() {
        return name;
    }

    public static ChannelDefinitionQueryProperty findByName(String propertyName) {
        return properties.get(propertyName);
    }

}
