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

package org.flowable.engine.impl.jobexecutor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.impl.util.CommandContextUtil;

public class AsyncContinuationEventHandler {
    public static final String PROPERTY_NAME_PROCESS_VARIABLES = "processVariables";
    public static final String PROPERTY_NAME_TRANSIENT_VARIABLES = "transientVariables";

    protected Map<String,Object> configurationMap;

    public AsyncContinuationEventHandler(String configuration) {
        this.configurationMap = readConfiguration(configuration);
    }

    public static String createConfiguration(Map<String,Object> processVariables, Map<String,Object> transientVariables) {
        ObjectMapper objectMapper = CommandContextUtil.getProcessEngineConfiguration().getObjectMapper();

        Map<String,Object> configurationMap = new HashMap<>();

        if (null != processVariables) {
            configurationMap.put(PROPERTY_NAME_PROCESS_VARIABLES, processVariables);
        }

        if (null != transientVariables) {
            configurationMap.put(PROPERTY_NAME_TRANSIENT_VARIABLES, transientVariables);
        }

        try {
            return objectMapper.writeValueAsString(configurationMap);
        } catch (JsonProcessingException e) {
            throw new FlowableException("Error writing json configuration value for " + configurationMap, e);
        }
    }

    public static Map<String,Object> readConfiguration(String configuration) {
        if(null != configuration) {
            ObjectMapper objectMapper = CommandContextUtil.getProcessEngineConfiguration().getObjectMapper();
            try {
                return objectMapper.readValue(configuration, new TypeReference<Map<String,Object>>() {});
            } catch (IOException e) {
                throw new FlowableException("Error reading json configuration value for " + configuration, e);
            }
        }
        return Collections.emptyMap();
    }
}
