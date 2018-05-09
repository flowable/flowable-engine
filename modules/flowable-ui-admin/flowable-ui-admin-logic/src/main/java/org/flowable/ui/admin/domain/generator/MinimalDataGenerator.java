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
package org.flowable.ui.admin.domain.generator;

import java.util.List;

import org.flowable.ui.admin.dto.ServerConfigRepresentation;
import org.flowable.ui.admin.service.engine.ServerConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Generates the minimal data needed when the application is booted with no data at all.
 *
 * @author jbarrez
 */
public class MinimalDataGenerator implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinimalDataGenerator.class);

    @Autowired
    protected ServerConfigService serverConfigService;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() == null) { // Using Spring MVC, there are multiple child contexts. We only care about the root
            LOGGER.info("Verifying if minimal data is present");

            List<ServerConfigRepresentation> serverConfigs = serverConfigService.findAll();
            if (serverConfigs.size() == 0) {
                LOGGER.info("No server configurations found, creating default server configurations");
                serverConfigService.createDefaultServerConfigs();
            
            } else if (serverConfigs.size() == 4) {
                serverConfigService.createCmmnDefaultServerConfig();
                
            } else if (serverConfigs.size() == 5) {
                serverConfigService.createAppDefaultServerConfig();
            }
        }
    }
}
