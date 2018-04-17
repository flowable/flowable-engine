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

package org.flowable.idm.api;

import java.util.List;

import org.flowable.common.engine.api.query.Query;

/**
 * Allows to programmatically query for {@link Group}s.
 * 
 * @author Joram Barrez
 */
public interface GroupQuery extends Query<GroupQuery, Group> {

    /** Only select {@link Group}s with the given id. */
    GroupQuery groupId(String groupId);

    /** Only select {@link Group}s with the given ids. */
    GroupQuery groupIds(List<String> groupIds);

    /** Only select {@link Group}s with the given name. */
    GroupQuery groupName(String groupName);

    /**
     * Only select {@link Group}s where the name matches the given parameter. The syntax to use is that of SQL, eg. %test%.
     */
    GroupQuery groupNameLike(String groupNameLike);

    /**
     * Only select {@link Group}s where the name matches the given parameter (ignoring case). The syntax to use is that of SQL, eg. %test%.
     */
    GroupQuery groupNameLikeIgnoreCase(String groupNameLikeIgnoreCase);

    /** Only select {@link Group}s which have the given type. */
    GroupQuery groupType(String groupType);

    /** Only selects {@link Group}s where the given user is a member of. */
    GroupQuery groupMember(String groupMemberUserId);

    /** Only selects {@link Group}s where the given users are a member of. */
    GroupQuery groupMembers(List<String> groupMemberUserIds);

    // sorting ////////////////////////////////////////////////////////

    /**
     * Order by group id (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    GroupQuery orderByGroupId();

    /**
     * Order by group name (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    GroupQuery orderByGroupName();

    /**
     * Order by group type (needs to be followed by {@link #asc()} or {@link #desc()}).
     */
    GroupQuery orderByGroupType();

}
