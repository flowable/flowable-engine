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
package org.flowable.engine.impl;

import org.flowable.engine.runtime.DataObject;

public class DataObjectImpl implements DataObject {
    protected String id;
    protected String processInstanceId;
    protected String executionId;
    protected String name;
    protected Object value;
    protected String description;
    protected String localizedName;
    protected String localizedDescription;
    protected String dataObjectDefinitionKey;

    private String type;

    public DataObjectImpl(String id, String processInstanceId, String executionId, String name, Object value, String description, String type, String localizedName,
            String localizedDescription, String dataObjectDefinitionKey) {

        this.id = id;
        this.processInstanceId = processInstanceId;
        this.executionId = executionId;
        this.name = name;
        this.value = value;
        this.type = type;
        this.description = description;
        this.localizedName = localizedName;
        this.localizedDescription = localizedDescription;
        this.dataObjectDefinitionKey = dataObjectDefinitionKey;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    @Override
    public String getExecutionId() {
        return executionId;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getLocalizedName() {
        if (localizedName != null && localizedName.length() > 0) {
            return localizedName;
        } else {
            return name;
        }
    }

    public void setLocalizedName(String localizedName) {
        this.localizedName = localizedName;
    }

    @Override
    public String getDescription() {
        if (localizedDescription != null && localizedDescription.length() > 0) {
            return localizedDescription;
        } else {
            return description;
        }
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getDataObjectDefinitionKey() {
        return dataObjectDefinitionKey;
    }

    public void setDataObjectDefinitionKey(String dataObjectDefinitionKey) {
        this.dataObjectDefinitionKey = dataObjectDefinitionKey;
    }
}