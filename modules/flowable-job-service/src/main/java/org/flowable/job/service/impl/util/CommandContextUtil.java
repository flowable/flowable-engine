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
package org.flowable.job.service.impl.util;

import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.db.DbSqlSession;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.asyncexecutor.JobManager;
import org.flowable.job.service.impl.persistence.entity.DeadLetterJobEntityManager;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntityManager;
import org.flowable.job.service.impl.persistence.entity.JobByteArrayEntityManager;
import org.flowable.job.service.impl.persistence.entity.JobEntityManager;
import org.flowable.job.service.impl.persistence.entity.SuspendedJobEntityManager;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntityManager;

public class CommandContextUtil {

    public static JobServiceConfiguration getJobServiceConfiguration() {
        return getJobServiceConfiguration(getCommandContext());
    }
    
    public static JobServiceConfiguration getJobServiceConfiguration(CommandContext commandContext) {
        if (commandContext != null) {
            return (JobServiceConfiguration) commandContext.getCurrentEngineConfiguration().getServiceConfigurations()
                            .get(EngineConfigurationConstants.KEY_JOB_SERVICE_CONFIG);
        }
        return null;
    }
    
    public static DbSqlSession getDbSqlSession() {
        return getDbSqlSession(getCommandContext());
    }
    
    public static DbSqlSession getDbSqlSession(CommandContext commandContext) {
        return commandContext.getSession(DbSqlSession.class);
    }
    
    public static FlowableEventDispatcher getEventDispatcher() {
        return getEventDispatcher(getCommandContext());
    }
    
    public static FlowableEventDispatcher getEventDispatcher(CommandContext commandContext) {
        return getJobServiceConfiguration(commandContext).getEventDispatcher();
    }
    
    public static JobManager getJobManager() {
        return getJobManager(getCommandContext());
    }
    
    public static JobManager getJobManager(CommandContext commandContext) {
        return getJobServiceConfiguration(commandContext).getJobManager();
    }
    
    public static JobEntityManager getJobEntityManager() {
        return getJobEntityManager(getCommandContext());
    }
    
    public static JobEntityManager getJobEntityManager(CommandContext commandContext) {
        return getJobServiceConfiguration(commandContext).getJobEntityManager();
    }
    
    public static DeadLetterJobEntityManager getDeadLetterJobEntityManager() {
        return getDeadLetterJobEntityManager(getCommandContext());
    }
    
    public static DeadLetterJobEntityManager getDeadLetterJobEntityManager(CommandContext commandContext) {
        return getJobServiceConfiguration(commandContext).getDeadLetterJobEntityManager();
    }
    
    public static SuspendedJobEntityManager getSuspendedJobEntityManager() {
        return getSuspendedJobEntityManager(getCommandContext());
    }
    
    public static SuspendedJobEntityManager getSuspendedJobEntityManager(CommandContext commandContext) {
        return getJobServiceConfiguration(commandContext).getSuspendedJobEntityManager();
    }
    
    public static TimerJobEntityManager getTimerJobEntityManager() {
        return getTimerJobEntityManager(getCommandContext());
    }
    
    public static TimerJobEntityManager getTimerJobEntityManager(CommandContext commandContext) {
        return getJobServiceConfiguration(commandContext).getTimerJobEntityManager();
    }
    
    public static HistoryJobEntityManager getHistoryJobEntityManager() {
        return getHistoryJobEntityManager(getCommandContext());
    }
    
    public static HistoryJobEntityManager getHistoryJobEntityManager(CommandContext commandContext) {
        return getJobServiceConfiguration(commandContext).getHistoryJobEntityManager();
    }
    
    public static JobByteArrayEntityManager getJobByteArrayEntityManager() {
        return getJobByteArrayEntityManager(getCommandContext());
    }
    
    public static JobByteArrayEntityManager getJobByteArrayEntityManager(CommandContext commandContext) {
        return getJobServiceConfiguration(commandContext).getJobByteArrayEntityManager();
    }
    
    public static CommandContext getCommandContext() {
        return Context.getCommandContext();
    }

}
