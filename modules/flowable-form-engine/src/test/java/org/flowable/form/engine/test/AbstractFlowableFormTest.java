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
package org.flowable.form.engine.test;

import org.flowable.common.engine.impl.test.LoggingExtension;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.api.FormService;
import org.flowable.form.engine.FormEngine;
import org.flowable.form.engine.FormEngineConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Parent class for internal Flowable Form tests.
 * 
 * Boots up a form engine and caches it.
 * 
 * When using H2 and the default schema name, it will also boot the H2 webapp (reachable with browser on http://localhost:8082/)
 * 
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
@ExtendWith(FlowableFormExtension.class)
@ExtendWith(LoggingExtension.class)
public class AbstractFlowableFormTest {

    public static String H2_TEST_JDBC_URL = "jdbc:h2:mem:flowableform;DB_CLOSE_DELAY=1000";

    protected FormEngine formEngine;
    protected FormEngineConfiguration formEngineConfiguration;
    protected FormRepositoryService repositoryService;
    protected FormService formService;

    @BeforeEach
    public void initFormEngine(FormEngine formEngine) {
        this.formEngine = formEngine;
        this.formEngineConfiguration = formEngine.getFormEngineConfiguration();
        this.repositoryService = formEngine.getFormRepositoryService();
        this.formService = formEngine.getFormService();
    }

}
