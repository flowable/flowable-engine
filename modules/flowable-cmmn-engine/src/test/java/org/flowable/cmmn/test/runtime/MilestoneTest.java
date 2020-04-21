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
package org.flowable.cmmn.test.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class MilestoneTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testMilestoneVariable() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder().caseDefinitionKey("testMilestoneVariable").start();
        List<Task> tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).extracting(Task::getName).containsExactly("A");

        // Completing A will reach milestone M1, which sets a variable that activates the second stage
        cmmnTaskService.complete(tasks.get(0).getId());
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).extracting(Task::getName).containsExactly("B");

        // Completing B will reach milestone M2, which sets a variable that activates task C
        cmmnTaskService.complete(tasks.get(0).getId());
        tasks = cmmnTaskService.createTaskQuery().caseInstanceId(caseInstance.getId()).list();
        assertThat(tasks).extracting(Task::getName).containsExactly("C");

        cmmnTaskService.complete(tasks.get(0).getId());
        assertCaseInstanceEnded(caseInstance);
    }

}
