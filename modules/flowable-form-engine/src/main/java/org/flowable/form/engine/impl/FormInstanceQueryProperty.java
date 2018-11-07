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

package org.flowable.form.engine.impl;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.form.api.FormInstanceQuery;

/**
 * Contains the possible properties that can be used in a {@link FormInstanceQuery}.
 * 
 * @author Tijs Rademakers
 */
public class FormInstanceQueryProperty implements QueryProperty {

    private static final long serialVersionUID = 1L;

    private static final Map<String, FormInstanceQueryProperty> properties = new HashMap<>();

    public static final FormInstanceQueryProperty SUBMITTED_DATE = new FormInstanceQueryProperty("RES.SUBMITTED_DATE_");
    public static final FormInstanceQueryProperty TENANT_ID = new FormInstanceQueryProperty("RES.TENANT_ID_");

    private String name;

    public FormInstanceQueryProperty(String name) {
        this.name = name;
        properties.put(name, this);
    }

    @Override
    public String getName() {
        return name;
    }

    public static FormInstanceQueryProperty findByName(String propertyName) {
        return properties.get(propertyName);
    }

}
