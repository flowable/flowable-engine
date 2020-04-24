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

import java.time.Duration;
import java.util.List;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.job.api.AcquiredExternalWorkerJob;
import org.flowable.job.api.ExternalWorkerJobProvider;
import org.flowable.job.service.impl.cmd.AcquireExternalWorkerJobsCmd;

/**
 * @author Filip Hrisafov
 */
public class ExternalWorkerJobProviderImpl implements ExternalWorkerJobProvider {

    protected final CommandExecutor commandExecutor;

    protected String topic;
    protected Duration lockDuration;

    public ExternalWorkerJobProviderImpl(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    @Override
    public ExternalWorkerJobProvider topic(String topic, Duration lockDuration) {
        if (topic == null) {
            throw new FlowableIllegalArgumentException("topic is null");
        }

        if (lockDuration == null) {
            throw new FlowableIllegalArgumentException("lockDuration is null");
        }

        this.topic = topic;
        this.lockDuration = lockDuration;
        return this;
    }

    @Override
    public List<AcquiredExternalWorkerJob> acquireAndLock(int numberOfTasks, String workerId) {
        return commandExecutor.execute(new AcquireExternalWorkerJobsCmd(workerId, lockDuration, numberOfTasks, topic));
    }

    public String getTopic() {
        return topic;
    }

    public Duration getLockDuration() {
        return lockDuration;
    }
}
