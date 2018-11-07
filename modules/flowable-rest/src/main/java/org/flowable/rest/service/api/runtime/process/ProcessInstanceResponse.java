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

package org.flowable.rest.service.api.runtime.process;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.flowable.common.rest.util.DateToStringSerializer;
import org.flowable.rest.service.api.engine.variable.RestVariable;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModelProperty;

/**
 * Modified to add a "completed" flag, which lets the caller know if the process instance has run to completion without encountering a wait state or experiencing an error/ exception.
 * 
 * @author Frederik Heremans
 * @author Ryan Johnston (@rjfsu)
 */
public class ProcessInstanceResponse {
    
    protected String id;
    protected String url;
    protected String name;
    protected String businessKey;
    protected boolean suspended;
    protected boolean ended;
    protected String processDefinitionId;
    protected String processDefinitionUrl;
    protected String processDefinitionName;
    protected String processDefinitionDescription;
    protected String activityId;
    protected String startUserId;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date startTime;
    protected List<RestVariable> variables = new ArrayList<>();
    protected String callbackId;
    protected String callbackType;
    protected String tenantId;

    // Added by Ryan Johnston
    protected boolean completed;

    @ApiModelProperty(example = "187")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ApiModelProperty(example = "http://localhost:8182/repository/process-definitions/processOne%3A1%3A4")
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
    
    @ApiModelProperty(example = "myProcessInstanceName")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(example = "myBusinessKey")
    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    public boolean isEnded() {
        return ended;
    }

    public void setEnded(boolean ended) {
        this.ended = ended;
    }

    @ApiModelProperty(example = "oneTaskProcess:1:158")
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    @ApiModelProperty(example = "http://localhost:8182/repository/process-definitions/processOne%3A1%3A4")
    public String getProcessDefinitionUrl() {
        return processDefinitionUrl;
    }

    public void setProcessDefinitionUrl(String processDefinitionUrl) {
        this.processDefinitionUrl = processDefinitionUrl;
    }
    
    @ApiModelProperty(example = "A process definition name")
    public String getProcessDefinitionName() {
        return processDefinitionName;
    }

    public void setProcessDefinitionName(String processDefinitionName) {
        this.processDefinitionName = processDefinitionName;
    }

    @ApiModelProperty(example = "A process definition description")
    public String getProcessDefinitionDescription() {
        return processDefinitionDescription;
    }

    public void setProcessDefinitionDescription(String processDefinitionDescription) {
        this.processDefinitionDescription = processDefinitionDescription;
    }

    @ApiModelProperty(example = "processTask")
    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }
    
    @ApiModelProperty(example = "johnDoe")
    public String getStartUserId() {
        return startUserId;
    }

    public void setStartUserId(String startUserId) {
        this.startUserId = startUserId;
    }

    @ApiModelProperty(example = "2018-04-17T10:17:43.902+0000", dataType = "string")
    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public List<RestVariable> getVariables() {
        return variables;
    }

    public void setVariables(List<RestVariable> variables) {
        this.variables = variables;
    }

    public void addVariable(RestVariable variable) {
        variables.add(variable);
    }
    
    @ApiModelProperty(example = "3")
    public String getCallbackId() {
        return callbackId;
    }

    public void setCallbackId(String callbackId) {
        this.callbackId = callbackId;
    }

    @ApiModelProperty(example = "cmmn")
    public String getCallbackType() {
        return callbackType;
    }

    public void setCallbackType(String callbackType) {
        this.callbackType = callbackType;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @ApiModelProperty(example = "someTenantId")
    public String getTenantId() {
        return tenantId;
    }

    // Added by Ryan Johnston
    public boolean isCompleted() {
        return completed;
    }

    // Added by Ryan Johnston
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
