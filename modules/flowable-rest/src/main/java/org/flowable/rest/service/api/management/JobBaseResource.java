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

package org.flowable.rest.service.api.management;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.ManagementService;
import org.flowable.job.api.HistoryJob;
import org.flowable.job.api.Job;
import org.flowable.rest.service.api.BpmnRestApiInterceptor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Frederik Heremans
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class JobBaseResource {

    @Autowired
    protected ManagementService managementService;
    
    @Autowired(required=false)
    protected BpmnRestApiInterceptor restApiInterceptor;

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

    protected HistoryJob getHistoryJobById(String jobId) {
        HistoryJob job = managementService.createHistoryJobQuery().jobId(jobId).singleResult();
        validateHistoryJob(job, jobId);
        return job;
    }
    
    protected void validateJob(Job job, String jobId) {
        if (job == null) {
            throw new FlowableObjectNotFoundException("Could not find a job with id '" + jobId + "'.", Job.class);
        }
        
        if (restApiInterceptor != null) {
            restApiInterceptor.accessJobInfoById(job);
        }
    }

    protected void validateHistoryJob(HistoryJob job, String jobId) {
        if (job == null) {
            throw new FlowableObjectNotFoundException("Could not find a history job with id '" + jobId + "'.", HistoryJob.class);
        }

        if (restApiInterceptor != null) {
            restApiInterceptor.accessHistoryJobInfoById(job);
        }
    }
}
