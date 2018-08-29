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
package org.flowable.standalone.escapeclause;

import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProcessDefinitionQueryEscapeClauseTest extends AbstractEscapeClauseTestCase {

    private String deploymentOneId;
    private String deploymentTwoId;

    @BeforeEach
    protected void setUp() throws Exception {
        deploymentOneId = repositoryService
                .createDeployment()
                .tenantId("One%")
                .name("one%")
                .category("testCategory")
                .addClasspathResource("org/flowable/engine/test/repository/one%.bpmn20.xml")
                .deploy()
                .getId();

        deploymentTwoId = repositoryService
                .createDeployment()
                .tenantId("Two_")
                .name("two_")
                .addClasspathResource("org/flowable/engine/test/repository/two_.bpmn20.xml")
                .deploy()
                .getId();

    }

    @AfterEach
    protected void tearDown() throws Exception {
        repositoryService.deleteDeployment(deploymentOneId, true);
        repositoryService.deleteDeployment(deploymentTwoId, true);
    }

    @Test
    public void testQueryByNameLike() {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionNameLike("%\\%%");
        assertEquals("One%", query.singleResult().getName());
        assertEquals(1, query.list().size());
        assertEquals(1, query.count());

        query = repositoryService.createProcessDefinitionQuery().processDefinitionNameLike("%\\_%");
        assertEquals("Two_", query.singleResult().getName());
        assertEquals(1, query.list().size());
        assertEquals(1, query.count());
    }

    @Test
    public void testQueryByCategoryLike() {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionCategoryLike("%\\_%");
        assertEquals("Examples_", query.singleResult().getCategory());
        assertEquals(1, query.list().size());
        assertEquals(1, query.count());
    }

    @Test
    public void testQueryByKeyLike() {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionKeyLike("%\\_%");
        assertEquals("two_", query.singleResult().getKey());
        assertEquals(1, query.list().size());
        assertEquals(1, query.count());
    }

    @Test
    public void testQueryByResourceNameLike() {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionResourceNameLike("%\\%%");
        assertEquals("org/flowable/engine/test/repository/one%.bpmn20.xml", query.singleResult().getResourceName());
        assertEquals(1, query.list().size());
        assertEquals(1, query.count());

        query = repositoryService.createProcessDefinitionQuery().processDefinitionResourceNameLike("%\\_%");
        assertEquals("org/flowable/engine/test/repository/two_.bpmn20.xml", query.singleResult().getResourceName());
        assertEquals(1, query.list().size());
        assertEquals(1, query.count());
    }

    @Test
    public void testQueryByTenantIdLike() {
        ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery().processDefinitionTenantIdLike("%\\%%");
        assertEquals("One%", query.singleResult().getTenantId());
        assertEquals(1, query.list().size());
        assertEquals(1, query.count());

        query = repositoryService.createProcessDefinitionQuery().processDefinitionTenantIdLike("%\\_%");
        assertEquals("Two_", query.singleResult().getTenantId());
        assertEquals(1, query.list().size());
        assertEquals(1, query.count());

        query = repositoryService.createProcessDefinitionQuery().latestVersion().processDefinitionTenantIdLike("%\\%%");
        assertEquals("One%", query.singleResult().getTenantId());
        assertEquals(1, query.list().size());
        assertEquals(1, query.count());

        query = repositoryService.createProcessDefinitionQuery().latestVersion().processDefinitionTenantIdLike("%\\_%");
        assertEquals("Two_", query.singleResult().getTenantId());
        assertEquals(1, query.list().size());
        assertEquals(1, query.count());
    }
}
