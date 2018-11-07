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
package org.flowable.cmmn.engine.impl.cmd;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.JobService;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.impl.persistence.entity.AbstractRuntimeJobEntity;
import org.flowable.job.service.impl.persistence.entity.JobEntity;

/**
 * @author Saeid Mirzaei
 * @author Joram Barrez
 */
public class JobRetryCmd implements Command<Object> {

    protected String jobId;
    protected Throwable exception;

    public JobRetryCmd(String jobId, Throwable exception) {
        this.jobId = jobId;
        this.exception = exception;
    }

    @Override
    public Object execute(CommandContext commandContext) {
        JobService jobService = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getJobServiceConfiguration().getJobService();
        TimerJobService timerJobService = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getJobServiceConfiguration().getTimerJobService();
        
        JobEntity job = jobService.findJobById(jobId);
        if (job == null) {
            return null;
        }

        AbstractRuntimeJobEntity newJobEntity = null;
        if (job.getRetries() <= 1) {
            newJobEntity = jobService.moveJobToDeadLetterJob(job);
        } else {
            newJobEntity = timerJobService.moveJobToTimerJob(job);
        }
        newJobEntity.setRetries(job.getRetries() - 1);
        
        if (job.getDuedate() == null || JobEntity.JOB_TYPE_MESSAGE.equals(job.getJobType())) {
            // add wait time for failed async job
            newJobEntity.setDuedate(calculateDueDate(commandContext, CommandContextUtil.getCmmnEngineConfiguration(commandContext).getAsyncFailedJobWaitTime(), null));
        } else {
            // add default wait time for failed job
            newJobEntity.setDuedate(calculateDueDate(commandContext, CommandContextUtil.getCmmnEngineConfiguration(commandContext).getDefaultFailedJobWaitTime(), job.getDuedate()));
        }

        if (exception != null) {
            newJobEntity.setExceptionMessage(exception.getMessage());
            newJobEntity.setExceptionStacktrace(getExceptionStacktrace());
        }

        return null;
    }

    protected Date calculateDueDate(CommandContext commandContext, int waitTimeInSeconds, Date oldDate) {
        Calendar newDateCal = new GregorianCalendar();
        if (oldDate != null) {
            newDateCal.setTime(oldDate);
        } else {
            newDateCal.setTime(CommandContextUtil.getCmmnEngineConfiguration(commandContext).getClock().getCurrentTime());
        }

        newDateCal.add(Calendar.SECOND, waitTimeInSeconds);
        return newDateCal.getTime();
    }

    protected String getExceptionStacktrace() {
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

}
