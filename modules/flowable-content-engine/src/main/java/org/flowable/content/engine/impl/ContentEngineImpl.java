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
package org.flowable.content.engine.impl;

import org.flowable.content.api.ContentManagementService;
import org.flowable.content.api.ContentService;
import org.flowable.content.engine.ContentEngine;
import org.flowable.content.engine.ContentEngineConfiguration;
import org.flowable.content.engine.ContentEngines;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 */
public class ContentEngineImpl implements ContentEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentEngineImpl.class);

    protected String name;
    protected ContentManagementService managementService;
    protected ContentService contentService;
    protected ContentEngineConfiguration engineConfiguration;

    public ContentEngineImpl(ContentEngineConfiguration engineConfiguration) {
        this.engineConfiguration = engineConfiguration;
        this.name = engineConfiguration.getEngineName();
        this.managementService = engineConfiguration.getContentManagementService();
        this.contentService = engineConfiguration.getContentService();

        if (engineConfiguration.getSchemaManagementCmd() != null) {
            engineConfiguration.getCommandExecutor().execute(engineConfiguration.getSchemaCommandConfig(), engineConfiguration.getSchemaManagementCmd());
        }
        
        if (name == null) {
            LOGGER.info("default flowable ContentEngine created");
        } else {
            LOGGER.info("ContentEngine {} created", name);
        }

        ContentEngines.registerContentEngine(this);
    }

    @Override
    public void close() {
        ContentEngines.unregister(this);
        engineConfiguration.close();
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ContentManagementService getContentManagementService() {
        return managementService;
    }

    @Override
    public ContentService getContentService() {
        return contentService;
    }

    @Override
    public ContentEngineConfiguration getContentEngineConfiguration() {
        return engineConfiguration;
    }
}
