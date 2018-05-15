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
package org.flowable.job.service.impl.persistence.entity;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.impl.persistence.entity.AbstractEntity;
import org.flowable.job.api.JobInfo;
import org.flowable.job.service.JobServiceConfiguration;

/**
 * Abstract job entity class.
 *
 * @author Tijs Rademakers
 */
public abstract class AbstractJobEntityImpl extends AbstractEntity implements AbstractRuntimeJobEntity, Serializable {

    private static final long serialVersionUID = 1L;

    protected Date createTime;
    protected Date duedate;

    protected String executionId;
    protected String processInstanceId;
    protected String processDefinitionId;
    
    protected String scopeId;
    protected String subScopeId;
    protected String scopeType;
    protected String scopeDefinitionId;

    protected boolean isExclusive = DEFAULT_EXCLUSIVE;

    protected int retries;

    protected int maxIterations;
    protected String repeat;
    protected Date endDate;

    protected String jobHandlerType;
    protected String jobHandlerConfiguration;
    protected JobByteArrayRef customValuesByteArrayRef;

    protected JobByteArrayRef exceptionByteArrayRef;
    protected String exceptionMessage;

    protected String tenantId = JobServiceConfiguration.NO_TENANT_ID;
    protected String jobType;

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("retries", retries);
        persistentState.put("createTime", createTime);
        persistentState.put("duedate", duedate);
        persistentState.put("exceptionMessage", exceptionMessage);
        persistentState.put("jobHandlerType", jobHandlerType);
        persistentState.put("processDefinitionId", processDefinitionId);

        if (customValuesByteArrayRef != null) {
            persistentState.put("customValuesByteArrayRef", customValuesByteArrayRef);
        }

        if (exceptionByteArrayRef != null) {
            persistentState.put("exceptionByteArrayRef", exceptionByteArrayRef);
        }

        return persistentState;
    }

    // getters and setters ////////////////////////////////////////////////////////

    @Override
    public Date getCreateTime() {
        return createTime;
    }

    @Override
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public Date getDuedate() {
        return duedate;
    }

    @Override
    public void setDuedate(Date duedate) {
        this.duedate = duedate;
    }

    @Override
    public String getExecutionId() {
        return executionId;
    }

    @Override
    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    @Override
    public int getRetries() {
        return retries;
    }

    @Override
    public void setRetries(int retries) {
        this.retries = retries;
    }

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    @Override
    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    @Override
    public boolean isExclusive() {
        return isExclusive;
    }

    @Override
    public void setExclusive(boolean isExclusive) {
        this.isExclusive = isExclusive;
    }

    @Override
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    @Override
    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }
    
    @Override
    public String getScopeId() {
        return scopeId;
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
    public String getScopeType() {
        return scopeType;
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
    public String getRepeat() {
        return repeat;
    }

    @Override
    public void setRepeat(String repeat) {
        this.repeat = repeat;
    }

    @Override
    public Date getEndDate() {
        return endDate;
    }

    @Override
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    @Override
    public int getMaxIterations() {
        return maxIterations;
    }

    @Override
    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    @Override
    public String getJobHandlerType() {
        return jobHandlerType;
    }

    @Override
    public void setJobHandlerType(String jobHandlerType) {
        this.jobHandlerType = jobHandlerType;
    }

    @Override
    public String getJobHandlerConfiguration() {
        return jobHandlerConfiguration;
    }

    @Override
    public void setJobHandlerConfiguration(String jobHandlerConfiguration) {
        this.jobHandlerConfiguration = jobHandlerConfiguration;
    }

    @Override
    public JobByteArrayRef getCustomValuesByteArrayRef() {
        return customValuesByteArrayRef;
    }

    @Override
    public String getCustomValues() {
        return getJobByteArrayRefAsString(customValuesByteArrayRef);
    }

    @Override
    public void setCustomValues(String customValues) {
        if(customValuesByteArrayRef == null) {
            customValuesByteArrayRef = new JobByteArrayRef();
        }
        customValuesByteArrayRef.setValue("jobCustomValues", customValues);
    }

    @Override
    public String getJobType() {
        return jobType;
    }

    @Override
    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public String getExceptionStacktrace() {
        return getJobByteArrayRefAsString(exceptionByteArrayRef);
    }

    @Override
    public void setExceptionStacktrace(String exception) {
        if (exceptionByteArrayRef == null) {
            exceptionByteArrayRef = new JobByteArrayRef();
        }
        exceptionByteArrayRef.setValue("stacktrace", exception);
    }

    @Override
    public String getExceptionMessage() {
        return exceptionMessage;
    }

    @Override
    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = StringUtils.abbreviate(exceptionMessage, JobInfo.MAX_EXCEPTION_MESSAGE_LENGTH);
    }

    @Override
    public JobByteArrayRef getExceptionByteArrayRef() {
        return exceptionByteArrayRef;
    }

    private String getJobByteArrayRefAsString(JobByteArrayRef jobByteArrayRef) {
        if (jobByteArrayRef == null) {
            return null;
        }
        return jobByteArrayRef.asString();
    }

    @Override
    public String toString() {
        return getClass().getName() + " [id=" + id + "]";
    }

}
