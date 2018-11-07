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

package org.flowable.rest.service.api.history;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModelProperty;

import org.flowable.common.rest.util.DateToStringSerializer;
import org.flowable.rest.service.api.engine.variable.RestVariable;

import java.util.Date;

/**
 * @author Tijs Rademakers
 */
public class HistoricDetailResponse {

    public static String FORM_PROPERTY = "formProperty";
    public static String VARIABLE_UPDATE = "variableUpdate";

    protected String id;
    protected String processInstanceId;
    protected String processInstanceUrl;
    protected String executionId;
    protected String activityInstanceId;
    protected String taskId;
    protected String taskUrl;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date time;
    protected String detailType;

    // Historic variable update properties
    protected Integer revision;
    protected RestVariable variable;

    // Form properties
    protected String propertyId;
    protected String propertyValue;


    @ApiModelProperty(example = "26")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ApiModelProperty(example = "5")
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    @ApiModelProperty(example = "http://localhost:8182/history/historic-process-instances/5")
    public String getProcessInstanceUrl() {
        return processInstanceUrl;
    }

    public void setProcessInstanceUrl(String processInstanceUrl) {
        this.processInstanceUrl = processInstanceUrl;
    }

    @ApiModelProperty(example = "6")
    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    @ApiModelProperty(example = "10")
    public String getActivityInstanceId() {
        return activityInstanceId;
    }

    public void setActivityInstanceId(String activityInstanceId) {
        this.activityInstanceId = activityInstanceId;
    }

    @ApiModelProperty(example = "6")
    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    @ApiModelProperty(example = "http://localhost:8182/history/historic-task-instances/6")
    public String getTaskUrl() {
        return taskUrl;
    }

    public void setTaskUrl(String taskUrl) {
        this.taskUrl = taskUrl;
    }

    @ApiModelProperty(example = "2013-04-17T10:17:43.902+0000")
    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    @ApiModelProperty(example = "variableUpdate")
    public String getDetailType() {
        return detailType;
    }

    public void setDetailType(String detailType) {
        this.detailType = detailType;
    }

    @ApiModelProperty(example = "2")
    public Integer getRevision() {
        return revision;
    }

    public void setRevision(Integer revision) {
        this.revision = revision;
    }

    public RestVariable getVariable() {
        return variable;
    }

    public void setVariable(RestVariable variable) {
        this.variable = variable;
    }

    @ApiModelProperty(example = "null")
    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    @ApiModelProperty(example = "null")
    public String getPropertyValue() {
        return propertyValue;
    }

    public void setPropertyValue(String propertyValue) {
        this.propertyValue = propertyValue;
    }
}
