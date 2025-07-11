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

import org.flowable.common.engine.impl.test.EnsureCleanDb;
import org.flowable.common.engine.impl.test.LoggingExtension;
import org.flowable.dmn.api.DmnDecisionService;
import org.flowable.dmn.api.DmnHistoryService;
import org.flowable.dmn.api.DmnManagementService;
import org.flowable.dmn.api.DmnRepositoryService;
import org.flowable.dmn.engine.DmnEngine;
import org.flowable.dmn.engine.DmnEngineConfiguration;
import org.flowable.dmn.engine.impl.test.PluggableFlowableDmnExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

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
@ExtendWith(PluggableFlowableDmnExtension.class)
@ExtendWith(LoggingExtension.class)
@EnsureCleanDb(
        excludeTables = "ACT_GE_PROPERTY"
)
public class BaseFlowableDmnTest {

    protected DmnEngine dmnEngine;
    protected DmnEngineConfiguration dmnEngineConfiguration;
    protected DmnRepositoryService repositoryService;
    protected DmnDecisionService ruleService;
    protected DmnHistoryService historyService;
    protected DmnManagementService managementService;

    @BeforeEach
    public void initDmnEngine(DmnEngine dmnEngine) {
        this.dmnEngine = dmnEngine;
        this.dmnEngineConfiguration = dmnEngine.getDmnEngineConfiguration();
        this.repositoryService = dmnEngine.getDmnRepositoryService();
        this.ruleService = dmnEngine.getDmnDecisionService();
        this.historyService = dmnEngine.getDmnHistoryService();
        this.managementService = dmnEngine.getDmnManagementService();
    }

}
