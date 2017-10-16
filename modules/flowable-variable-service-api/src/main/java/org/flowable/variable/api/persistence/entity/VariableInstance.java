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
package org.flowable.variable.api.persistence.entity;

import org.flowable.variable.api.types.ValueFields;

/**
 * Generic variable class that can be reused for V6 and V5 engine.
 * 
 * @author Tijs Rademakers
 */
public interface VariableInstance extends ValueFields {

    String getId();
    
    void setId(String id);
    
    void setName(String name);
    
    void setExecutionId(String executionId);

    void setProcessInstanceId(String processInstanceId);
    
    void setProcessDefinitionId(String processDefinitionId);
    String getProcessDefinitionId();

    Object getValue();

    void setValue(Object value);

    String getTypeName();

    void setTypeName(String typeName);

    void setTaskId(String taskId);
    
    void setScopeId(String scopeId);
    
    void setSubScopeId(String subScopeId);
    
    void setScopeType(String scopeType);

}
