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
package org.flowable.eventregistry.impl;

import org.flowable.common.engine.impl.FlowableVersions;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRepositoryService;

public interface EventRegistryEngine {

    /**
     * the version of the flowable event registry library
     */
    public static String VERSION = FlowableVersions.CURRENT_VERSION;

    /**
     * The name as specified in 'event-registry-engine-name' in the flowable.registry.cfg.xml configuration file. The default name for a event registry engine is 'default
     */
    String getName();

    void close();

    EventRepositoryService getEventRepositoryService();
    
    EventRegistry getEventRegistry();

    EventRegistryEngineConfiguration getEventRegistryEngineConfiguration();
}
