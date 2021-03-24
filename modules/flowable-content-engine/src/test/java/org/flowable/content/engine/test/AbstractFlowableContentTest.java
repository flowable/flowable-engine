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

import org.flowable.content.api.ContentManagementService;
import org.flowable.content.api.ContentService;
import org.flowable.content.engine.ContentEngine;
import org.flowable.content.engine.ContentEngineConfiguration;
import org.junit.Before;
import org.junit.Rule;

/**
 * Parent class for internal Flowable Content tests.
 * 
 * Boots up a content engine and caches it.
 * 
 * When using H2 and the default schema name, it will also boot the H2 webapp (reachable with browser on http://localhost:8082/)
 * 
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class AbstractFlowableContentTest {

    public static String H2_TEST_JDBC_URL = "jdbc:h2:mem:flowablecontent;DB_CLOSE_DELAY=1000";

    @Rule
    public FlowableContentRule rule = new FlowableContentRule();

    protected static ContentEngine cachedContentEngine;
    protected ContentEngineConfiguration contentEngineConfiguration;
    protected ContentService contentService;
    protected ContentManagementService contentManagementService;

    @Before
    public void initContentEngine() {
        if (cachedContentEngine == null) {
            cachedContentEngine = rule.getContentEngine();
        }

        this.contentEngineConfiguration = cachedContentEngine.getContentEngineConfiguration();
        this.contentService = cachedContentEngine.getContentService();
        this.contentManagementService = cachedContentEngine.getContentManagementService();
    }

}
