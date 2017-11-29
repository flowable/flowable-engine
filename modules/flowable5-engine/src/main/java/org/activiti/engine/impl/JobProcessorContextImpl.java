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
package org.activiti.engine.impl;

import org.activiti.engine.impl.persistence.entity.AbstractJobEntity;
import org.activiti.engine.runtime.JobProcessor;
import org.activiti.engine.runtime.JobProcessorContext;

/**
 * The default {@link JobProcessorContext} implementation used in the {@link JobProcessor}.
 *
 * @author Guy Brand
 * @see JobProcessor
 */
public class JobProcessorContextImpl implements JobProcessorContext {

    protected final Phase phase;
    protected final AbstractJobEntity jobEntity;

    public JobProcessorContextImpl(Phase phase, AbstractJobEntity jobEntity) {
        this.phase = phase;
        this.jobEntity = jobEntity;
    }

    @Override
    public Phase getPhase() {
        return phase;
    }

    @Override
    public AbstractJobEntity getJobEntity() {
        return jobEntity;
    }

    @Override
    public boolean isInPhase(Phase phase) {
        return this.phase.equals(phase);
    }

    @Override
    public String toString() {
        return "JobProcessorContextImpl{" +
                "phase=" + phase +
                ", jobEntity=" + jobEntity +
                '}';
    }

}
