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
package org.flowable.external.job.rest.service.api;

import org.flowable.external.job.rest.service.api.acquire.AcquireExternalWorkerJobRequest;
import org.flowable.external.job.rest.service.api.acquire.ExternalWorkerJobCompleteRequest;
import org.flowable.external.job.rest.service.api.acquire.ExternalWorkerJobErrorRequest;
import org.flowable.external.job.rest.service.api.acquire.ExternalWorkerJobFailureRequest;
import org.flowable.external.job.rest.service.api.acquire.ExternalWorkerJobTerminateRequest;
import org.flowable.external.job.rest.service.api.query.ExternalWorkerJobQueryRequest;
import org.flowable.job.api.ExternalWorkerJob;
import org.flowable.job.api.ExternalWorkerJobAcquireBuilder;
import org.flowable.job.api.ExternalWorkerJobQuery;

/**
 * @author Filip Hrisafov
 */
public interface ExternalWorkerJobRestApiInterceptor {

    void accessExternalWorkerJobInfoWithQuery(ExternalWorkerJobQuery jobQuery, ExternalWorkerJobQueryRequest request);

    void accessExternalWorkerJobById(ExternalWorkerJob job);

    void accessAcquireExternalWorkerJobs(ExternalWorkerJobAcquireBuilder acquireBuilder, AcquireExternalWorkerJobRequest request);

    void completeExternalWorkerJob(ExternalWorkerJob job, ExternalWorkerJobCompleteRequest request);

    void bpmnErrorExternalWorkerJob(ExternalWorkerJob job, ExternalWorkerJobErrorRequest request);

    void cmmnTerminateExternalWorkerJob(ExternalWorkerJob job, ExternalWorkerJobTerminateRequest request);

    void failExternalWorkerJob(ExternalWorkerJob job, ExternalWorkerJobFailureRequest request);
}
