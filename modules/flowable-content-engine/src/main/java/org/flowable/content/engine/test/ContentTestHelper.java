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
package org.flowable.content.engine.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.content.api.ContentItem;
import org.flowable.content.api.ContentService;
import org.flowable.content.engine.ContentEngine;
import org.flowable.content.engine.ContentEngineConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public abstract class ContentTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentTestHelper.class);

    public static final String EMPTY_LINE = "\n";

    static Map<String, ContentEngine> contentEngines = new HashMap<>();

    // Engine startup and shutdown helpers
    // ///////////////////////////////////////////////////

    public static ContentEngine getContentEngine(String configurationResource) {
        ContentEngine contentEngine = contentEngines.get(configurationResource);
        if (contentEngine == null) {
            LOGGER.debug("==== BUILDING CONTENT ENGINE ========================================================================");
            contentEngine = ContentEngineConfiguration.createContentEngineConfigurationFromResource(configurationResource)
                    .setDatabaseSchemaUpdate(ContentEngineConfiguration.DB_SCHEMA_UPDATE_DROP_CREATE)
                    .buildContentEngine();
            LOGGER.debug("==== CONTENT ENGINE CREATED =========================================================================");
            contentEngines.put(configurationResource, contentEngine);
        }
        return contentEngine;
    }

    public static void closeContentEngines() {
        for (ContentEngine contentEngine : contentEngines.values()) {
            contentEngine.close();
        }
        contentEngines.clear();
    }

    /**
     * Each test is assumed to clean up all DB content it entered. After a test method executed, this method scans all tables to see if the DB is completely clean. It throws AssertionFailed in case
     * the DB is not clean. If the DB is not clean, it is cleaned by performing a create a drop.
     */
    public static void assertAndEnsureCleanDb(ContentEngine contentEngine) {
        LOGGER.debug("verifying that db is clean after test");
        ContentService contentService = contentEngine.getContentEngineConfiguration().getContentService();
        List<ContentItem> items = contentService.createContentItemQuery().list();
        if (items != null && !items.isEmpty()) {
            throw new AssertionError("ContentItem is not empty");
        }
    }

}
