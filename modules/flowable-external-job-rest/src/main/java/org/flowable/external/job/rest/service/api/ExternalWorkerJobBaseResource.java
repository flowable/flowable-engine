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

import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.ManagementService;
import org.flowable.job.api.ExternalWorkerJob;
import org.flowable.job.api.ExternalWorkerJobQuery;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Filip Hrisafov
 */
public class ExternalWorkerJobBaseResource {

    protected ManagementService managementService;
    protected CmmnManagementService cmmnManagementService;
    protected ExternalWorkerJobRestApiInterceptor restApiInterceptor;

    protected ExternalWorkerJobQuery createExternalWorkerJobQuery() {
        if (managementService != null) {
            return managementService.createExternalWorkerJobQuery();
        } else if (cmmnManagementService != null) {
            return cmmnManagementService.createExternalWorkerJobQuery();
        } else {
            throw new FlowableException("Cannot query external jobs. There is no BPMN or CMMN engine available");
        }
    }

    protected ExternalWorkerJob getExternalWorkerJobById(String jobId) {
        ExternalWorkerJob job = createExternalWorkerJobQuery().jobId(jobId).singleResult();
        if (job == null) {
            throw new FlowableObjectNotFoundException("Could not find external worker job with id '" + jobId + "'.", ExternalWorkerJob.class);
        }

        if (restApiInterceptor != null) {
            restApiInterceptor.accessExternalWorkerJobById(job);
        }

        return job;
    }

    @Autowired(required = false)
    public void setManagementService(ManagementService managementService) {
        this.managementService = managementService;
    }

    @Autowired(required = false)
    public void setCmmnManagementService(CmmnManagementService cmmnManagementService) {
        this.cmmnManagementService = cmmnManagementService;
    }

    @Autowired(required = false)
    public void setRestApiInterceptor(ExternalWorkerJobRestApiInterceptor restApiInterceptor) {
        this.restApiInterceptor = restApiInterceptor;
    }
}
