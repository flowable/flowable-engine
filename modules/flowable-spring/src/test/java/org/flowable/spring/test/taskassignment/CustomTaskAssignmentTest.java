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
package org.flowable.spring.test.taskassignment;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.common.engine.impl.util.CollectionUtil;
import org.flowable.engine.test.Deployment;
import org.flowable.spring.impl.test.SpringFlowableTestCase;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Joram Barrez
 */
@ContextConfiguration("classpath:org/flowable/spring/test/taskassignment/taskassignment-context.xml")
public class CustomTaskAssignmentTest extends SpringFlowableTestCase {

    @Test
    @Deployment
    public void testSetAssigneeThroughSpringService() {
        runtimeService.startProcessInstanceByKey("assigneeThroughSpringService", CollectionUtil.singletonMap("emp", "fozzie"));
        assertThat(taskService.createTaskQuery().taskAssignee("Kermit The Frog").count()).isEqualTo(1);
    }

    @Test
    @Deployment
    public void testSetCandidateUsersThroughSpringService() {
        runtimeService.startProcessInstanceByKey("candidateUsersThroughSpringService", CollectionUtil.singletonMap("emp", "fozzie"));
        assertThat(taskService.createTaskQuery().taskCandidateUser("kermit").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskCandidateUser("fozzie").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskCandidateUser("gonzo").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskCandidateUser("mispiggy").count()).isZero();
    }

    @Test
    @Deployment
    public void testSetCandidateGroupsThroughSpringService() {
        runtimeService.startProcessInstanceByKey("candidateUsersThroughSpringService", CollectionUtil.singletonMap("emp", "fozzie"));
        assertThat(taskService.createTaskQuery().taskCandidateGroup("management").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskCandidateGroup("directors").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskCandidateGroup("accountancy").count()).isEqualTo(1);
        assertThat(taskService.createTaskQuery().taskCandidateGroup("sales").count()).isZero();
    }

}
