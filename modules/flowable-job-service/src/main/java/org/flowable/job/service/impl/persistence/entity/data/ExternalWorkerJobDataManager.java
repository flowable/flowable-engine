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
package org.flowable.job.service.impl.persistence.entity.data;

import java.util.List;

import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.job.api.ExternalWorkerJob;
import org.flowable.job.service.impl.ExternalWorkerJobAcquireBuilderImpl;
import org.flowable.job.service.impl.ExternalWorkerJobQueryImpl;
import org.flowable.job.service.impl.persistence.entity.ExternalWorkerJobEntity;

/**
 * @author Filip Hrisafov
 */
public interface ExternalWorkerJobDataManager extends DataManager<ExternalWorkerJobEntity>, JobInfoDataManager<ExternalWorkerJobEntity> {

    ExternalWorkerJobEntity findJobByCorrelationId(String correlationId);

    List<ExternalWorkerJob> findJobsByQueryCriteria(ExternalWorkerJobQueryImpl jobQuery);

    long findJobCountByQueryCriteria(ExternalWorkerJobQueryImpl jobQuery);

    void deleteJobsByExecutionId(String executionId);

    List<ExternalWorkerJobEntity> findExternalJobsToExecute(ExternalWorkerJobAcquireBuilderImpl builder, int numberOfJobs);

    List<ExternalWorkerJobEntity> findJobsByScopeIdAndSubScopeId(String scopeId, String subScopeId);
}
