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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.annotation.PostConstruct;

/**
 * Development @Profile specific configuration property overrides
 *
 * @author Yvo Swillens
 */
@Profile({"dev"})
@Configuration
public class ApplicationDevelopmentConfiguration {

    protected static final boolean FLOWABLE_MODELER_REST_ENABLED = true;
    protected static final String FLOWABLE_MODELER_DEPLOYMENT_URL = "http://localhost:9999/flowable-task/app-api";

    @Autowired
    private FlowableModelerAppProperties flowableModelerAppProperties;

    @PostConstruct
    public void postConstruct() {
        flowableModelerAppProperties.setRestEnabled(FLOWABLE_MODELER_REST_ENABLED);
        flowableModelerAppProperties.setDeploymentApiUrl(FLOWABLE_MODELER_DEPLOYMENT_URL);
    }
}
