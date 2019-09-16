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
package org.flowable.common.engine.impl.eventbus;

import java.util.Map;

public class AbstractFlowableEventBusItemBuilder implements FlowableEventBusItemConstants {
    
    protected static void addProcessInfo(String executionId, String processInstanceId, String processDefinitionId, Map<String, Object> data) {
        putIfNotNull(EXECUTION_ID, executionId, data);
        putIfNotNull(PROCESS_INSTANCE_ID, processInstanceId, data);
        putIfNotNull(PROCESS_DEFINITION_ID, processDefinitionId, data);
    }

    protected static void addScopeInfo(String scopeId, String subScopeId, String scopeType, Map<String, Object> data) {
        putIfNotNull(SCOPE_ID, scopeId, data);
        putIfNotNull(SUB_SCOPE_ID, subScopeId, data);
        putIfNotNull(SCOPE_TYPE, scopeType, data);
    }
    
    protected static void putIfNotNull(String key, String value, Map<String, Object> data) {
        if (value != null) {
            data.put(key, value);
        }
    }
}
