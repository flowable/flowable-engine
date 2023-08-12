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
package org.flowable.engine.test.api.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.task.CommentQuery;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.task.Comment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author David Lamas
 */
public class CommentQueryTest extends PluggableFlowableTestCase {
    private List<String> taskIds;
    private List<String> processIds;
    private List<String> commentIds;
    private Deployment deployment;

    @BeforeEach
    protected void setUp() throws ParseException {
        deployment = repositoryService.createDeployment().addClasspathResource("org/flowable/engine/test/api/runtime/oneTaskProcess.bpmn20.xml").deploy();

        Authentication.setAuthenticatedUserId("userId");
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
        processEngineConfiguration.getClock().setCurrentTime(sdf.parse("01/01/2021 00:00:00"));

        Pair<List<String>, List<String>> tasks = generateCommentsForTasks();
        Pair<List<String>, List<String>> processes = generateCommentsForProcesses();

        taskIds = tasks.getLeft();
        commentIds = tasks.getRight();

        processIds = processes.getLeft();
        commentIds.addAll(processes.getRight());
    }

    @AfterEach
    public void tearDown() {
        Authentication.setAuthenticatedUserId(null);
        taskService.deleteTasks(taskIds, true);
        deleteDeployment(deployment.getId());
    }

    @Test
    public void testQueryNoCriteria() {
        CommentQuery query = taskService.createCommentQuery();
        assertThat(query.count()).isEqualTo(5);
        assertThat(query.list()).hasSize(5);
        assertThatThrownBy(query::singleResult).isExactlyInstanceOf(FlowableException.class);
    }

    @Test
    public void testQueryByCommentId() {
        CommentQuery query = taskService.createCommentQuery().commentId(commentIds.get(0));
        assertThat(query.singleResult()).isNotNull();
        assertThat(query.list()).hasSize(1);
        assertThat(query.count()).isEqualTo(1);
    }

    @Test
    public void testQueryByInvalidCommentId() {
        CommentQuery query = taskService.createCommentQuery().commentId("invalid comment");
        assertThat(query.singleResult()).isNull();
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();
    }

    @Test
    public void testQueryByTaskId() {
        CommentQuery query = taskService.createCommentQuery().taskId(taskIds.get(0));
        assertThat(query.singleResult()).isNotNull();
        assertThat(query.list()).hasSize(1);
        assertThat(query.count()).isEqualTo(1);
    }

    @Test
    public void testQueryByInvalidTaskId() {
        CommentQuery query = taskService.createCommentQuery().taskId("invalid task");
        assertThat(query.singleResult()).isNull();
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();
    }

    @Test
    public void testQueryByProcessInstanceId() {
        CommentQuery query = taskService.createCommentQuery().processInstanceId(processIds.get(0));
        assertThat(query.singleResult()).isNotNull();
        assertThat(query.list()).hasSize(1);
        assertThat(query.count()).isEqualTo(1);
    }

    @Test
    public void testQueryByInvalidProcessInstanceId() {
        CommentQuery query = taskService.createCommentQuery().processInstanceId("invalid process");
        assertThat(query.singleResult()).isNull();
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();
    }

    @Test
    public void testQueryByType() {
        // Create a comment with a custom type
        taskService.addComment(taskIds.get(0), null, "customtype", "message");

        CommentQuery query = taskService.createCommentQuery().type("customtype");
        assertThat(query.singleResult()).isNotNull();
        assertThat(query.list()).hasSize(1);
        assertThat(query.count()).isEqualTo(1);
    }

    @Test
    public void testQueryByInvalidType() {
        CommentQuery query = taskService.createCommentQuery().type("invalid type");
        assertThat(query.singleResult()).isNull();
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();
    }

    @Test
    public void testQueryByCreatedBy() {
        CommentQuery query = taskService.createCommentQuery().createdBy("userId");
        assertThat(query.list()).hasSize(5);
        assertThat(query.count()).isEqualTo(5);

        Authentication.setAuthenticatedUserId("kermit");
        taskService.addComment(taskIds.get(0), null, "comment by kermit");
        query = taskService.createCommentQuery().createdBy("kermit");
        assertThat(query.list()).hasSize(1);
        assertThat(query.count()).isEqualTo(1);
    }

    @Test
    public void testQueryByInvalidCreatedBy() {
        CommentQuery query = taskService.createCommentQuery().createdBy("invalid user");
        assertThat(query.singleResult()).isNull();
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();
    }

