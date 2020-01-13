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
package org.flowable.eventregistry.api.management;

import org.flowable.eventregistry.api.EventRegistryConfigurationApi;

/**
 * @author Joram Barrez
 */
public interface EventRegistryHouseKeepingManager {

    /**
     * Typically called on bootup.
     * Will initialize necessary timer jobs to make sure the house keeping is executed regularly.
     */
    void initializeHouseKeepingJobs();

    /**
     * Allows to programmatically trigger the house keeping functionality.
     * Normally this will be executed by a timer job in the background.
     */
    void executeHouseKeeping();

    void setEventRegistryConfiguration(EventRegistryConfigurationApi eventRegistryConfiguration);

}
