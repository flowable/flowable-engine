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
package org.flowable.content.engine;

import org.flowable.common.engine.impl.FlowableVersions;
import org.flowable.content.api.ContentManagementService;
import org.flowable.content.api.ContentService;

public interface ContentEngine {

    /**
     * the version of the flowable content library
     */
    public static String VERSION = FlowableVersions.CURRENT_VERSION;

    /**
     * The name as specified in 'content-engine-name' in the flowable.content.cfg.xml configuration file. The default name for a process engine is 'default
     */
    String getName();

    void close();

    ContentManagementService getContentManagementService();

    ContentService getContentService();

    ContentEngineConfiguration getContentEngineConfiguration();
}
