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

import org.flowable.engine.HistoryService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.test.FlowableRule;
import org.junit.Before;
import org.junit.Rule;

/**
 * @author Yvo Swillens
 */
public class AbstractFlowableDmnEngineConfiguratorTest {

    public static final String H2_TEST_JDBC_URL = "jdbc:h2:mem:flowable;DB_CLOSE_DELAY=1000";

    @Rule
    public FlowableRule flowableRule = new FlowableRule();

    protected static ProcessEngine cachedProcessEngine;
    protected RepositoryService repositoryService;
    protected RuntimeService runtimeService;
    protected HistoryService historyService;

    @Before
    public void initProcessEngine() {
        if (cachedProcessEngine == null) {
            cachedProcessEngine = flowableRule.getProcessEngine();
        }

        this.repositoryService = cachedProcessEngine.getRepositoryService();
        this.runtimeService = cachedProcessEngine.getRuntimeService();
        this.historyService = cachedProcessEngine.getHistoryService();
    }

}
