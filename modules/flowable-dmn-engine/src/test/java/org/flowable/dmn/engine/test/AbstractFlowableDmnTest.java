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
package org.flowable.dmn.engine.test;

import org.flowable.dmn.api.DmnDecisionService;
import org.flowable.dmn.api.DmnHistoryService;
import org.flowable.dmn.api.DmnManagementService;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.junit.Before;
import org.junit.Rule;

/**
 * Parent class for internal Flowable DMN tests.
 * 
 * Boots up a dmn engine and caches it.
 * 
 * When using H2 and the default schema name, it will also boot the H2 webapp (reachable with browser on http://localhost:8082/)
 * 
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class AbstractFlowableDmnTest {

    public static final String H2_TEST_JDBC_URL = "jdbc:h2:mem:flowable;DB_CLOSE_DELAY=1000";

    @Rule
    public FlowableDmnRule flowableDmnRule = new FlowableDmnRule();

    protected static DmnEngine cachedDmnEngine;
    protected DmnEngineConfiguration dmnEngineConfiguration;
    protected DmnRepositoryService repositoryService;
    protected DmnDecisionService ruleService;
    protected DmnHistoryService dmnHistoryService;
    protected DmnManagementService managementService;

    @Before
    public void initDmnEngine() {
        if (cachedDmnEngine == null) {
            cachedDmnEngine = flowableDmnRule.getDmnEngine();
        }
        this.dmnEngineConfiguration = cachedDmnEngine.getDmnEngineConfiguration();
        this.repositoryService = cachedDmnEngine.getDmnRepositoryService();
        this.ruleService = cachedDmnEngine.getDmnDecisionService();
        this.dmnHistoryService = cachedDmnEngine.getDmnHistoryService();
        this.managementService = cachedDmnEngine.getDmnManagementService();
    }

}
