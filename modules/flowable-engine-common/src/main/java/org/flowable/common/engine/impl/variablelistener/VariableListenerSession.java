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
package org.flowable.common.engine.impl.variablelistener;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.Session;

public class VariableListenerSession implements Session {
    
    protected CommandContext commandContext;
    
    protected Map<String, List<VariableListenerSessionData>> variableData;

    public void addVariableData(String variableName, String changeType, String scopeId, String scopeType, String scopeDefinitionId) {
        if (variableData == null) {
            variableData = new LinkedHashMap<>();
        }
        
        if (!variableData.containsKey(variableName)) {
            variableData.put(variableName, new ArrayList<>());
        }
        
        VariableListenerSessionData sessionData = new VariableListenerSessionData(changeType, scopeId, scopeType, scopeDefinitionId);
        variableData.get(variableName).add(sessionData);
    }
    
    @Override
    public void flush() {

    }

    @Override
    public void close() {

    }

    public Map<String, List<VariableListenerSessionData>> getVariableData() {
        return variableData;
    }

    public void setVariableData(Map<String, List<VariableListenerSessionData>> variableData) {
        this.variableData = variableData;
    }
}
