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

package org.flowable.cmmn.rest.service.api.management;

import java.util.Date;

import org.flowable.common.rest.util.DateToStringSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author Joram Barrez
 */
public class HistoryJobResponse {

    protected String id;
    protected String url;
    protected String scopeType;
    protected Integer retries;
    protected String exceptionMessage;
    protected String jobHandlerType;
    protected String jobHandlerConfiguration;
    protected String advancedJobHandlerConfiguration;
    protected String tenantId;
    protected String customValues;
    @JsonSerialize(using = DateToStringSerializer.class, as = Date.class)
    protected Date createTime;

    @ApiModelProperty(example = "8")
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @ApiModelProperty(example = "http://localhost:8182/management/jobs/8")
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @ApiModelProperty(example = "3")
    public Integer getRetries() {
        return retries;
    }

    public void setRetries(Integer retries) {
        this.retries = retries;
    }

    @ApiModelProperty(example = "null")
    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    @ApiModelProperty(example = "2013-06-03T22:05:05.474+0000")
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @ApiModelProperty(example = "null")
    public String getTenantId() {
        return tenantId;
    }

    @ApiModelProperty(example = "cmmn")
    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    @ApiModelProperty(example = "async-history")
    public String getJobHandlerType() {
        return jobHandlerType;
    }

    public void setJobHandlerType(String jobHandlerType) {
        this.jobHandlerType = jobHandlerType;
    }

    @ApiModelProperty(example = "myCfg")
    public String getJobHandlerConfiguration() {
        return jobHandlerConfiguration;
    }

    public void setJobHandlerConfiguration(String jobHandlerConfiguration) {
        this.jobHandlerConfiguration = jobHandlerConfiguration;
    }

    @ApiModelProperty(example = "myAdvancedCfg")
    public String getAdvancedJobHandlerConfiguration() {
        return advancedJobHandlerConfiguration;
    }

    public void setAdvancedJobHandlerConfiguration(String advancedJobHandlerConfiguration) {
        this.advancedJobHandlerConfiguration = advancedJobHandlerConfiguration;
    }

    @ApiModelProperty(example = "custom value")
    public String getCustomValues() {
        return customValues;
    }

    public void setCustomValues(String customValues) {
        this.customValues = customValues;
    }
}
