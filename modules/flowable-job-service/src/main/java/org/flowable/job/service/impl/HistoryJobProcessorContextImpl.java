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
package org.flowable.job.service.impl;

import org.flowable.job.service.HistoryJobProcessor;
import org.flowable.job.service.HistoryJobProcessorContext;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;

/**
 * The default {@link HistoryJobProcessorContext} implementation used in the {@link HistoryJobProcessor}.
 *
 * @author Guy Brand
 * @see HistoryJobProcessor
 */
public class HistoryJobProcessorContextImpl implements HistoryJobProcessorContext {

    protected final Phase phase;
    protected final HistoryJobEntity historyJobEntity;

    public HistoryJobProcessorContextImpl(Phase phase, HistoryJobEntity historyJobEntity) {
        this.phase = phase;
        this.historyJobEntity = historyJobEntity;
    }

    @Override
    public Phase getPhase() {
        return phase;
    }

    @Override
    public HistoryJobEntity getHistoryJobEntity() {
        return historyJobEntity;
    }

    @Override
    public boolean isInPhase(Phase phase) {
        return this.phase.equals(phase);
    }

    @Override
    public String toString() {
        return "HistoryJobProcessorContextImpl{" +
                "phase=" + phase +
                ", historyJobEntity=" + historyJobEntity +
                '}';
    }

}
