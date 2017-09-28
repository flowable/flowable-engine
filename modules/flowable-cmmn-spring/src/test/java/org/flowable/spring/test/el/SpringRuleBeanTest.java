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

package org.flowable.spring.test.el;

import org.flowable.cmmn.engine.CmmnRepositoryService;
import org.flowable.cmmn.engine.CmmnRuntimeService;
import org.flowable.cmmn.engine.repository.CmmnDeployment;
import org.flowable.cmmn.engine.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.junit.After;
import org.junit.Assert;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Tijs Rademakers
 */
public class SpringRuleBeanTest extends FlowableCmmnTestCase {

    protected static final String CTX_PATH = "org/flowable/spring/test/el/SpringBeanTest-context.xml";

    protected ApplicationContext applicationContext;
    protected CmmnRepositoryService repositoryService;
    protected CmmnRuntimeService runtimeService;

    protected void createAppContext(String path) {
        this.applicationContext = new ClassPathXmlApplicationContext(path);
        this.repositoryService = applicationContext.getBean(CmmnRepositoryService.class);
        this.runtimeService = applicationContext.getBean(CmmnRuntimeService.class);
    }

    @After
    protected void tearDown() throws Exception {
        removeAllDeployments();
        this.applicationContext = null;
        this.repositoryService = null;
        this.runtimeService = null;
    }

    public void testSimpleCaseBean() {
        createAppContext(CTX_PATH);
        repositoryService.createDeployment().addClasspathResource("org/flowable/spring/test/el/springbean.dmn").deploy();
        
        CaseInstance caseInstance = runtimeService.createCaseInstanceBuilder()
                        .caseDefinitionKey("myCase")
                        .variable("input1", "John Doe")
                        .start();
        
        Assert.assertNotNull(caseInstance);
    }

    // --Helper methods
    // ----------------------------------------------------------

    private void removeAllDeployments() {
        for (CmmnDeployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeploymentAndRelatedData(deployment.getId());
        }
    }

}