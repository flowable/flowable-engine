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

import org.flowable.common.engine.impl.test.EnsureCleanDb;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

/**
 * Http Server and API to test HTTP Activity
 *
 * @author Harsha Teja Kanna
 */
@EnsureCleanDb(excludeTables = {
    "ACT_GE_PROPERTY",
    "ACT_ID_PROPERTY",
    "ACT_CMMN_DATABASECHANGELOG",
    "ACT_CMMN_DATABASECHANGELOGLOCK"
})
@Tag("http")
public abstract class HttpServiceTaskTestCase extends PluggableFlowableTestCase {
    
    @BeforeEach
    protected void setUp() throws Exception {
        HttpServiceTaskTestServer.setUp();
    }
}
