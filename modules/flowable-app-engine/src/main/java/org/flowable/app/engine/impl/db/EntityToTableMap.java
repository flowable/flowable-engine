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

import java.util.HashMap;
import java.util.Map;

import org.flowable.app.engine.impl.persistence.entity.AppDefinitionEntity;
import org.flowable.app.engine.impl.persistence.entity.AppDeploymentEntity;
import org.flowable.app.engine.impl.persistence.entity.AppResourceEntity;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Filip Hrisafov
 */
public class EntityToTableMap {

    public static Map<Class<? extends Entity>, String> entityToTableNameMap = new HashMap<>();

    static {
        entityToTableNameMap.put(AppDeploymentEntity.class, "ACT_APP_DEPLOYMENT");
        entityToTableNameMap.put(AppResourceEntity.class, "ACT_APP_DEPLOYMENT_RESOURCE");
        entityToTableNameMap.put(AppDefinitionEntity.class, "ACT_APP_APPDEF");

        entityToTableNameMap.put(VariableInstanceEntity.class, "ACT_RU_VARIABLE");
        entityToTableNameMap.put(IdentityLinkEntity.class, "ACT_RU_IDENTITYLINK");
    }

}
