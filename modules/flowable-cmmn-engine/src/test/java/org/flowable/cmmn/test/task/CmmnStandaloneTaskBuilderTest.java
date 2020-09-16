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
package org.flowable.cmmn.test.task;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.cmmn.engine.test.FlowableCmmnTestCase;
import org.flowable.task.api.Task;
import org.junit.Test;

/**
 * @author Joram Barrez
 */
public class CmmnStandaloneTaskBuilderTest extends FlowableCmmnTestCase {

    @Test
    public void testCreateTaskWithBuilderAndScopes() {
        Task task = cmmnTaskService.createTaskBuilder().name("builderTask").
            scopeId("testScopeId").
            scopeType("testScopeType").
            create();

        try {
            Task taskFromQuery = cmmnTaskService.createTaskQuery().taskId(task.getId()).singleResult();
            assertThat(taskFromQuery.getScopeId()).isEqualTo("testScopeId");
            assertThat(taskFromQuery.getScopeType()).isEqualTo("testScopeType");
        } finally {
            cmmnTaskService.deleteTask(task.getId(), true);
        }
    }

    @Test
    public void testCreateTaskWithBuilderWithoutScopes() {
        Task task = cmmnTaskService.createTaskBuilder().name("builderTask").
            create();
        try {
            Task taskFromQuery = cmmnTaskService.createTaskQuery().taskId(task.getId()).singleResult();
            assertThat(taskFromQuery.getScopeId()).isNull();
            assertThat(taskFromQuery.getScopeType()).isNull();
        } finally {
            cmmnTaskService.deleteTask(task.getId(), true);
        }
    }

}
