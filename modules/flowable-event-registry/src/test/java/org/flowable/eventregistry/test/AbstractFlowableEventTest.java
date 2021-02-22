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
package org.flowable.eventregistry.test;

import org.flowable.common.engine.impl.test.EnsureCleanDb;
import org.flowable.common.engine.impl.test.LoggingExtension;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.impl.EventRegistryEngine;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Parent class for internal Flowable Event Registry tests.
 * <p>
 * Boots up a form engine and caches it.
 *
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
@ExtendWith(FlowableEventExtension.class)
@ExtendWith(LoggingExtension.class)
@EnsureCleanDb(excludeTables = {
        "ACT_GE_PROPERTY",
        "ACT_ID_PROPERTY",
        "FLW_EV_DATABASECHANGELOGLOCK",
        "FLW_EV_DATABASECHANGELOG"
})
public class AbstractFlowableEventTest {

    protected EventRegistryEngine eventRegistryEngine;
    protected EventRegistryEngineConfiguration eventEngineConfiguration;
    protected EventRepositoryService repositoryService;
    protected EventRegistry eventRegistry;

    @BeforeEach
    public void initEventRegistryEngine(EventRegistryEngine eventRegistryEngine, EventRegistryEngineConfiguration eventRegistryEngineConfiguration,
            EventRepositoryService eventRepositoryService, EventRegistry eventRegistry) {

        this.eventRegistryEngine = eventRegistryEngine;
        this.eventEngineConfiguration = eventRegistryEngineConfiguration;
        this.repositoryService = eventRepositoryService;
        this.eventRegistry = eventRegistry;
    }

}
