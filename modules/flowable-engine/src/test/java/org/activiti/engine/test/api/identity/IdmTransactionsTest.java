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
package org.activiti.engine.test.api.identity;

import java.util.List;

import org.activiti.engine.IdentityService;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.delegate.TaskListener;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.test.PluggableActivitiTestCase;
import org.activiti.engine.task.Task;
import org.activiti.engine.test.Deployment;
import org.activiti.idm.api.User;

/**
 * @author Joram Barrez
 */
public class IdmTransactionsTest extends PluggableActivitiTestCase {
  
  @Override
  protected void tearDown() throws Exception {
    
    List<User> allUsers = identityService.createUserQuery().list();
    for (User user : allUsers) {
      identityService.deleteUser(user.getId());
    }
    
    super.tearDown();
  }
  
  @Deployment
  public void testCommitOnNoException() {
    
    // No users should exist prior to this test
    assertEquals(0, identityService.createUserQuery().list().size());
    
    runtimeService.startProcessInstanceByKey("testProcess");
    Task task = taskService.createTaskQuery().singleResult();
    
    taskService.complete(task.getId());
    assertEquals(1, identityService.createUserQuery().list().size());
    
  }
  
  @Deployment
  public void testTransactionRolledBackOnException() {
    
    // No users should exist prior to this test
    assertEquals(0, identityService.createUserQuery().list().size());
    
    runtimeService.startProcessInstanceByKey("testProcess");
    Task task = taskService.createTaskQuery().singleResult();
    
    // Completing the task throws an exception
    try {
      taskService.complete(task.getId());
      fail();
    } catch (Exception e) {
      // Exception expected
    }
    
    // Should have rolled back to task
    assertNotNull(taskService.createTaskQuery().singleResult());
    assertEquals(0L, historyService.createHistoricProcessInstanceQuery().finished().count());
    
    // The logic in the tasklistener (creating a new user) should rolled back too: 
    // no new user should have been created
    assertEquals(0, identityService.createUserQuery().list().size());
    
  }
  
  public static class NoopDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
    }
    
  }
  
  public static class TestExceptionThrowingDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
      throw new RuntimeException("Fail!");
    }
    
  }
  
  public static class TestCreateUserTaskListener implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
      IdentityService identityService = Context.getProcessEngineConfiguration().getIdentityService();
      User user = identityService.newUser("Kermit");
      user.setFirstName("Mr");
      user.setLastName("Kermit");
      identityService.saveUser(user);
    }
    
  }

}
