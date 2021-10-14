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
package org.flowable.batch.service.impl.persistence.entity;

import java.util.Date;

import org.flowable.batch.api.BatchPart;
import org.flowable.common.engine.impl.db.HasRevision;
import org.flowable.common.engine.impl.persistence.entity.ByteArrayRef;
import org.flowable.common.engine.impl.persistence.entity.Entity;

public interface BatchPartEntity extends BatchPart, Entity, HasRevision {

    void setType(String type);

    void setBatchType(String batchType);

    void setBatchId(String batchId);

    void setCreateTime(Date createTime);

    void setCompleteTime(Date completeTime);

    @Override
    boolean isCompleted();

    void setSearchKey(String searchKey);

    void setSearchKey2(String searchKey);

    void setBatchSearchKey(String searchKey);

    void setBatchSearchKey2(String searchKey);
    
    void setStatus(String status);

    void setScopeId(String scopeId);
    
    void setScopeType(String scopeType);
    
    void setSubScopeId(String subScopeId);

    ByteArrayRef getResultDocRefId();

    void setResultDocumentJson(String resultDocumentJson, String engineType);
    
    void setTenantId(String tenantId);
}

