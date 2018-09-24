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

package org.flowable.engine.test.api.app;

import java.io.InputStream;

import org.flowable.engine.app.AppModel;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.junit.jupiter.api.Test;

/**
 * @author Tijs Rademakers
 */
public class AppResourceDeploymentTest extends PluggableFlowableTestCase {

    @Test
    public void testSingleAppResource() {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("org/flowable/engine/test/api/app/test.app");
        Deployment deployment = repositoryService.createDeployment().addInputStream("test.app", inputStream).deploy();

        Object appObject = repositoryService.getAppResourceObject(deployment.getId());
        assertNotNull(appObject);
        assertTrue(appObject instanceof AppModel);
        AppModel appModel = (AppModel) appObject;
        assertEquals("testTheme", appModel.getTheme());
        assertEquals("testIcon", appModel.getIcon());

        appModel = repositoryService.getAppResourceModel(deployment.getId());
        assertEquals("testTheme", appModel.getTheme());
        assertEquals("testIcon", appModel.getIcon());

        repositoryService.deleteDeployment(deployment.getId(), true);

        try {
            appObject = repositoryService.getAppResourceObject(deployment.getId());
            fail("exception excepted");
        } catch (Exception e) {
            // expected exception
        }
    }

    @Test
    public void testAppResourceWithProcessDefinition() {
        InputStream appInputStream = this.getClass().getClassLoader().getResourceAsStream("org/flowable/engine/test/api/app/test.app");
        InputStream bpmnInputStream = this.getClass().getClassLoader().getResourceAsStream("org/flowable/engine/test/repository/one.bpmn20.xml");

        Deployment deployment = repositoryService.createDeployment()
                .addInputStream("test.app", appInputStream)
                .addInputStream("one.bpmn20.xml", bpmnInputStream)
                .deploy();

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey("one").singleResult();
        assertEquals("one", processDefinition.getKey());
        assertEquals(deployment.getId(), processDefinition.getDeploymentId());

        Object appObject = repositoryService.getAppResourceObject(deployment.getId());
        assertNotNull(appObject);
        assertTrue(appObject instanceof AppModel);
        AppModel appModel = (AppModel) appObject;
        assertEquals("testTheme", appModel.getTheme());
        assertEquals("testIcon", appModel.getIcon());

        appModel = repositoryService.getAppResourceModel(deployment.getId());
        assertEquals("testTheme", appModel.getTheme());
        assertEquals("testIcon", appModel.getIcon());

        repositoryService.deleteDeployment(deployment.getId(), true);

        try {
            appObject = repositoryService.getAppResourceObject(deployment.getId());
            fail("exception excepted");
        } catch (Exception e) {
            // expected exception
        }
    }

}
