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
package org.flowable.cmmn.engine.interceptor;

import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.model.ExternalWorkerServiceTask;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntity;

public class CreateCmmnExternalWorkerJobAfterContext {

    protected final ExternalWorkerServiceTask externalWorkerServiceTask;
    protected final ExternalWorkerJobEntity externalWorkerJobEntity;
    protected final PlanItemInstanceEntity planItemInstance;

    public CreateCmmnExternalWorkerJobAfterContext(ExternalWorkerServiceTask externalWorkerServiceTask,
            ExternalWorkerJobEntity externalWorkerJobEntity, PlanItemInstanceEntity planItemInstance) {
        this.externalWorkerServiceTask = externalWorkerServiceTask;
        this.externalWorkerJobEntity = externalWorkerJobEntity;
        this.planItemInstance = planItemInstance;
    }

    public ExternalWorkerServiceTask getExternalWorkerServiceTask() {
        return externalWorkerServiceTask;
    }

    public ExternalWorkerJobEntity getExternalWorkerJobEntity() {
        return externalWorkerJobEntity;
    }

    public PlanItemInstanceEntity getPlanItemInstance() {
        return planItemInstance;
    }
}
