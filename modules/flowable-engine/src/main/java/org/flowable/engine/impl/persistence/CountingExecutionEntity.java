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
package org.flowable.engine.impl.persistence;

import org.flowable.common.engine.impl.persistence.entity.Entity;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public interface CountingExecutionEntity extends Entity {
    
    boolean isProcessInstanceType();
    
    boolean isCountEnabled();

    int getEventSubscriptionCount();
    void incrementEventSubscriptionCount();
    void decrementEventSubscriptionCount();

    int getTaskCount();
    void incrementTaskCount();
    void decrementTaskCount();

    int getJobCount();
    void incrementJobCount();
    void decrementJobCount();

    int getTimerJobCount();
    void incrementTimerJobCount();
    void decrementTimerJobCount();

    int getSuspendedJobCount();
    void incrementSuspendedJobCount();
    void decrementSuspendedJobCount();

    int getDeadLetterJobCount();
    void incrementDeadLetterJobCount();
    void decrementDeadLetterJobCount();

    int getExternalWorkerJobCount();
    void incrementExternalWorkerJobCount();
    void decrementExternalWorkerJobCount();

    int getVariableCount();
    void incrementVariableCount();
    void decrementVariableCount();

    int getIdentityLinkCount();
    void incrementIdentityLinkCount();
    void decrementIdentityLinkCount();

}