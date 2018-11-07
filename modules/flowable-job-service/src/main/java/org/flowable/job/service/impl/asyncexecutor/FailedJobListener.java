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

package org.flowable.job.service.impl.asyncexecutor;

import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandContextCloseListener;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.job.api.Job;
import org.flowable.job.service.event.impl.FlowableJobEventBuilder;
import org.flowable.job.service.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Frederik Heremans
 * @author Saeid Mirzaei
 * @author Joram Barrez
 */
public class FailedJobListener implements CommandContextCloseListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(FailedJobListener.class);

    protected CommandExecutor commandExecutor;
    protected Job job;

    public FailedJobListener(CommandExecutor commandExecutor, Job job) {
        this.commandExecutor = commandExecutor;
        this.job = job;
    }

    @Override
    public void closing(CommandContext commandContext) {
    }

    @Override
    public void afterSessionsFlush(CommandContext commandContext) {
    }

    @Override
    public void closed(CommandContext context) {
        FlowableEventDispatcher eventDispatcher = CommandContextUtil.getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(
                    FlowableJobEventBuilder.createEntityEvent(FlowableEngineEventType.JOB_EXECUTION_SUCCESS, job));
        }
    }

    @Override
    public void closeFailure(CommandContext commandContext) {
        FlowableEventDispatcher eventDispatcher = CommandContextUtil.getEventDispatcher();
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            eventDispatcher.dispatchEvent(FlowableJobEventBuilder.createEntityExceptionEvent(
                    FlowableEngineEventType.JOB_EXECUTION_FAILURE, job, commandContext.getException()));
        }

        CommandConfig commandConfig = commandExecutor.getDefaultConfig().transactionRequiresNew();
        FailedJobCommandFactory failedJobCommandFactory = CommandContextUtil.getJobServiceConfiguration().getFailedJobCommandFactory();
        Command<Object> cmd = failedJobCommandFactory.getCommand(job.getId(), commandContext.getException());

        LOGGER.trace("Using FailedJobCommandFactory '{}' and command of type '{}'", failedJobCommandFactory.getClass(), cmd.getClass());
        commandExecutor.execute(commandConfig, cmd);
    }

}
