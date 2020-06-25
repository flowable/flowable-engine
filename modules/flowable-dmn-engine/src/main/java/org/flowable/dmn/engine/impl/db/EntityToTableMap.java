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
package org.flowable.dmn.engine.impl.db;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.dmn.engine.impl.persistence.entity.DecisionEntity;
import org.flowable.dmn.engine.impl.persistence.entity.DmnDeploymentEntity;
import org.flowable.dmn.engine.impl.persistence.entity.DmnResourceEntity;
import org.flowable.dmn.engine.impl.persistence.entity.HistoricDecisionExecutionEntity;

/**
 * @author Filip Hrisafov
 */
public class EntityToTableMap {

    public static Map<Class<?>, String> apiTypeToTableNameMap = new HashMap<>();
    public static Map<Class<? extends Entity>, String> entityToTableNameMap = new HashMap<>();

    static {

        // DMN module
        entityToTableNameMap.put(DmnDeploymentEntity.class, "ACT_DMN_DEPLOYMENT");
        entityToTableNameMap.put(DecisionEntity.class, "ACT_DMN_DECISION");
        entityToTableNameMap.put(DmnResourceEntity.class, "ACT_DMN_DEPLOYMENT_RESOURCE");
        entityToTableNameMap.put(HistoricDecisionExecutionEntity.class, "ACT_DMN_HI_DECISION_EXECUTION");
    }

    public static String getTableName(Class<?> entityClass) {
        if (Entity.class.isAssignableFrom(entityClass)) {
            return entityToTableNameMap.get(entityClass);
        } else {
            return apiTypeToTableNameMap.get(entityClass);
        }
    }

}
