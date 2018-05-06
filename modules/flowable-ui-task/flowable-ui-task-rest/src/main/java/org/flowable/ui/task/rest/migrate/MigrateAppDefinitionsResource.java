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
package org.flowable.ui.task.rest.migrate;

import org.flowable.ui.task.service.runtime.FlowableAppDefinitionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing the app definitions.
 */
@RestController
@RequestMapping("/app")
public class MigrateAppDefinitionsResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrateAppDefinitionsResource.class);

    @Autowired
    protected FlowableAppDefinitionService appDefinitionService;

    @RequestMapping(value = "/rest/migrate/app-definitions", method = RequestMethod.GET)
    public String migrateAppDefinitions() {
        return appDefinitionService.migrateAppDefinitions();
    }
}
