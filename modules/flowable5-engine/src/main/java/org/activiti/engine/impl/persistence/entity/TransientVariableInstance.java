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
package org.activiti.engine.impl.persistence.entity;

import org.flowable.variable.api.delegate.VariableScope;
import org.flowable.variable.api.persistence.entity.VariableInstance;

/**
 * A dummy implementation of {@link VariableInstance}, used for storing transient variables on a {@link VariableScope}, as the {@link VariableScope} works with instances of {@link VariableInstance}
 * and not with raw key/values.
 * 
 * Nothing more than a thin wrapper around a name and value. All the other methods are not implemented.
 * 
 * @author Joram Barrez
 */
public class TransientVariableInstance implements VariableInstance {

    public static String TYPE_TRANSIENT = "transient";

    protected String variableName;
    protected Object variableValue;

    public TransientVariableInstance(String variableName, Object variableValue) {
        this.variableName = variableName;
        this.variableValue = variableValue;
    }

    @Override
    public String getName() {
        return variableName;
    }

    @Override
    public String getTextValue() {
        return null;
    }

    @Override
    public void setTextValue(String textValue) {

    }

    @Override
    public String getTextValue2() {
        return null;
    }

    @Override
    public void setTextValue2(String textValue2) {

    }

    @Override
    public Long getLongValue() {
        return null;
    }

    @Override
    public void setLongValue(Long longValue) {

    }

    @Override
    public Double getDoubleValue() {
        return null;
    }

    @Override
    public void setDoubleValue(Double doubleValue) {

    }

    @Override
    public byte[] getBytes() {
        return null;
    }

    @Override
    public void setBytes(byte[] bytes) {

    }

    @Override
    public Object getCachedValue() {
        return null;
    }

    @Override
    public void setCachedValue(Object cachedValue) {

    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public void setId(String id) {

    }

    @Override
    public void setName(String name) {

    }

    @Override
    public void setProcessInstanceId(String processInstanceId) {

    }
    
    @Override
    public void setProcessDefinitionId(String processDefinitionId) {

    }

    @Override
    public void setExecutionId(String executionId) {

    }

    @Override
    public Object getValue() {
        return variableValue;
    }

    @Override
    public void setValue(Object value) {
        variableValue = value;
    }

    @Override
    public String getTypeName() {
        return TYPE_TRANSIENT;
    }

    @Override
    public void setTypeName(String typeName) {

    }

    @Override
    public String getProcessInstanceId() {
        return null;
    }
    
    @Override
    public String getProcessDefinitionId() {
        return null;
    }

    @Override
    public String getTaskId() {
        return null;
    }

    @Override
    public void setTaskId(String taskId) {

    }

    @Override
    public String getExecutionId() {
        return null;
    }

    // non-supported (v6)

    @Override
    public String getScopeId() {
        return null;
    }
    
    @Override
    public String getSubScopeId() {
        return null;
    }

    @Override
    public String getScopeType() {
        return null;
    }

    @Override
    public void setScopeId(String scopeId) {
        
    }
    
    @Override
    public void setSubScopeId(String subScopeId) {
        
    }

    @Override
    public void setScopeType(String scopeType) {
        
    }

    @Override
    public String getScopeDefinitionId() {
        return null;
    }

    @Override
    public void setScopeDefinitionId(String scopeDefinitionId) {
        
    }

    @Override
    public String getMetaInfo() {
        return null;
    }

    @Override
    public void setMetaInfo(String metaInfo) {

    }
}
