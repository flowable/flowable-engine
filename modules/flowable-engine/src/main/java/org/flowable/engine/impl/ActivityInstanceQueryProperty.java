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

package org.flowable.engine.impl;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.engine.runtime.ActivityInstanceQuery;

/**
 * Contains the possible properties which can be used in a {@link ActivityInstanceQuery}.
 * 
 * @author martin.grofcik
 */
public class ActivityInstanceQueryProperty implements QueryProperty {

    private static final long serialVersionUID = 1L;

    private static final Map<String, ActivityInstanceQueryProperty> properties = new HashMap<>();

    public static final ActivityInstanceQueryProperty ACTIVITY_INSTANCE_ID = new ActivityInstanceQueryProperty("ID_");
    public static final ActivityInstanceQueryProperty PROCESS_INSTANCE_ID = new ActivityInstanceQueryProperty("PROC_INST_ID_");
    public static final ActivityInstanceQueryProperty EXECUTION_ID = new ActivityInstanceQueryProperty("EXECUTION_ID_");
    public static final ActivityInstanceQueryProperty ACTIVITY_ID = new ActivityInstanceQueryProperty("ACT_ID_");
    public static final ActivityInstanceQueryProperty ACTIVITY_NAME = new ActivityInstanceQueryProperty("ACT_NAME_");
    public static final ActivityInstanceQueryProperty ACTIVITY_TYPE = new ActivityInstanceQueryProperty("ACT_TYPE_");
    public static final ActivityInstanceQueryProperty PROCESS_DEFINITION_ID = new ActivityInstanceQueryProperty("PROC_DEF_ID_");
    public static final ActivityInstanceQueryProperty START = new ActivityInstanceQueryProperty("START_TIME_");
    public static final ActivityInstanceQueryProperty END = new ActivityInstanceQueryProperty("END_TIME_");
    public static final ActivityInstanceQueryProperty DURATION = new ActivityInstanceQueryProperty("DURATION_");
    public static final ActivityInstanceQueryProperty TENANT_ID = new ActivityInstanceQueryProperty("TENANT_ID_");

    private String name;

    public ActivityInstanceQueryProperty(String name) {
        this.name = name;
        properties.put(name, this);
    }

    @Override
    public String getName() {
        return name;
    }

    public static ActivityInstanceQueryProperty findByName(String propertyName) {
        return properties.get(propertyName);
    }
}
