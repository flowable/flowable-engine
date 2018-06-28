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

package org.flowable.cmmn.rest.service.api.management;

import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.rest.service.api.CmmnRestApiInterceptor;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.job.api.Job;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Tijs Rademakers
 */
public class JobBaseResource {

    @Autowired
    protected CmmnManagementService managementService;
    
    @Autowired(required=false)
    protected CmmnRestApiInterceptor restApiInterceptor;

    protected Job getJobById(String jobId) {
        Job job = managementService.createJobQuery().jobId(jobId).singleResult();
        validateJob(job, jobId);
        return job;
    }
    
    protected Job getTimerJobById(String jobId) {
        Job job = managementService.createTimerJobQuery().jobId(jobId).singleResult();
        validateJob(job, jobId);
        return job;
    }
    
    protected Job getSuspendedJobById(String jobId) {
        Job job = managementService.createSuspendedJobQuery().jobId(jobId).singleResult();
        validateJob(job, jobId);
        return job;
    }

    protected Job getDeadLetterJobById(String jobId) {
        Job job = managementService.createDeadLetterJobQuery().jobId(jobId).singleResult();
        validateJob(job, jobId);
        return job;
    }
    
    protected void validateJob(Job job, String jobId) {
        if (job == null) {
            throw new FlowableObjectNotFoundException("Could not find a deadletter job with id '" + jobId + "'.", Job.class);
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessJobInfoById(job);
        }
    }
}
