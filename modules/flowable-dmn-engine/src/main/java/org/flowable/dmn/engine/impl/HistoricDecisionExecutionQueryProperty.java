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
import org.flowable.dmn.api.DmnHistoricDecisionExecutionQuery;

/**
 * Contains the possible properties that can be used in a {@link DmnHistoricDecisionExecutionQuery}.
 * 
 * @author Tijs Rademakers
 */
public class HistoricDecisionExecutionQueryProperty implements QueryProperty {

    private static final long serialVersionUID = 1L;

    private static final Map<String, HistoricDecisionExecutionQueryProperty> properties = new HashMap<>();

    public static final HistoricDecisionExecutionQueryProperty START_TIME = new HistoricDecisionExecutionQueryProperty("RES.START_TIME_");
    public static final HistoricDecisionExecutionQueryProperty END_TIME = new HistoricDecisionExecutionQueryProperty("RES.END_TIME_");
    public static final HistoricDecisionExecutionQueryProperty TENANT_ID = new HistoricDecisionExecutionQueryProperty("RES.TENANT_ID_");

    private String name;

    public HistoricDecisionExecutionQueryProperty(String name) {
        this.name = name;
        properties.put(name, this);
    }

    @Override
    public String getName() {
        return name;
    }

    public static HistoricDecisionExecutionQueryProperty findByName(String propertyName) {
        return properties.get(propertyName);
    }

}
