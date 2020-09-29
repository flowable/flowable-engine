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

package org.flowable.rest.service.api.repository;

import java.util.Date;

import org.flowable.rest.service.api.RestActionRequest;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author Frederik Heremans
 */
public class ProcessDefinitionActionRequest extends RestActionRequest {

    public static final String ACTION_SUSPEND = "suspend";
    public static final String ACTION_ACTIVATE = "activate";

    private boolean includeProcessInstances;
    private Date date;
    private String category;

    public void setIncludeProcessInstances(boolean includeProcessInstances) {
        this.includeProcessInstances = includeProcessInstances;
    }

    @ApiModelProperty(value = "Whether or not to suspend/activate running process-instances for this process-definition. If omitted, the process-instances are left in the state they are")
    public boolean isIncludeProcessInstances() {
        return includeProcessInstances;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @ApiModelProperty(value = "Date (ISO-8601) when the suspension/activation should be executed. If omitted, the suspend/activation is effective immediately.")
    public Date getDate() {
        return date;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

    @Override
    @ApiModelProperty(value = "Action to perform: Either activate or suspend", example = "activate", required = true)
    public String getAction() {
        return super.getAction();
    }
}
