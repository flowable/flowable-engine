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
package org.flowable.http.bpmn;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;

/**
 * Http Server and API to test HTTP Activity
 *
 * @author Harsha Teja Kanna
 */
public abstract class HttpServiceTaskTestCase extends PluggableFlowableTestCase {
    
    static {
        TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK.add("ACT_CMMN_DATABASECHANGELOG");
        TABLENAMES_EXCLUDED_FROM_DB_CLEAN_CHECK.add("ACT_CMMN_DATABASECHANGELOGLOCK");
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        HttpServiceTaskTestServer.setUp();
    }
}
