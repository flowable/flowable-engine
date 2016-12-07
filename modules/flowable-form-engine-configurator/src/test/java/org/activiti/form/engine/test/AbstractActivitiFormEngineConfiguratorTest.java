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
package org.activiti.form.engine.test;

import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.test.ActivitiRule;
import org.activiti.form.api.FormRepositoryService;
import org.junit.Before;
import org.junit.Rule;

/**
 * @author Yvo Swillens
 */
public class AbstractActivitiFormEngineConfiguratorTest {

  public static String H2_TEST_JDBC_URL = "jdbc:h2:mem:activiti;DB_CLOSE_DELAY=1000";

  @Rule
  public ActivitiRule activitiRule = new ActivitiRule();

  protected static ProcessEngine cachedProcessEngine;
  protected RepositoryService repositoryService;
  protected FormRepositoryService formRepositoryService;

  @Before
  public void initProcessEngine() {
    if (cachedProcessEngine == null) {
      cachedProcessEngine = activitiRule.getProcessEngine();
    }

    this.repositoryService = cachedProcessEngine.getRepositoryService();
    this.formRepositoryService = cachedProcessEngine.getFormEngineRepositoryService();
  }

}
