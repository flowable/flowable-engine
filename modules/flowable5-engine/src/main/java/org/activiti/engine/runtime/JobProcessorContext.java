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
package org.activiti.engine.runtime;


import org.activiti.engine.impl.persistence.entity.AbstractJobEntity;

/**
 * The context used in the {@link JobProcessor}. It holds the job phase and the
 * job entity.
 *
 * @author Guy Brand
 * @see Phase
 */
public interface JobProcessorContext {

    /**
     * The job phases.
     */
    enum Phase {
        /**
         * The job is in this phase before it gets created. The job entity from
         * the {@link #getJobEntity()} can therefore be modified before it gets persisted.
         */
        BEFORE_CREATE,

        /**
         * The job is in this phase before it gets executed. Normally this stage
         * is entered through the async executor meaning that the job is executed
         * in another thread.
         */
        BEFORE_EXECUTE
    }

    /**
     * Get the job phase.
     *
     * @return the job phase
     */
    Phase getPhase();

    /**
     * Get the job entity.
     *
     * @return the job entity
     */
    AbstractJobEntity getJobEntity();

    /**
     * Returns {@code true} if the {@link JobProcessorContext} is in the specified phase, false otherwise.
     *
     * @param phase the phase to check
     * @return {@code true} if the {@link JobProcessorContext} is in the specified phase, false otherwise.
     */
    boolean isInPhase(Phase phase);

}
