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

import org.flowable.job.service.HistoryJobProcessor;
import org.flowable.job.service.HistoryJobProcessorContext;
import org.flowable.job.service.JobProcessor;
import org.flowable.job.service.JobProcessorContext;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.HistoryJobProcessorContextImpl;
import org.flowable.job.service.impl.JobProcessorContextImpl;
import org.flowable.job.service.impl.persistence.entity.AbstractJobEntity;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;

/**
 * @author Filip Hrisafov
 */
public class JobProcessorUtil {

    public static void callJobProcessors(JobServiceConfiguration jobServiceConfiguration, JobProcessorContext.Phase processorType,
            AbstractJobEntity abstractJobEntity) {
        if (jobServiceConfiguration.getJobProcessors() != null) {
            JobProcessorContextImpl jobProcessorContext = new JobProcessorContextImpl(processorType, abstractJobEntity);
            for (JobProcessor jobProcessor : jobServiceConfiguration.getJobProcessors()) {
                jobProcessor.process(jobProcessorContext);
            }
        }
    }

    public static void callHistoryJobProcessors(JobServiceConfiguration jobServiceConfiguration, HistoryJobProcessorContext.Phase processorType,
            HistoryJobEntity historyJobEntity) {
        if (jobServiceConfiguration.getHistoryJobProcessors() != null) {
            HistoryJobProcessorContextImpl historyJobProcessorContext = new HistoryJobProcessorContextImpl(processorType, historyJobEntity);
            for (HistoryJobProcessor historyJobProcessor : jobServiceConfiguration.getHistoryJobProcessors()) {
                historyJobProcessor.process(historyJobProcessorContext);
            }
        }
    }

}
