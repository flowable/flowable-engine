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

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.impl.db.BulkDeleteable;
import org.activiti.engine.impl.db.HasRevision;
import org.activiti.engine.impl.db.PersistentObject;
import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.job.api.Job;

/**
 * Abstract job entity class.
 *
 * @author Tijs Rademakers
 */
public abstract class AbstractJobEntity implements Job, PersistentObject, HasRevision, BulkDeleteable, Serializable {

    public static final boolean DEFAULT_EXCLUSIVE = true;
    public static final int DEFAULT_RETRIES = 3;
    private static final int MAX_EXCEPTION_MESSAGE_LENGTH = 255;

    private static final long serialVersionUID = 1L;

    protected String id;
    protected int revision;

    protected Date duedate;
    protected Date createTime;

    protected String executionId;
    protected String processInstanceId;
    protected String processDefinitionId;
    
    protected String scopeId;
    protected String subScopeId;
    protected String scopeType;
    protected String scopeDefinitionId;

    protected boolean isExclusive = DEFAULT_EXCLUSIVE;

    protected int retries = DEFAULT_RETRIES;

    protected String jobHandlerType;
    protected String jobHandlerConfiguration;
    protected int maxIterations;
    protected String repeat;
    protected Date endDate;

    protected final ByteArrayRef exceptionByteArrayRef = new ByteArrayRef();
    protected final ByteArrayRef customValuesByteArrayRef = new ByteArrayRef();

    protected String exceptionMessage;

    protected String tenantId = ProcessEngineConfiguration.NO_TENANT_ID;
    protected String jobType;

    public void setExecution(ExecutionEntity execution) {
        executionId = execution.getId();
        processInstanceId = execution.getProcessInstanceId();
        processDefinitionId = execution.getProcessDefinitionId();
    }

    public String getExceptionStacktrace() {
        byte[] bytes = exceptionByteArrayRef.getBytes();
        if (bytes == null) {
            return null;
        }
        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new ActivitiException("UTF-8 is not a supported encoding");
        }
    }

    public void setExceptionStacktrace(String exception) {
        exceptionByteArrayRef.setValue("stacktrace", getUtf8Bytes(exception));
    }

    @Override
    public String getCustomValues() {
        byte[] bytes = customValuesByteArrayRef.getBytes();
        if (bytes == null) {
            return null;
        }
        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new ActivitiException("UTF-8 is not a supported encoding");
        }
    }

    public void setCustomValues(String customValues) {
        customValuesByteArrayRef.setValue("jobCustomValues", getUtf8Bytes(customValues));
    }

    private byte[] getUtf8Bytes(String str) {
        if (str == null) {
            return null;
        }
        try {
            return str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new ActivitiException("UTF-8 is not a supported encoding");
        }
    }

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("retries", retries);
        persistentState.put("duedate", duedate);
        persistentState.put("exceptionMessage", exceptionMessage);
        persistentState.put("exceptionByteArrayRef", exceptionByteArrayRef.getId());
        persistentState.put("customValuesByteArrayRef", customValuesByteArrayRef.getId());
        return persistentState;
    }

    @Override
    public int getRevisionNext() {
        return revision + 1;
    }

    // getters and setters //////////////////////////////////////////////////////

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public int getRevision() {
        return revision;
    }

    @Override
    public void setRevision(int revision) {
        this.revision = revision;
    }

    @Override
    public Date getDuedate() {
        return duedate;
    }

    public void setDuedate(Date duedate) {
        this.duedate = duedate;
    }

    @Override
    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    @Override
    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    @Override
    public boolean isExclusive() {
        return isExclusive;
    }

    public void setExclusive(boolean isExclusive) {
        this.isExclusive = isExclusive;
    }

    @Override
    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }
    
    @Override
    public String getScopeId() {
        return scopeId;
    }

    public void setScopeId(String scopeId) {
        this.scopeId = scopeId;
    }

    @Override
    public String getSubScopeId() {
        return subScopeId;
    }

    public void setSubScopeId(String subScopeId) {
        this.subScopeId = subScopeId;
    }

    @Override
    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    @Override
    public String getScopeDefinitionId() {
        return scopeDefinitionId;
    }

    public void setScopeDefinitionId(String scopeDefinitionId) {
        this.scopeDefinitionId = scopeDefinitionId;
    }

    @Override
    public String getJobHandlerType() {
        return jobHandlerType;
    }

    public void setJobHandlerType(String jobHandlerType) {
        this.jobHandlerType = jobHandlerType;
    }

    @Override
    public String getJobHandlerConfiguration() {
        return jobHandlerConfiguration;
    }

    public void setJobHandlerConfiguration(String jobHandlerConfiguration) {
        this.jobHandlerConfiguration = jobHandlerConfiguration;
    }

    public String getRepeat() {
        return repeat;
    }

    public void setRepeat(String repeat) {
        this.repeat = repeat;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    @Override
    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = StringUtils.abbreviate(exceptionMessage, MAX_EXCEPTION_MESSAGE_LENGTH);
    }

    @Override
    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
