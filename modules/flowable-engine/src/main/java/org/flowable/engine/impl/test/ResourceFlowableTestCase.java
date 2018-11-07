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

package org.flowable.engine.impl.test;

import org.flowable.engine.ProcessEngineConfiguration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
@Tag("resource")
public abstract class ResourceFlowableTestCase extends AbstractFlowableTestCase {

    @RegisterExtension
    protected final ResourceFlowableExtension extension;

    protected String activitiConfigurationResource;
    protected String processEngineName;

    public ResourceFlowableTestCase(String activitiConfigurationResource) {
        this(activitiConfigurationResource, null);
    }

    public ResourceFlowableTestCase(String activitiConfigurationResource, String processEngineName) {
        this.activitiConfigurationResource = activitiConfigurationResource;
        this.processEngineName = processEngineName;
        this.extension = new ResourceFlowableExtension(activitiConfigurationResource, processEngineName, this::additionalConfiguration);
    }

    protected void additionalConfiguration(ProcessEngineConfiguration processEngineConfiguration) {

    }

    protected void rebootEngine() {
        initializeServices(extension.rebootEngine());
    }

}
