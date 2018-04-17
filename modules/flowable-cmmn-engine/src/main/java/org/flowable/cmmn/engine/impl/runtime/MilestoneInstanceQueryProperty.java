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

package org.flowable.cmmn.engine.impl.runtime;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.query.QueryProperty;

/**
 * @author Joram Barrez
 */
public class MilestoneInstanceQueryProperty implements QueryProperty {

    private static final long serialVersionUID = 1L;

    private static final Map<String, MilestoneInstanceQueryProperty> properties = new HashMap<>();

    public static final MilestoneInstanceQueryProperty MILESTONE_NAME = new MilestoneInstanceQueryProperty("RES.NAME_");
    public static final MilestoneInstanceQueryProperty MILESTONE_TIMESTAMP = new MilestoneInstanceQueryProperty("RES.TIME_STAMP_");

    private String name;

    public MilestoneInstanceQueryProperty(String name) {
        this.name = name;
        properties.put(name, this);
    }

    public String getName() {
        return name;
    }

    public static MilestoneInstanceQueryProperty findByName(String propertyName) {
        return properties.get(propertyName);
    }

}
