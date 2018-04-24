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
package org.flowable.spring.boot.app;

import org.flowable.spring.boot.FlowableServlet;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * @author Tijs Rademakers
 */
@ConfigurationProperties(prefix = "flowable.app")
public class FlowableAppProperties {

    /**
     * The servlet configuration for the Process Rest API.
     */
    @NestedConfigurationProperty
    private final FlowableServlet servlet = new FlowableServlet("/app-api", "Flowable App Rest API");
    
    public FlowableServlet getServlet() {
        return servlet;
    }
}
