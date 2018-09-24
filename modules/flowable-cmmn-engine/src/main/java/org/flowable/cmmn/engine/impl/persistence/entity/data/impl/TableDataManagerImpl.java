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
package org.flowable.cmmn.engine.impl.persistence.entity.data.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnDeploymentEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnResourceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricMilestoneInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricPlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.SentryPartInstanceEntity;
import org.flowable.cmmn.engine.impl.persistence.entity.data.TableDataManager;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntity;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntity;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntity;

/**
 * @author Joram Barrez
 */
public class TableDataManagerImpl implements TableDataManager {

    public static Map<Class<? extends Entity>, String> entityToTableNameMap = new HashMap<>();

    static {
        entityToTableNameMap.put(CmmnDeploymentEntity.class, "ACT_CMMN_DEPLOYMENT");
        entityToTableNameMap.put(CmmnResourceEntity.class, "ACT_CMMN_DEPLOYMENT_RESOURCE");
        entityToTableNameMap.put(CaseDefinitionEntity.class, "ACT_CMMN_CASEDEF");
        entityToTableNameMap.put(CaseInstanceEntity.class, "ACT_CMMN_RU_CASE_INST");
        entityToTableNameMap.put(PlanItemInstanceEntity.class, "ACT_CMMN_RU_PLAN_ITEM_INST");
        entityToTableNameMap.put(SentryPartInstanceEntity.class, "ACT_CMMN_RU_SENTRY_PART_INST");
        entityToTableNameMap.put(MilestoneInstanceEntity.class, "ACT_CMMN_RU_MIL_INST");
        entityToTableNameMap.put(HistoricCaseInstanceEntity.class, "ACT_CMMN_HI_CASE_INST");
        entityToTableNameMap.put(HistoricMilestoneInstanceEntity.class, "ACT_CMMN_HI_MIL_INST");
        entityToTableNameMap.put(HistoricPlanItemInstanceEntity.class, "ACT_CMMN_HI_PLAN_ITEM_INST");
        entityToTableNameMap.put(VariableInstanceEntity.class, "ACT_RU_VARIABLE");
        entityToTableNameMap.put(HistoricVariableInstanceEntity.class, "ACT_HI_VARINST");
        entityToTableNameMap.put(TaskEntity.class, "ACT_RU_TASK");
        entityToTableNameMap.put(HistoricTaskInstanceEntity.class, "ACT_HI_TASKINST");
        entityToTableNameMap.put(IdentityLinkEntity.class, "ACT_RU_IDENTITYLINK");
        entityToTableNameMap.put(HistoricIdentityLinkEntity.class, "ACT_HI_IDENTITYLINK");
    }

    public TableDataManagerImpl() {
    }

    @Override
    public Map<String, Long> getTableCount() {
        Map<String, Long> counts = new HashMap<>();
        for (String table : getTablesPresentInDatabase()) {
            counts.put(table, (Long) CommandContextUtil.getDbSqlSession().selectOne("org.flowable.cmmn.engine.impl.TableData.selectTableCount", table));
        }
        return counts;
    }

    @Override
    public Collection<String> getTablesPresentInDatabase() {
        return entityToTableNameMap.values();
    }

    @Override
    public String getTableName(Class<?> entityClass, boolean withPrefix) {
        String databaseTablePrefix = CommandContextUtil.getDbSqlSession().getDbSqlSessionFactory().getDatabaseTablePrefix();
        String tableName = entityToTableNameMap.get(entityClass);
        if (withPrefix) {
            return databaseTablePrefix + tableName;
        } else {
            return tableName;
        }
    }

}
