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

package org.flowable.job.service;

import org.flowable.job.service.impl.persistence.entity.AbstractRuntimeJobEntity;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.service.delegate.VariableScope;

/**
 * @author Tijs Rademakers
 */
public interface JobScopeInterface {

    VariableScope resolveVariableScope(Job job);
    
    boolean isFlowable5ProcessDefinitionId(String processDefinitionId);
    
    void executeV5Job(Job job);
    
    void executeV5JobWithLockAndRetry(Job job);
    
    boolean handleJobInsert(Job job);
    
    void handleJobDelete(Job job);
    
    void handleFailedJob(AbstractRuntimeJobEntity job, Throwable exception);
    
    void updateJobScopeLockTime(Job job);
    
    void clearJobScopeLockTime(Job job);
    
    void deleteV5Job(String jobId);
    
    void restoreJobExtraData(JobEntity jobEntity, VariableScope variableScope);
}
