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

public class PrivilegeMappingEntityImpl extends AbstractIdmEngineEntity implements PrivilegeMappingEntity {

    protected String privilegeId;
    protected String userId;
    protected String groupId;

    @Override
    public Object getPersistentState() {
        // Privilege mapping is immutable
        return PrivilegeMappingEntityImpl.class;
    }

    @Override
    public String getPrivilegeId() {
        return privilegeId;
    }

    @Override
    public void setPrivilegeId(String privilegeId) {
        this.privilegeId = privilegeId;
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
