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
package org.flowable.spring.security;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.flowable.idm.api.Group;

/**
 * An immutable serializable implementation of {@link Group}
 * @author Filip Hrisafov
 */
public class GroupDetails implements Group, Serializable {

    private static final long serialVersionUID = 1L;

    protected final String id;
    protected final String name;
    protected final String type;

    protected GroupDetails(String id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        // Not supported
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        // Not supported
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String string) {
        // Not supported

    }

    public static GroupDetails create(Group group) {
        return new GroupDetails(group.getId(), group.getName(), group.getType());
    }

    public static List<GroupDetails> create(List<Group> groups) {
        List<GroupDetails> groupDetails = new ArrayList<>(groups.size());
        for (Group group : groups) {
            groupDetails.add(create(group));
        }
        return groupDetails;
    }
}
