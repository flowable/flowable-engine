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

package org.flowable.dmn.engine.impl;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.dmn.api.DmnDecisionQuery;

/**
 * Contains the possible properties that can be used in a {@link DmnDecisionQuery}.
 * 
 * @author Joram Barrez
 * @author Yvo Swillens
 */
public class DecisionQueryProperty implements QueryProperty {

    private static final long serialVersionUID = 1L;

    private static final Map<String, DecisionQueryProperty> properties = new HashMap<>();

    public static final DecisionQueryProperty DECISION_KEY = new DecisionQueryProperty("RES.KEY_");
    public static final DecisionQueryProperty DECISION_CATEGORY = new DecisionQueryProperty("RES.CATEGORY_");
    public static final DecisionQueryProperty DECISION_ID = new DecisionQueryProperty("RES.ID_");
    public static final DecisionQueryProperty DECISION_VERSION = new DecisionQueryProperty("RES.VERSION_");
    public static final DecisionQueryProperty DECISION_NAME = new DecisionQueryProperty("RES.NAME_");
    public static final DecisionQueryProperty DECISION_DEPLOYMENT_ID = new DecisionQueryProperty("RES.DEPLOYMENT_ID_");
    public static final DecisionQueryProperty DECISION_TENANT_ID = new DecisionQueryProperty("RES.TENANT_ID_");
    public static final DecisionQueryProperty DECISION_TYPE = new DecisionQueryProperty("RES.DECISION_TYPE_");

    private String name;

    public DecisionQueryProperty(String name) {
        this.name = name;
        properties.put(name, this);
    }

    @Override
    public String getName() {
        return name;
    }

    public static DecisionQueryProperty findByName(String propertyName) {
        return properties.get(propertyName);
    }

}
