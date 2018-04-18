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

import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.idm.api.event.FlowableIdmEventType;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.delegate.event.impl.FlowableIdmEventBuilder;
import org.flowable.idm.engine.impl.persistence.entity.data.MembershipDataManager;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class MembershipEntityManagerImpl extends AbstractEntityManager<MembershipEntity> implements MembershipEntityManager {

    protected MembershipDataManager membershipDataManager;

    public MembershipEntityManagerImpl(IdmEngineConfiguration idmEngineConfiguration, MembershipDataManager membershipDataManager) {
        super(idmEngineConfiguration);
        this.membershipDataManager = membershipDataManager;
    }

    @Override
    protected DataManager<MembershipEntity> getDataManager() {
        return membershipDataManager;
    }

    @Override
    public void createMembership(String userId, String groupId) {
        MembershipEntity membershipEntity = create();
        membershipEntity.setUserId(userId);
        membershipEntity.setGroupId(groupId);
        insert(membershipEntity, false);

        if (getEventDispatcher().isEnabled()) {
            getEventDispatcher().dispatchEvent(FlowableIdmEventBuilder.createMembershipEvent(FlowableIdmEventType.MEMBERSHIP_CREATED, groupId, userId));
        }
    }

    @Override
    public void deleteMembership(String userId, String groupId) {
        membershipDataManager.deleteMembership(userId, groupId);
        if (getEventDispatcher().isEnabled()) {
            getEventDispatcher().dispatchEvent(FlowableIdmEventBuilder.createMembershipEvent(FlowableIdmEventType.MEMBERSHIP_DELETED, groupId, userId));
        }
    }

    @Override
    public void deleteMembershipByGroupId(String groupId) {
        membershipDataManager.deleteMembershipByGroupId(groupId);
    }

    @Override
    public void deleteMembershipByUserId(String userId) {
        membershipDataManager.deleteMembershipByUserId(userId);
    }

    public MembershipDataManager getMembershipDataManager() {
        return membershipDataManager;
    }

    public void setMembershipDataManager(MembershipDataManager membershipDataManager) {
        this.membershipDataManager = membershipDataManager;
    }

}
