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
package org.flowable.idm.engine.impl.persistence.entity;

import java.io.Serializable;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class MembershipEntityImpl extends AbstractIdmEngineNoRevisionEntity implements MembershipEntity, Serializable {

    private static final long serialVersionUID = 1L;

    protected String userId;
    protected String groupId;

    public MembershipEntityImpl() {

    }

    @Override
    public Object getPersistentState() {
        // membership is not updatable
        return MembershipEntityImpl.class;
    }

    @Override
    public String getId() {
        // membership doesn't have an id, returning a fake one to make the internals work
        return userId + groupId;
    }

    @Override
    public void setId(String id) {
        // membership doesn't have an id
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String getGroupId() {
        return groupId;
    }

    @Override
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

}
