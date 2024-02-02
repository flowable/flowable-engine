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
package org.flowable.variable.service.impl.persistence.entity;

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

    public static final String TYPE_TRANSIENT = "transient";

    protected String variableName;
    protected Object variableValue;

    protected String taskId;

    protected String executionId;
    protected String processInstanceId;
    protected String processDefinitionId;

    protected String scopeId;
    protected String subScopeId;
    protected String scopeDefinitionId;
    protected String scopeType;

    protected String metaInfo;

    public TransientVariableInstance(String variableName, Object variableValue) {
        this.variableName = variableName;
        this.variableValue = variableValue;
    }

    @Override
    public String getName() {
        return variableName;
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
        this.processInstanceId = processInstanceId;
    }
    
    @Override
    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    @Override
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
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
        // Not relevant
    }

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    @Override
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    @Override
    public String getTaskId() {
        return taskId;
    }

    @Override
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    @Override
    public String getExecutionId() {
        return executionId;
    }

    @Override
    public String getScopeId() {
        return scopeId;
    }

    @Override
    public String getScopeType() {
        return scopeType;
    }

    @Override
    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }

    @Override
    public String getSubScopeId() {
        return subScopeId;
    }

    @Override
    public void setSubScopeId(String subScopeId) {
        this.subScopeId = subScopeId;
    }

    @Override
    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    @Override
    public String getScopeDefinitionId() {
        return scopeDefinitionId;
    }

    @Override
    public void setScopeDefinitionId(String scopeDefinitionId) {
        this.scopeDefinitionId = scopeDefinitionId;
    }

    @Override
    public String getMetaInfo() {
        return metaInfo;
    }

    @Override
    public void setMetaInfo(String metaInfo) {
        this.metaInfo = metaInfo;
    }

    // The methods below are not relevant, as getValue() is used directly to return the value set during the transaction

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

}
