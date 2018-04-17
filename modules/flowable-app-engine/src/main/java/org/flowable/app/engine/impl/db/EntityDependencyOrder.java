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
package org.flowable.app.engine.impl.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.flowable.app.engine.impl.persistence.entity.AppDefinitionEntityImpl;
import org.flowable.app.engine.impl.persistence.entity.AppDeploymentEntityImpl;
import org.flowable.app.engine.impl.persistence.entity.AppResourceEntityImpl;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntityImpl;
import org.flowable.variable.service.impl.persistence.entity.VariableByteArrayEntityImpl;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntityImpl;

/**
 * @author Tijs Rademakers
 */
public class EntityDependencyOrder {

    public static List<Class<? extends Entity>> DELETE_ORDER = new ArrayList<>();
    public static List<Class<? extends Entity>> INSERT_ORDER = new ArrayList<>();

    static {

        DELETE_ORDER.add(VariableInstanceEntityImpl.class);
        DELETE_ORDER.add(VariableByteArrayEntityImpl.class);
        DELETE_ORDER.add(IdentityLinkEntityImpl.class);
        DELETE_ORDER.add(AppDefinitionEntityImpl.class);
        DELETE_ORDER.add(AppResourceEntityImpl.class);
        DELETE_ORDER.add(AppDeploymentEntityImpl.class);
        
        INSERT_ORDER = new ArrayList<>(DELETE_ORDER);
        Collections.reverse(INSERT_ORDER);

    }
    
}