    @Test
    public void testQueryByCreatedByOneOf() {
        CommentQuery query = taskService.createCommentQuery().createdByOneOf(Collections.singletonList("userId"));
        assertThat(query.list()).hasSize(5);
        assertThat(query.count()).isEqualTo(5);

        Authentication.setAuthenticatedUserId("kermit");
        taskService.addComment(taskIds.get(0), null, "comment by kermit");

        query = taskService.createCommentQuery().createdByOneOf(Arrays.asList("userId", "kermit"));
        assertThat(query.list()).hasSize(6);
        assertThat(query.count()).isEqualTo(6);

        query = taskService.createCommentQuery().createdByOneOf(Collections.singletonList("invalid user"));
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();
    }

    @Test
    public void testQueryByCreatedBefore() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");

        // Should give all comments (5)
        Date before = sdf.parse("02/01/2021 00:00:00");

        CommentQuery query = taskService.createCommentQuery().createdBefore(before);
        assertThat(query.list()).hasSize(5);
        assertThat(query.count()).isEqualTo(5);

        // Should give 0 comments
        before = sdf.parse("01/01/2021 00:00:00");
        query = taskService.createCommentQuery().createdBefore(before);
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();
    }

    @Test
    public void testQueryByCreatedAfter() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");

        // Should give all comments (5)
        Date after = sdf.parse("31/12/2020 00:00:00");

        CommentQuery query = taskService.createCommentQuery().createdAfter(after);
        assertThat(query.list()).hasSize(5);
        assertThat(query.count()).isEqualTo(5);

        // Should give 0 comments
        after = sdf.parse("02/01/2021 00:00:00");
        query = taskService.createCommentQuery().createdAfter(after);
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();
    }

    @Test
    public void testQueryByCreatedOn() throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");

        // Should give all comments (5)
        Date createdOn = sdf.parse("01/01/2021 00:00:00");

        CommentQuery query = taskService.createCommentQuery().createdOn(createdOn);
        assertThat(query.list()).hasSize(5);
        assertThat(query.count()).isEqualTo(5);

        // Should give 0 comments
        createdOn = sdf.parse("02/01/2021 00:00:00");
        query = taskService.createCommentQuery().createdOn(createdOn);
        assertThat(query.list()).isEmpty();
        assertThat(query.count()).isZero();
    }

    @Test
    public void testQuerySorting() {
        assertThat(taskService.createCommentQuery().orderByTaskId().asc().list()).hasSize(5);
        assertThat(taskService.createCommentQuery().orderByProcessInstanceId().asc().list()).hasSize(5);
        assertThat(taskService.createCommentQuery().orderByType().asc().list()).hasSize(5);
        assertThat(taskService.createCommentQuery().orderByCreateTime().asc().list()).hasSize(5);
        assertThat(taskService.createCommentQuery().orderByUser().asc().list()).hasSize(5);

        assertThat(taskService.createCommentQuery().orderByTaskId().desc().list()).hasSize(5);
        assertThat(taskService.createCommentQuery().orderByProcessInstanceId().desc().list()).hasSize(5);
        assertThat(taskService.createCommentQuery().orderByType().desc().list()).hasSize(5);
        assertThat(taskService.createCommentQuery().orderByCreateTime().desc().list()).hasSize(5);
        assertThat(taskService.createCommentQuery().orderByUser().desc().list()).hasSize(5);

        assertThat(taskService.createCommentQuery().orderByTaskId().type("comment").asc().list()).hasSize(5);
        assertThat(taskService.createCommentQuery().orderByTaskId().type("comment").desc().list()).hasSize(5);
    }

    private Pair<List<String>, List<String>> generateCommentsForTasks() {
        List<String> taskIds = new ArrayList<>();
        List<String> commentIds = new ArrayList<>();
        // Create 2 tasks and 1 comment for each task
        for (int i = 0; i < 2; i++) {
            Task task = taskService.newTask();
            task.setName("New task");
            taskService.saveTask(task);

            Comment comment = taskService.addComment(task.getId(), null, "Comment " + i + " for task");
            taskIds.add(task.getId());
            commentIds.add(comment.getId());
        }
        return Pair.of(taskIds, commentIds);
    }

    private Pair<List<String>, List<String>> generateCommentsForProcesses() {
        List<String> processInstances = new ArrayList<>();
        List<String> commentIds = new ArrayList<>();
        // Create 3 processes and 1 comment for each process
        for (int i = 0; i < 3; i++) {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");

            Comment comment = taskService.addComment(null, processInstance.getId(), "Comment " + i + " for process");
            processInstances.add(processInstance.getId());
            commentIds.add(comment.getId());
        }
        return Pair.of(processInstances, commentIds);
    }
}
