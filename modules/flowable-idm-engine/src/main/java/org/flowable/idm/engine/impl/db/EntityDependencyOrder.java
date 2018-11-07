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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.idm.engine.impl.persistence.entity.GroupEntityImpl;
import org.flowable.idm.engine.impl.persistence.entity.IdentityInfoEntityImpl;
import org.flowable.idm.engine.impl.persistence.entity.IdmByteArrayEntityImpl;
import org.flowable.idm.engine.impl.persistence.entity.IdmPropertyEntityImpl;
import org.flowable.idm.engine.impl.persistence.entity.MembershipEntityImpl;
import org.flowable.idm.engine.impl.persistence.entity.PrivilegeEntityImpl;
import org.flowable.idm.engine.impl.persistence.entity.PrivilegeMappingEntityImpl;
import org.flowable.idm.engine.impl.persistence.entity.TokenEntityImpl;
import org.flowable.idm.engine.impl.persistence.entity.UserEntityImpl;

public class EntityDependencyOrder {

    public static List<Class<? extends Entity>> DELETE_ORDER = new ArrayList<>();
    public static List<Class<? extends Entity>> INSERT_ORDER = new ArrayList<>();

    static {

        DELETE_ORDER.add(IdmPropertyEntityImpl.class);
        DELETE_ORDER.add(IdmByteArrayEntityImpl.class);
        DELETE_ORDER.add(MembershipEntityImpl.class);
        DELETE_ORDER.add(GroupEntityImpl.class);
        DELETE_ORDER.add(UserEntityImpl.class);
        DELETE_ORDER.add(IdentityInfoEntityImpl.class);
        DELETE_ORDER.add(TokenEntityImpl.class);
        DELETE_ORDER.add(PrivilegeMappingEntityImpl.class);
        DELETE_ORDER.add(PrivilegeEntityImpl.class);
        
        INSERT_ORDER = new ArrayList<>(DELETE_ORDER);
        Collections.reverse(INSERT_ORDER);

    }
    
}
