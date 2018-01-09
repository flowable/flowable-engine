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
package org.activiti.engine.impl.jobexecutor;

import org.activiti.engine.impl.context.Context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 */
public abstract class TimerChangeProcessDefinitionSuspensionStateJobHandler implements JobHandler {

    private static final String JOB_HANDLER_CFG_INCLUDE_PROCESS_INSTANCES = "includeProcessInstances";

    public static String createJobHandlerConfiguration(boolean includeProcessInstances) {
        ObjectNode jsonNode = Context.getProcessEngineConfiguration().getObjectMapper().createObjectNode();
        jsonNode.put(JOB_HANDLER_CFG_INCLUDE_PROCESS_INSTANCES, includeProcessInstances);
        return jsonNode.toString();
    }

    public static boolean getIncludeProcessInstances(JsonNode configNode) {
        if (configNode.has(JOB_HANDLER_CFG_INCLUDE_PROCESS_INSTANCES)) {
            return configNode.get(JOB_HANDLER_CFG_INCLUDE_PROCESS_INSTANCES).asBoolean();
        }
        return false;
    }

}
