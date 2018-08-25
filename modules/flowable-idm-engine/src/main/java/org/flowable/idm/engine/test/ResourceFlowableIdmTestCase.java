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

package org.flowable.idm.engine.test;

import org.flowable.idm.engine.IdmEngineConfiguration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
@Tag("resource")
public abstract class ResourceFlowableIdmTestCase extends AbstractFlowableIdmTestCase {

    protected String idmConfigurationResource;
    protected String idmEngineName;

    @RegisterExtension
    protected final ResourceFlowableIdmExtension extension;

    public ResourceFlowableIdmTestCase(String idmConfigurationResource) {
        this(idmConfigurationResource, null);
    }

    public ResourceFlowableIdmTestCase(String idmConfigurationResource, String idmEngineName) {
        this.idmConfigurationResource = idmConfigurationResource;
        this.idmEngineName = idmEngineName;
        this.extension = new ResourceFlowableIdmExtension(idmConfigurationResource, idmEngineName, this::additionalConfiguration);
    }

    protected void additionalConfiguration(IdmEngineConfiguration idmEngineConfiguration) {

    }

}
