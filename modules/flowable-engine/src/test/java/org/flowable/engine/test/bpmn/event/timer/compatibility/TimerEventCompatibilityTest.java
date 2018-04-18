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
package org.flowable.engine.test.bpmn.event.timer.compatibility;

import java.util.Date;

import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.engine.impl.ProcessEngineImpl;
import org.flowable.engine.impl.jobexecutor.TimerEventHandler;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;

public abstract class TimerEventCompatibilityTest extends PluggableFlowableTestCase {

    protected void changeConfigurationToPlainText(TimerJobEntity job) {

        String activityId = TimerEventHandler.getActivityIdFromConfiguration(job.getJobHandlerConfiguration());

        final TimerJobEntity finalJob = job;
        CommandExecutor commandExecutor = ((ProcessEngineImpl) processEngine).getProcessEngineConfiguration().getCommandExecutor();
        CommandConfig config = new CommandConfig().transactionNotSupported();
        final String finalActivityId = activityId;
        commandExecutor.execute(config, new Command<Object>() {

            @Override
            public Object execute(CommandContext commandContext) {
                DbSqlSession session = CommandContextUtil.getDbSqlSession(commandContext);
                session.delete(finalJob);
                session.flush();
                session.commit();
                return null;
            }
        });

        commandExecutor.execute(config, new Command<Object>() {

            @Override
            public Object execute(CommandContext commandContext) {
                DbSqlSession session = CommandContextUtil.getDbSqlSession(commandContext);

                finalJob.setJobHandlerConfiguration(finalActivityId);
                finalJob.setId(null);
                session.insert(finalJob);

                session.flush();
                session.commit();
                return null;
            }
        });
    }

    protected void moveByMinutes(int minutes) throws Exception {
        processEngineConfiguration.getClock().setCurrentTime(new Date(processEngineConfiguration.getClock().getCurrentTime().getTime() + ((minutes * 60 * 1000))));
    }
}
