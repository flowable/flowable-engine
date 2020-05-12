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
package org.flowable.idm.engine.impl.db;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.Privilege;
import org.flowable.idm.api.Token;
import org.flowable.idm.api.User;
import org.flowable.idm.engine.impl.persistence.entity.GroupEntity;
import org.flowable.idm.engine.impl.persistence.entity.IdentityInfoEntity;
import org.flowable.idm.engine.impl.persistence.entity.IdmByteArrayEntity;
import org.flowable.idm.engine.impl.persistence.entity.IdmPropertyEntity;
import org.flowable.idm.engine.impl.persistence.entity.MembershipEntity;
import org.flowable.idm.engine.impl.persistence.entity.PrivilegeEntity;
import org.flowable.idm.engine.impl.persistence.entity.TokenEntity;
import org.flowable.idm.engine.impl.persistence.entity.UserEntity;

/**
 * @author Filip Hrisafov
 */
public class EntityToTableMap {

    public static Map<Class<?>, String> apiTypeToTableNameMap = new HashMap<>();
    public static Map<Class<? extends Entity>, String> entityToTableNameMap = new HashMap<>();

    static {

        // Identity module
        entityToTableNameMap.put(GroupEntity.class, "ACT_ID_GROUP");
        entityToTableNameMap.put(MembershipEntity.class, "ACT_ID_MEMBERSHIP");
        entityToTableNameMap.put(UserEntity.class, "ACT_ID_USER");
        entityToTableNameMap.put(IdentityInfoEntity.class, "ACT_ID_INFO");
        entityToTableNameMap.put(TokenEntity.class, "ACT_ID_TOKEN");
        entityToTableNameMap.put(PrivilegeEntity.class, "ACT_ID_PRIV");

        // general
        entityToTableNameMap.put(IdmPropertyEntity.class, "ACT_ID_PROPERTY");
        entityToTableNameMap.put(IdmByteArrayEntity.class, "ACT_ID_BYTEARRAY");

        apiTypeToTableNameMap.put(Group.class, "ACT_ID_GROUP");
        apiTypeToTableNameMap.put(User.class, "ACT_ID_USER");
        apiTypeToTableNameMap.put(Token.class, "ACT_ID_TOKEN");
        apiTypeToTableNameMap.put(Privilege.class, "ACT_ID_PRIV");
    }

    public static String getTableName(Class<?> entityClass) {
        if (Entity.class.isAssignableFrom(entityClass)) {
            return entityToTableNameMap.get(entityClass);
        } else {
            return apiTypeToTableNameMap.get(entityClass);
        }
    }

}
