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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

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
@ExtendWith(FlowableContentExtension.class)
public class AbstractFlowableContentTest {

    protected ContentEngineConfiguration contentEngineConfiguration;
    protected ContentService contentService;
    protected ContentManagementService contentManagementService;

    @BeforeEach
    public void initContentEngine(ContentEngine contentEngine) {
        this.contentEngineConfiguration = contentEngine.getContentEngineConfiguration();
        this.contentService = contentEngine.getContentService();
        this.contentManagementService = contentEngine.getContentManagementService();
    }

}
