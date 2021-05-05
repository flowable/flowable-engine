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

public class VariableListenerSessionData {
    
    public static final String VARIABLE_CREATE = "create";
    public static final String VARIABLE_UPDATE = "update";
    public static final String VARIABLE_DELETE = "delete";

    protected String changeType;
    protected String scopeId;
    protected String scopeType;
    protected String scopeDefinitionId;
    
    public VariableListenerSessionData(String changeType, String scopeId, String scopeType, String scopeDefinitionId) {
        this.changeType = changeType;
        this.scopeId = scopeId;
        this.scopeType = scopeType;
        this.scopeDefinitionId = scopeDefinitionId;
    }
    
    public VariableListenerSessionData() {
        
    }
    
    public String getChangeType() {
        return changeType;
    }
    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }
    public String getScopeId() {
        return scopeId;
    }
    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }
    public String getScopeType() {
        return scopeType;
    }
    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }
    public String getScopeDefinitionId() {
        return scopeDefinitionId;
    }
    public void setScopeDefinitionId(String scopeDefinitionId) {
        this.scopeDefinitionId = scopeDefinitionId;
    }     
}
