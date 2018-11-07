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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.flowable.common.rest.util.DateToStringSerializer;
import org.flowable.rest.service.api.engine.variable.RestVariable;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author Tijs Rademakers
 */
public class HistoricProcessInstanceResponse {

    protected String id;
    protected String url;
    protected String name;
    protected String businessKey;
    protected String processDefinitionId;
    protected String processDefinitionUrl;
    protected String processDefinitionName;
    protected String processDefinitionDescription;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date startTime;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date endTime;
    protected Long durationInMillis;
    protected String startUserId;
    protected String startActivityId;
    protected String endActivityId;
    protected String deleteReason;
    protected String superProcessInstanceId;
    protected List<RestVariable> variables = new ArrayList<>();
    protected String callbackId;
    protected String callbackType;
    protected String tenantId;

    @ApiModelProperty(example = "5")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ApiModelProperty(example = "http://localhost:8182/history/historic-process-instances/5")
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

    @ApiModelProperty(example = "myKey")
    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    @ApiModelProperty(example = "oneTaskProcess%3A1%3A4")
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    @ApiModelProperty(example = "http://localhost:8182/repository/process-definitions/oneTaskProcess%3A1%3A4")
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

    @ApiModelProperty(example = "2013-04-17T10:17:43.902+0000")
    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    @ApiModelProperty(example = "2013-04-18T14:06:32.715+0000")
    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    @ApiModelProperty(example = "86400056")
    public Long getDurationInMillis() {
        return durationInMillis;
    }

    public void setDurationInMillis(Long durationInMillis) {
        this.durationInMillis = durationInMillis;
    }

    @ApiModelProperty(example = "kermit")
    public String getStartUserId() {
        return startUserId;
    }

    public void setStartUserId(String startUserId) {
        this.startUserId = startUserId;
    }

    @ApiModelProperty(example = "startEvent")
    public String getStartActivityId() {
        return startActivityId;
    }

    public void setStartActivityId(String startActivityId) {
        this.startActivityId = startActivityId;
    }

    @ApiModelProperty(example = "endEvent")
    public String getEndActivityId() {
        return endActivityId;
    }

    public void setEndActivityId(String endActivityId) {
        this.endActivityId = endActivityId;
    }

    @ApiModelProperty(example = "null")
    public String getDeleteReason() {
        return deleteReason;
    }

    public void setDeleteReason(String deleteReason) {
        this.deleteReason = deleteReason;
    }

    @ApiModelProperty(example = "3")
    public String getSuperProcessInstanceId() {
        return superProcessInstanceId;
    }

    public void setSuperProcessInstanceId(String superProcessInstanceId) {
        this.superProcessInstanceId = superProcessInstanceId;
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

    @ApiModelProperty(example = "someTenantId")
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantId() {
        return tenantId;
    }
}
