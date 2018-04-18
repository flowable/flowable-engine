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
package org.flowable.rest.service.api.runtime.process;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.engine.impl.EventSubscriptionQueryProperty;

/**
 * @author Tijs Rademakers
 */
public class EventSubscriptionQueryProperties {

    public static Map<String, QueryProperty> PROPERTIES;

    static {
        PROPERTIES = new HashMap<>();
        PROPERTIES.put("id", EventSubscriptionQueryProperty.ID);
        PROPERTIES.put("created", EventSubscriptionQueryProperty.CREATED);
        PROPERTIES.put("executionId", EventSubscriptionQueryProperty.EXECUTION_ID);
        PROPERTIES.put("processInstanceId", EventSubscriptionQueryProperty.PROCESS_INSTANCE_ID);
        PROPERTIES.put("processDefinitionId", EventSubscriptionQueryProperty.PROCESS_DEFINITION_ID);
        PROPERTIES.put("tenantId", EventSubscriptionQueryProperty.TENANT_ID);
    }

}
