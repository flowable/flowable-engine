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
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.persistence.entity.AbstractEntity;
import org.flowable.job.api.JobInfo;
import org.flowable.job.service.JobServiceConfiguration;

/**
 * History Job entity.
 *
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class HistoryJobEntityImpl extends AbstractEntity implements HistoryJobEntity, Serializable {

    private static final long serialVersionUID = 1L;

    protected int retries;

    protected String jobHandlerType;
    protected String jobHandlerConfiguration;
    protected JobByteArrayRef customValuesByteArrayRef;
    protected JobByteArrayRef advancedJobHandlerConfigurationByteArrayRef;

    protected JobByteArrayRef exceptionByteArrayRef;
    protected String exceptionMessage;

    protected String tenantId = JobServiceConfiguration.NO_TENANT_ID;

    protected String lockOwner;
    protected Date lockExpirationTime;
    protected Date createTime;

    @Override
    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("retries", retries);
        persistentState.put("exceptionMessage", exceptionMessage);
        persistentState.put("jobHandlerType", jobHandlerType);

        putByteArrayRefIdToMap("exceptionByteArrayId", exceptionByteArrayRef, persistentState);
        putByteArrayRefIdToMap("customValuesByteArrayRef", customValuesByteArrayRef, persistentState);
        putByteArrayRefIdToMap("advancedJobHandlerConfigurationByteArrayRef", advancedJobHandlerConfigurationByteArrayRef, persistentState);

        persistentState.put("lockOwner", lockOwner);
        persistentState.put("lockExpirationTime", lockExpirationTime);

        return persistentState;
    }

    private void putByteArrayRefIdToMap(String key, JobByteArrayRef jobByteArrayRef, Map<String, Object> map) {
        if(jobByteArrayRef != null) {
            map.put(key, jobByteArrayRef.getId());
        }
    }

    // getters and setters
    // ////////////////////////////////////////////////////////

    @Override
    public int getRetries() {
        return retries;
    }

    @Override
    public void setRetries(int retries) {
        this.retries = retries;
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
    public String getCustomValues() {
        return getJobByteArrayRefAsString(customValuesByteArrayRef);
    }

    @Override
    public void setCustomValues(String customValues) {
        if (customValuesByteArrayRef == null) {
            customValuesByteArrayRef = new JobByteArrayRef();
        }
        customValuesByteArrayRef.setValue("jobCustomValues", customValues);
    }

    @Override
    public JobByteArrayRef getCustomValuesByteArrayRef() {
        return customValuesByteArrayRef;
    }

    @Override
    public void setCustomValuesByteArrayRef(JobByteArrayRef customValuesByteArrayRef) {
        this.customValuesByteArrayRef = customValuesByteArrayRef;
    }

    @Override
    public JobByteArrayRef getAdvancedJobHandlerConfigurationByteArrayRef() {
        return advancedJobHandlerConfigurationByteArrayRef;
    }

    @Override
    public String getAdvancedJobHandlerConfiguration() {
        return getJobByteArrayRefAsString(advancedJobHandlerConfigurationByteArrayRef);
    }

    @Override
    public void setAdvancedJobHandlerConfigurationByteArrayRef(JobByteArrayRef configurationByteArrayRef) {
         this.advancedJobHandlerConfigurationByteArrayRef = configurationByteArrayRef;
    }

    @Override
    public void setAdvancedJobHandlerConfiguration(String jobHandlerConfiguration) {
        if (advancedJobHandlerConfigurationByteArrayRef == null) {
            advancedJobHandlerConfigurationByteArrayRef = new JobByteArrayRef();
        }
        advancedJobHandlerConfigurationByteArrayRef.setValue("cfg", jobHandlerConfiguration);
    }

    @Override
    public void setAdvancedJobHandlerConfigurationBytes(byte[] bytes) {
        if (advancedJobHandlerConfigurationByteArrayRef == null) {
            advancedJobHandlerConfigurationByteArrayRef = new JobByteArrayRef();
        }
        advancedJobHandlerConfigurationByteArrayRef.setValue("cfg", bytes);
    }

    @Override
    public void setExceptionByteArrayRef(JobByteArrayRef exceptionByteArrayRef) {
        this.exceptionByteArrayRef = exceptionByteArrayRef;
    }

    @Override
    public JobByteArrayRef getExceptionByteArrayRef() {
        return exceptionByteArrayRef;
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
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public Date getCreateTime() {
        return createTime;
    }

    @Override
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String getLockOwner() {
        return lockOwner;
    }

    @Override
    public void setLockOwner(String claimedBy) {
        this.lockOwner = claimedBy;
    }

    @Override
    public Date getLockExpirationTime() {
        return lockExpirationTime;
    }

    @Override
    public void setLockExpirationTime(Date claimedUntil) {
        this.lockExpirationTime = claimedUntil;
    }

    private String getJobByteArrayRefAsString(JobByteArrayRef jobByteArrayRef) {
        if (jobByteArrayRef == null) {
            return null;
        }
        return jobByteArrayRef.asString();
    }

    @Override
    public String toString() {
        return "HistoryJobEntity [id=" + id + "]";
    }

}
