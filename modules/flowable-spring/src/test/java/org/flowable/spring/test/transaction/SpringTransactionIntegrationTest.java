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

package org.flowable.spring.test.transaction;

import javax.sql.DataSource;

import org.flowable.bpmn.exceptions.XMLException;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.spring.impl.test.SpringFlowableTestCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Tom Baeyens
 */
@ContextConfiguration("classpath:org/flowable/spring/test/transaction/SpringTransactionIntegrationTest-context.xml")
public class SpringTransactionIntegrationTest extends SpringFlowableTestCase {

    @Autowired
    protected UserBean userBean;

    @Autowired
    protected DeployBean deployBean;

    @Autowired
    protected DataSource dataSource;

    @Test
    @Deployment
    public void testBasicFlowableSpringIntegration() {
        userBean.hello();

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().singleResult();
        assertEquals("Hello from Printer!", runtimeService.getVariable(processInstance.getId(), "myVar"));
    }

    @Test
    @Deployment
    public void testRollbackTransactionOnFlowableException() {

        // Create a table that the userBean is supposed to fill with some data
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("create table MY_TABLE (MY_TEXT varchar);");

        // The hello() method will start the process. The process will wait in a
        // user task
        userBean.hello();
        assertEquals(Long.valueOf(0), jdbcTemplate.queryForObject("select count(*) from MY_TABLE", Long.class));

        // The completeTask() method will write a record to the 'MY_TABLE' table
        // and complete the user task
        try {
            userBean.completeTask(taskService.createTaskQuery().singleResult().getId());
            fail();
        } catch (Exception e) {
        }

        // Since the service task after the user tasks throws an exception, both
        // the record and the process must be rolled back !
        assertEquals("My Task", taskService.createTaskQuery().singleResult().getName());
        assertEquals(Long.valueOf(0), jdbcTemplate.queryForObject("select count(*) from MY_TABLE", Long.class));

        // Cleanup
        jdbcTemplate.execute("drop table MY_TABLE if exists;");
    }

    @Test
    public void testRollBackOnDeployment() {
        // The second process should fail. None of the processes should be
        // deployed, the first one should be rolled back
        assertEquals(0, repositoryService.createProcessDefinitionQuery().count());
        try {
            deployBean.deployProcesses();
            fail();
        } catch (XMLException e) {
            // Parse exception should happen
        }
        assertEquals(0, repositoryService.createProcessDefinitionQuery().count());
    }

}
