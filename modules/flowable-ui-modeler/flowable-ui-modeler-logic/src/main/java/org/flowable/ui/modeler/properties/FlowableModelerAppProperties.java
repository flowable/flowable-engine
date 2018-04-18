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
package org.flowable.ui.modeler.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for the Modeler UI App.
 *
 * @author Filip Hrisafov
 */
@ConfigurationProperties(prefix = "flowable.modeler.app")
public class FlowableModelerAppProperties {

    /**
     * Enables the REST API (this is not the REST api used by the UI, but an api that's available over basic auth authentication).
     */
    private boolean restEnabled = true;

    /**
     * The root URI to the REST services of the Flowable engine, used by the Flowable Modeler application to deploy the application definition BAR file to the engine.
     * Default url for the Flowable Task application is http://localhost:8080/flowable-task/process-api
     */
    private String deploymentApiUrl = "http://localhost:8080/flowable-task/process-api";

    /**
     * The prefix for the database tables.
     */
    private String dataSourcePrefix = "";

    public boolean isRestEnabled() {
        return restEnabled;
    }

    public void setRestEnabled(boolean restEnabled) {
        this.restEnabled = restEnabled;
    }

    public String getDeploymentApiUrl() {
        return deploymentApiUrl;
    }

    public void setDeploymentApiUrl(String deploymentApiUrl) {
        this.deploymentApiUrl = deploymentApiUrl;
    }

    public String getDataSourcePrefix() {
        return dataSourcePrefix;
    }

    public void setDataSourcePrefix(String dataSourcePrefix) {
        this.dataSourcePrefix = dataSourcePrefix;
    }
}
