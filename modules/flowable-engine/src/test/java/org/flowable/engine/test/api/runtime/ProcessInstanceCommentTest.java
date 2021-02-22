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
package org.flowable.engine.test.api.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.test.HistoryTestHelper;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Comment;
import org.flowable.engine.test.Deployment;
import org.junit.jupiter.api.Test;

/**
 * @author Joram Barrez
 */
public class ProcessInstanceCommentTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testAddCommentToProcessInstance() {
        if (HistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, processEngineConfiguration)) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcessInstanceComment");

            taskService.addComment(null, processInstance.getId(), "Hello World");

            List<Comment> comments = taskService.getProcessInstanceComments(processInstance.getId());
            assertThat(comments).hasSize(1);

            List<Comment> commentsByType = taskService.getProcessInstanceComments(processInstance.getId(), "comment");
            assertThat(commentsByType).hasSize(1);

            commentsByType = taskService.getProcessInstanceComments(processInstance.getId(), "noThisType");
            assertThat(commentsByType).isEmpty();

            // Suspend process instance
            runtimeService.suspendProcessInstanceById(processInstance.getId());
            assertThatThrownBy(() -> taskService.addComment(null, processInstance.getId(), "Hello World 2"))
                    .isInstanceOf(FlowableException.class)
                    .hasMessageContaining("Cannot add a comment to a suspended execution");

            // Delete comments again
            taskService.deleteComments(null, processInstance.getId());
        }
    }

}
