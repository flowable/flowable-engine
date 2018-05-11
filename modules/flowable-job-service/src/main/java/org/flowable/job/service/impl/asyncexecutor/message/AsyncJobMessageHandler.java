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
package org.flowable.job.service.impl.asyncexecutor.message;

import org.flowable.job.service.impl.persistence.entity.JobEntity;

/**
 * Experimental.
 * 
 * An implementation of this interface needs to be injected into an {@link AsyncJobMessageReceiver} instance.
 * The helper will receive the information from a message and the implementation of this class should execute the actual logic.
 * 
 * @author Joram Barrez
 */
public interface AsyncJobMessageHandler {

    /**
     * Handle the job and its data.
     * 
     * Returning true will delete the job.
     * Returning false will unacquire the job and decrement the retries.
     */
    public abstract boolean handleJob(JobEntity job);
    
}
