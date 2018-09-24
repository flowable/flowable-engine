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

package org.flowable.cmmn.engine.impl.history;

import org.flowable.common.engine.api.query.QueryProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Dennis Federico
 */
public class HistoricPlanItemInstanceQueryProperty implements QueryProperty {

    private static final long serialVersionUID = 1L;

    private static final Map<String, HistoricPlanItemInstanceQueryProperty> properties = new HashMap<>();

    public static final HistoricPlanItemInstanceQueryProperty CREATED_TIME = new HistoricPlanItemInstanceQueryProperty("RES.CREATED_TIME_");
    public static final HistoricPlanItemInstanceQueryProperty ENDED_TIME = new HistoricPlanItemInstanceQueryProperty("RES.ENDED_TIME_");
    public static final HistoricPlanItemInstanceQueryProperty NAME = new HistoricPlanItemInstanceQueryProperty("RES.NAME_");

    private String name;

    public HistoricPlanItemInstanceQueryProperty(String name) {
        this.name = name;
        properties.put(name, this);
    }

    @Override
    public String getName() {
        return name;
    }
}
