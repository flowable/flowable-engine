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
package org.flowable.engine.task;

import java.util.Collection;
import java.util.Date;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.query.Query;


/**
 * @author David Lamas
 */
public interface CommentQuery extends Query<CommentQuery, Comment> {
    /**
     * Only select the comment with the given id.
     */
    CommentQuery commentId(String commentId);

    /**
     * Only select comments which have the given task id.
     */
    CommentQuery taskId(String taskId);

    /**
     * Only select comments which have the given process instance id.
     */
    CommentQuery processInstanceId(String processInstanceId);

    /**
     * Only select comments that have the given type.
     */
    CommentQuery type(String type);

    /**
     * Only select comments created by the given user.
     *
     * @throws FlowableIllegalArgumentException When query is executed and {@link #createdByOneOf(Collection)} has been
     *                                          executed on the query instance.
     */
    CommentQuery createdBy(String userId);

    /**
     * Only select comments created by one of the given users.
     *
     * @throws FlowableIllegalArgumentException When query is executed and {@link #createdBy(String)} has been executed
     *                                          on the query instance.
     */
    CommentQuery createdByOneOf(Collection<String> userIds);

    /**
     * Only select comments created before the given time.
     */
    CommentQuery createdBefore(Date beforeTime);

    /**
     * Only select comments created after the given time.
     */
    CommentQuery createdAfter(Date afterTime);

    /**
     * Only select comments created at the given time
     */
    CommentQuery createdOn(Date createTime);

    // ordering ////////////////////////////////////////////////////////////

    /**
     * Order by the time when the comment was created (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    CommentQuery orderByCreateTime();

    /**
     * Order by the user that created the comment (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    CommentQuery orderByUser();

    /**
     * Order by type (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    CommentQuery orderByType();

    /**
     * Order by task id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    CommentQuery orderByTaskId();

    /**
     * Order by process instance id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    CommentQuery orderByProcessInstanceId();
}
