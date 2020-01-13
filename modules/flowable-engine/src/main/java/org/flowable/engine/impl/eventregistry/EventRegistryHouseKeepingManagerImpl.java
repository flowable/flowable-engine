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
package org.flowable.engine.impl.eventregistry;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.flowable.common.engine.impl.calendar.BusinessCalendar;
import org.flowable.common.engine.impl.calendar.CycleBusinessCalendar;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.ManagementService;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.eventregistry.api.ChannelDefinition;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRegistryConfigurationApi;
import org.flowable.eventregistry.api.management.EventRegistryHouseKeepingManager;
import org.flowable.job.api.Job;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.TimerJobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.job.service.impl.persistence.entity.TimerJobEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the housekeeping logic of the event registry.
 *
 * Note: The reason that this class is in the process engine module, is because
 * the event registry engine does not depend on the job service.
 *
 * @author Joram Barrez
 */
public class EventRegistryHouseKeepingManagerImpl implements EventRegistryHouseKeepingManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventRegistryHouseKeepingManagerImpl.class);

    protected ProcessEngineConfigurationImpl processEngineConfiguration;
    protected EventRegistryConfigurationApi eventRegistryConfiguration;

    public EventRegistryHouseKeepingManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
    }

    @Override
    public void initializeHouseKeepingJobs() {
        processEngineConfiguration.getCommandExecutor().execute((Command<Void>) commandContext -> {
            createTimerJobIfNeeded();
            return null;
        });
    }

    protected void createTimerJobIfNeeded() {
        ManagementService managementService = processEngineConfiguration.getManagementService();
        TimerJobService timerJobService = ((JobServiceConfiguration) processEngineConfiguration.getServiceConfigurations()
            .get(EngineConfigurationConstants.KEY_JOB_SERVICE_CONFIG)).getTimerJobService();

        List<Job> existingJobs = managementService.createTimerJobQuery().handlerType(EventRegistryHouseKeepingJobHandler.TYPE).list();
        if (existingJobs.isEmpty()) {

            // No timer job yet, create one

            TimerJobEntity timerJob = timerJobService.createTimerJob();
            timerJob.setJobType(JobEntity.JOB_TYPE_TIMER);
            timerJob.setRevision(1);
            timerJob.setJobHandlerType(EventRegistryHouseKeepingJobHandler.TYPE);

            BusinessCalendar businessCalendar = processEngineConfiguration.getBusinessCalendarManager().getBusinessCalendar(CycleBusinessCalendar.NAME);
            timerJob.setDuedate(businessCalendar.resolveDuedate(processEngineConfiguration.getEventRegistryHouseKeepingTimeCycleConfig()));
            timerJob.setRepeat(processEngineConfiguration.getEventRegistryHouseKeepingTimeCycleConfig());

            timerJobService.scheduleTimerJob(timerJob);

        } else {

            // Timer job(s) exist, remove any if there's more than one (shouldn't happen)

            if (existingJobs.size() > 1) {
                for (int i = 1; i < existingJobs.size(); i++) {
                    managementService.deleteTimerJob(existingJobs.get(i).getId());
                }
            }

        }
    }

    @Override
    public void executeHouseKeeping() {
        synchronized (this) { //  synchronized is only really needed in case calls to this logic happen concurrently, which normally shouldn't be the case (e.g.  timer jobs repetition every x ms)

            // This query could be optimized in the future by keeping a timestamp of the last query
            // and querying by createtime (but detecting deletes would need dedicated logic!).
            // The amount of channel definitions however, should typically not be large.
            List<ChannelDefinition> channelDefinitions = eventRegistryConfiguration.getEventRepositoryService()
                .createChannelDefinitionQuery()
                .latestVersion()
                .list();

            EventRegistry eventRegistry = eventRegistryConfiguration.getEventRegistry();
            Set<String> inboundChannelKeys = eventRegistry.getInboundChannelModels().keySet();
            Set<String> outboundChannelKeys = eventRegistry.getOutboundChannelModels().keySet();

            // Check for new deployments
            for (ChannelDefinition channelDefinition : channelDefinitions) {

                // The key is unique. When no instance is returned, the channel definition has not yet been deployed before (e.g. deployed on another node)
                if (!inboundChannelKeys.contains(channelDefinition.getKey()) && !outboundChannelKeys.contains(channelDefinition.getKey())) {
                    eventRegistryConfiguration.getEventRepositoryService().getChannelModelById(channelDefinition.getId());
                    LOGGER.info("Deployed channel definition with key {}", channelDefinition.getKey());
                }

            }

            // Check for removed deployments
            Set<String> channelDefinitionKeys = channelDefinitions.stream().map(ChannelDefinition::getKey).collect(Collectors.toSet());
            for (String inboundChannelKey : inboundChannelKeys) {
                if (!channelDefinitionKeys.contains(inboundChannelKey)) {
                    eventRegistry.removeChannelModel(inboundChannelKey);
                }
            }
            for (String outboundChannelKey: outboundChannelKeys) {
                if (!channelDefinitionKeys.contains(outboundChannelKey)) {
                    eventRegistry.removeChannelModel(outboundChannelKey);
                }
            }

        }
    }

    public ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
        return processEngineConfiguration;
    }
    public void setProcessEngineConfiguration(ProcessEngineConfigurationImpl processEngineConfiguration) {
        this.processEngineConfiguration = processEngineConfiguration;
    }
    public EventRegistryConfigurationApi getEventRegistryConfiguration() {
        return eventRegistryConfiguration;
    }
    public void setEventRegistryConfiguration(EventRegistryConfigurationApi eventRegistryConfiguration) {
        this.eventRegistryConfiguration = eventRegistryConfiguration;
    }
}
