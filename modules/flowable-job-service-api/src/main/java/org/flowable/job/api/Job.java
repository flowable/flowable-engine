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

package org.flowable.job.api;

import java.util.Date;

/**
 * Represents one job (timer, async job, etc.).
 * 
 * @author Joram Barrez
 */
public interface Job extends JobInfo {

    String JOB_TYPE_TIMER = "timer";
    String JOB_TYPE_MESSAGE = "message";

    boolean DEFAULT_EXCLUSIVE = true;
    int MAX_EXCEPTION_MESSAGE_LENGTH = 255;

    /**
     * Returns the date on which this job is supposed to be processed.
     */
    Date getDuedate();

    /**x
     * Returns the id of the process instance which execution created the job.
     */
    String getProcessInstanceId();

    /**
     * Returns the specific execution on which the job was created.
     */
    String getExecutionId();

    /**
     * Returns the specific process definition on which the job was created
     */
    String getProcessDefinitionId();
    
    /**
     * Reference to a scope identifier or null if none is set.
     */
    String getScopeId();
    
    /**
     * Reference to a sub scope identifier or null if none is set.
     */
    String getSubScopeId();
    
    /**
     * Reference to a scope type or null if none is set.
     */
    String getScopeType();
    
    /**
     * Reference to a scope definition identifier or null if none is set.
     */
    String getScopeDefinitionId();

    /**
     * Is the job exclusive?
     */
    boolean isExclusive();

    /**
     * Get the job type for this job.
     */
    String getJobType();
    
    /**
     * Returns the create datetime of the job.
     */
    Date getCreateTime();

}
