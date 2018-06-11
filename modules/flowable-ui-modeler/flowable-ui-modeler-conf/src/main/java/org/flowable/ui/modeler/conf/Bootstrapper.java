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
package org.flowable.ui.modeler.conf;

import org.flowable.ui.modeler.properties.FlowableModelerAppProperties;
import org.flowable.ui.modeler.service.FlowableDecisionTableService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * Responsible for executing all action required after booting up the Spring container.
 *
 * @author Yvo Swillens
 */
@Component
public class Bootstrapper implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    private FlowableDecisionTableService decisionTableService;

    @Autowired
    private FlowableModelerAppProperties modelerAppProperties;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() == null) { // Using Spring MVC, there are multiple child contexts. We only care about the root

            if (modelerAppProperties == null || modelerAppProperties.isDecisionTableMigrationEnabled()) {
                decisionTableService.migrateDecisionTables();
            }
        }
    }
}
