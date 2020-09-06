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

import java.util.Date;

import org.flowable.common.engine.impl.db.HasRevision;
import org.flowable.common.engine.impl.persistence.entity.ByteArrayRef;
import org.flowable.common.engine.impl.persistence.entity.Entity;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public interface AbstractJobEntity extends Entity, HasRevision {

    /**
     * Set the scope type for the job.
     * The scope type is the type which is used by the job executor to pick
     * the jobs for executing.
     * <p>
     * For example if the job should be picked up by the CMMN Job executor then it
     * should have the same type as the CMMN job executor.
     * @param scopeType the scope type for the job
     */
    void setScopeType(String scopeType);

    String getScopeType();

    int getRetries();

    void setRetries(int retries);

    void setJobHandlerType(String jobHandlerType);

    String getJobHandlerType();

    String getJobHandlerConfiguration();

    void setJobHandlerConfiguration(String jobHandlerConfiguration);

    String getCustomValues();

    void setCustomValues(String customValues);

    ByteArrayRef getCustomValuesByteArrayRef();

    void setCustomValuesByteArrayRef(ByteArrayRef customValuesByteArrayRef);

    String getExceptionStacktrace();

    void setExceptionStacktrace(String exception);

    String getExceptionMessage();

    void setExceptionMessage(String exceptionMessage);

    ByteArrayRef getExceptionByteArrayRef();

    void setExceptionByteArrayRef(ByteArrayRef exceptionByteArrayRef);

    String getTenantId();

    void setTenantId(String tenantId);

    Date getCreateTime();

    void setCreateTime(Date createTime);

}
