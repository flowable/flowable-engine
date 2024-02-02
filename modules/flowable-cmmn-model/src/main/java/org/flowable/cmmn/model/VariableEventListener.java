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
package org.flowable.cmmn.model;

public class VariableEventListener extends EventListener {
    
    public static final String CHANGE_TYPE_ALL = "all";
    public static final String CHANGE_TYPE_UPDATE = "update";
    public static final String CHANGE_TYPE_CREATE = "create";
    public static final String CHANGE_TYPE_UPDATE_CREATE = "update-create";
    public static final String CHANGE_TYPE_DELETE = "delete";
 
    protected String variableName;
    protected String variableChangeType;
    
    public String getVariableName() {
        return variableName;
    }
    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }
    public String getVariableChangeType() {
        return variableChangeType;
    }
    public void setVariableChangeType(String variableChangeType) {
        this.variableChangeType = variableChangeType;
    }
}
