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

import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;

/**
 * The context used in the {@link HistoryJobProcessor}. It holds the history job phase and the
 * history job entity.
 *
 * @author Guy Brand
 * @see Phase
 */
public interface HistoryJobProcessorContext {

    /**
     * The job phases.
     */
    enum Phase {
        /**
         * The history job is in this phase before it gets created. The history job entity from
         * the {@link #getHistoryJobEntity()} can therefore be modified before it gets persisted.
         */
        BEFORE_CREATE,

        /**
         * The history job is in this phase before it gets executed. Normally this stage
         * is entered through the {@link AsyncExecutor} meaning that the history job is executed
         * in another thread.
         */
        BEFORE_EXECUTE
    }

    /**
     * Get the history job phase.
     *
     * @return the history job phase
     */
    Phase getPhase();

    /**
     * Get the history job entity.
     *
     * @return the history job entity
     */
    HistoryJobEntity getHistoryJobEntity();

    /**
     * Returns {@code true} if the {@link HistoryJobProcessorContext} is in the specified phase, false otherwise.
     *
     * @param phase the phase to check
     * @return {@code true} if the {@link HistoryJobProcessorContext} is in the specified phase, false otherwise.
     */
    boolean isInPhase(Phase phase);

}
