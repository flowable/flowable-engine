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

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.eventregistry.api.management.EventRegistryHouseKeepingManager;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class EventRegistryHouseKeepingJobHandler implements JobHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventRegistryHouseKeepingJobHandler.class);

    public static final String TYPE = "event-registry-housekeeping";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        EventRegistryEngineConfiguration eventRegistryEngineConfiguration = (EventRegistryEngineConfiguration) processEngineConfiguration
            .getEngineConfigurations()
            .get(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG);
        if (eventRegistryEngineConfiguration != null) {
            EventRegistryHouseKeepingManager eventRegistryHouseKeepingManager = eventRegistryEngineConfiguration.getEventRegistryHouseKeepingManager();
            if (eventRegistryHouseKeepingManager != null) {
                eventRegistryHouseKeepingManager.executeHouseKeeping();

            } else {
                LOGGER.warn("No eventRegistryHouseKeepingManager found. This is most likely a configuration error.");

            }

        } else {
            LOGGER.warn("No eventRegistryEngine found, This is most likely a configuration error.");

        }
    }

}
