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

package org.flowable.spring.test.autodeployment;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flowable.common.engine.impl.util.IoUtil;
import org.flowable.form.api.FormDefinition;
import org.flowable.form.api.FormDefinitionQuery;
import org.flowable.form.api.FormDeployment;
import org.flowable.form.api.FormDeploymentQuery;
import org.flowable.form.api.FormRepositoryService;
import org.flowable.form.engine.impl.test.AbstractTestCase;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class SpringAutoDeployTest extends AbstractTestCase {

    protected static final String CTX_PATH = "org/flowable/spring/test/autodeployment/SpringAutoDeployTest-context.xml";
    protected static final String CTX_NO_DROP_PATH = "org/flowable/spring/test/autodeployment/SpringAutoDeployTest-no-drop-context.xml";
    protected static final String CTX_CREATE_DROP_CLEAN_DB = "org/flowable/spring/test/autodeployment/SpringAutoDeployTest-create-drop-clean-db-context.xml";
    protected static final String CTX_DEPLOYMENT_MODE_DEFAULT = "org/flowable/spring/test/autodeployment/SpringAutoDeployTest-deploymentmode-default-context.xml";
    protected static final String CTX_DEPLOYMENT_MODE_SINGLE_RESOURCE = "org/flowable/spring/test/autodeployment/SpringAutoDeployTest-deploymentmode-single-resource-context.xml";
    protected static final String CTX_DEPLOYMENT_MODE_RESOURCE_PARENT_FOLDER = "org/flowable/spring/test/autodeployment/SpringAutoDeployTest-deploymentmode-resource-parent-folder-context.xml";

    protected ApplicationContext applicationContext;
    protected FormRepositoryService repositoryService;

    protected void createAppContext(String path) {
        this.applicationContext = new ClassPathXmlApplicationContext(path);
        this.repositoryService = applicationContext.getBean(FormRepositoryService.class);
    }

    @Override
    protected void tearDown() throws Exception {
        removeAllDeployments();
        this.applicationContext = null;
        this.repositoryService = null;
        super.tearDown();
    }

    public void testBasicFlowableSpringIntegration() {
        createAppContext("org/flowable/spring/test/autodeployment/SpringAutoDeployTest-context.xml");
        List<FormDefinition> formDefinitions = repositoryService.createFormDefinitionQuery().orderByFormDefinitionKey().asc().list();

        Set<String> formDefinitionKeys = new HashSet<>();
        for (FormDefinition formDefinition : formDefinitions) {
            formDefinitionKeys.add(formDefinition.getKey());
        }

        Set<String> expectedFormDefinitionKeys = new HashSet<>();
        expectedFormDefinitionKeys.add("form1");
        expectedFormDefinitionKeys.add("form2");

        assertEquals(expectedFormDefinitionKeys, formDefinitionKeys);
    }

    public void testNoRedeploymentForSpringContainerRestart() throws Exception {
        createAppContext(CTX_PATH);
        FormDeploymentQuery deploymentQuery = repositoryService.createDeploymentQuery();
        assertEquals(1, deploymentQuery.count());
        FormDefinitionQuery formDefinitionQuery = repositoryService.createFormDefinitionQuery();
        assertEquals(2, formDefinitionQuery.count());

        // Creating a new app context with same resources doesn't lead to more deployments
        new ClassPathXmlApplicationContext(CTX_NO_DROP_PATH);
        assertEquals(1, deploymentQuery.count());
        assertEquals(2, formDefinitionQuery.count());
    }

    // Updating the form file should lead to a new deployment when restarting the Spring container
    public void testResourceRedeploymentAfterFormDefinitionChange() throws Exception {
        createAppContext(CTX_PATH);
        assertEquals(1, repositoryService.createDeploymentQuery().count());
        ((AbstractXmlApplicationContext) applicationContext).destroy();

        String filePath = "org/flowable/spring/test/autodeployment/simple.form";
        String originalFormFileContent = IoUtil.readFileAsString(filePath);
        String updatedFormFileContent = originalFormFileContent.replace("My first form", "My first forms");
        assertTrue(updatedFormFileContent.length() > originalFormFileContent.length());
        IoUtil.writeStringToFile(updatedFormFileContent, filePath);

        // Classic produced/consumer problem here:
        // The file is already written in Java, but not yet completely persisted by the OS
        // Constructing the new app context reads the same file which is sometimes not yet fully written to disk
        Thread.sleep(2000);

        try {
            applicationContext = new ClassPathXmlApplicationContext(CTX_NO_DROP_PATH);
            repositoryService = (FormRepositoryService) applicationContext.getBean("formRepositoryService");
        } finally {
            // Reset file content such that future test are not seeing something funny
            IoUtil.writeStringToFile(originalFormFileContent, filePath);
        }

        // Assertions come AFTER the file write! Otherwise the form file is
        // messed up if the assertions fail.
        assertEquals(2, repositoryService.createDeploymentQuery().count());
        assertEquals(4, repositoryService.createFormDefinitionQuery().count());
    }

    public void testAutoDeployWithCreateDropOnCleanDb() {
        createAppContext(CTX_CREATE_DROP_CLEAN_DB);
        assertEquals(1, repositoryService.createDeploymentQuery().count());
        assertEquals(2, repositoryService.createFormDefinitionQuery().count());
    }

    public void testAutoDeployWithDeploymentModeDefault() {
        createAppContext(CTX_DEPLOYMENT_MODE_DEFAULT);
        assertEquals(1, repositoryService.createDeploymentQuery().count());
        assertEquals(2, repositoryService.createFormDefinitionQuery().count());
    }

    public void testAutoDeployWithDeploymentModeSingleResource() {
        createAppContext(CTX_DEPLOYMENT_MODE_SINGLE_RESOURCE);
        assertEquals(2, repositoryService.createDeploymentQuery().count());
        assertEquals(2, repositoryService.createFormDefinitionQuery().count());
    }

    public void testAutoDeployWithDeploymentModeResourceParentFolder() {
        createAppContext(CTX_DEPLOYMENT_MODE_RESOURCE_PARENT_FOLDER);
        assertEquals(2, repositoryService.createDeploymentQuery().count());
        assertEquals(3, repositoryService.createFormDefinitionQuery().count());
    }

    // --Helper methods
    // ----------------------------------------------------------

    private void removeAllDeployments() {
        for (FormDeployment deployment : repositoryService.createDeploymentQuery().list()) {
            repositoryService.deleteDeployment(deployment.getId());
        }
    }

}
