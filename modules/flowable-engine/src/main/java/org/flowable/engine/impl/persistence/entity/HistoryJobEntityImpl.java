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
package org.flowable.engine.impl.persistence.entity;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.impl.persistence.entity.AbstractEntity;
import org.flowable.engine.runtime.JobInfo;

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
    protected ByteArrayRef advancedJobHandlerConfigurationByteArrayRef;

    protected ByteArrayRef exceptionByteArrayRef;
    protected String exceptionMessage;

    protected String tenantId = ProcessEngineConfiguration.NO_TENANT_ID;

    protected String lockOwner;
    protected Date lockExpirationTime;
    protected Date createTime;

    public Object getPersistentState() {
        Map<String, Object> persistentState = new HashMap<>();
        persistentState.put("retries", retries);
        persistentState.put("exceptionMessage", exceptionMessage);
        persistentState.put("jobHandlerType", jobHandlerType);

        if (exceptionByteArrayRef != null) {
            persistentState.put("exceptionByteArrayId", exceptionByteArrayRef.getId());
        }

        if (advancedJobHandlerConfigurationByteArrayRef != null) {
            persistentState.put("advancedJobHandlerConfigurationByteArrayRef",
                    advancedJobHandlerConfigurationByteArrayRef.getId());
        }
        persistentState.put("lockOwner", lockOwner);
        persistentState.put("lockExpirationTime", lockExpirationTime);

        return persistentState;
    }

    // getters and setters
    // ////////////////////////////////////////////////////////

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public String getJobHandlerType() {
        return jobHandlerType;
    }

    public void setJobHandlerType(String jobHandlerType) {
        this.jobHandlerType = jobHandlerType;
    }

    public String getJobHandlerConfiguration() {
        return jobHandlerConfiguration;
    }

    public void setJobHandlerConfiguration(String jobHandlerConfiguration) {
        this.jobHandlerConfiguration = jobHandlerConfiguration;
    }

    public ByteArrayRef getAdvancedJobHandlerConfigurationByteArrayRef() {
        return advancedJobHandlerConfigurationByteArrayRef;
    }

    public String getAdvancedJobHandlerConfiguration() {
        if (advancedJobHandlerConfigurationByteArrayRef == null) {
            return null;
        }

        byte[] bytes = advancedJobHandlerConfigurationByteArrayRef.getBytes();
        if (bytes == null) {
            return null;
        }

        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new FlowableException("UTF-8 is not a supported encoding");
        }
    }
    
    @Override
    public void setAdvancedJobHandlerConfigurationByteArrayRef(ByteArrayRef configurationByteArrayRef) {
         this.advancedJobHandlerConfigurationByteArrayRef = configurationByteArrayRef;
    }

    public void setAdvancedJobHandlerConfiguration(String jobHandlerConfiguration) {
        if (advancedJobHandlerConfigurationByteArrayRef == null) {
            advancedJobHandlerConfigurationByteArrayRef = new ByteArrayRef();
        }
        advancedJobHandlerConfigurationByteArrayRef.setValue("cfg", getUtf8Bytes(jobHandlerConfiguration));
    }

    @Override
    public void setAdvancedJobHandlerConfigurationBytes(byte[] bytes) {
        if (advancedJobHandlerConfigurationByteArrayRef == null) {
            advancedJobHandlerConfigurationByteArrayRef = new ByteArrayRef();
        }
        advancedJobHandlerConfigurationByteArrayRef.setValue("cfg", bytes);
    }
    
    @Override
    public void setExceptionByteArrayRef(ByteArrayRef exceptionByteArrayRef) {
        this.exceptionByteArrayRef = exceptionByteArrayRef;
    }

    public ByteArrayRef getExceptionByteArrayRef() {
        return exceptionByteArrayRef;
    }

    public String getExceptionStacktrace() {
        if (exceptionByteArrayRef == null) {
            return null;
        }

        byte[] bytes = exceptionByteArrayRef.getBytes();
        if (bytes == null) {
            return null;
        }

        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new FlowableException("UTF-8 is not a supported encoding");
        }
    }

    public void setExceptionStacktrace(String exception) {
        if (exceptionByteArrayRef == null) {
            exceptionByteArrayRef = new ByteArrayRef();
        }
        exceptionByteArrayRef.setValue("stacktrace", getUtf8Bytes(exception));
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = StringUtils.abbreviate(exceptionMessage, JobInfo.MAX_EXCEPTION_MESSAGE_LENGTH);
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getLockOwner() {
        return lockOwner;
    }

    public void setLockOwner(String claimedBy) {
        this.lockOwner = claimedBy;
    }

    public Date getLockExpirationTime() {
        return lockExpirationTime;
    }

    public void setLockExpirationTime(Date claimedUntil) {
        this.lockExpirationTime = claimedUntil;
    }

    protected byte[] getUtf8Bytes(String str) {
        if (str == null) {
            return null;
        }
        try {
            return str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new FlowableException("UTF-8 is not a supported encoding");
        }
    }

    @Override
    public String toString() {
        return "HistoryJobEntity [id=" + id + "]";
    }

}
