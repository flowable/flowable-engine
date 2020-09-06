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
package org.flowable.cmmn.engine.impl.history.async.json.transformer;

import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getDateFromJson;
import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getStringFromJson;

import java.util.Collections;
import java.util.List;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.history.async.CmmnAsyncHistoryConstants;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.entitylink.api.history.HistoricEntityLinkService;
import org.flowable.entitylink.service.impl.persistence.entity.HistoricEntityLinkEntity;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
public class EntityLinkCreatedHistoryJsonTransformer extends AbstractHistoryJsonTransformer {

    public EntityLinkCreatedHistoryJsonTransformer(CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(cmmnEngineConfiguration);
    }
    
    @Override
    public List<String> getTypes() {
        return Collections.singletonList(CmmnAsyncHistoryConstants.TYPE_ENTITY_LINK_CREATED);
    }

    @Override
    public boolean isApplicable(ObjectNode historicalData, CommandContext commandContext) {
        return true;
    }

    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
        HistoricEntityLinkService historicEntityLinkService = cmmnEngineConfiguration.getEntityLinkServiceConfiguration().getHistoricEntityLinkService();
        HistoricEntityLinkEntity historicEntityLinkEntity = (HistoricEntityLinkEntity) historicEntityLinkService.createHistoricEntityLink();
        historicEntityLinkEntity.setId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_ID));
        historicEntityLinkEntity.setLinkType(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_ENTITY_LINK_TYPE));
        historicEntityLinkEntity.setCreateTime(getDateFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_CREATE_TIME));
        historicEntityLinkEntity.setScopeId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_SCOPE_ID));
        historicEntityLinkEntity.setSubScopeId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_SUB_SCOPE_ID));
        historicEntityLinkEntity.setScopeType(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_SCOPE_TYPE));
        historicEntityLinkEntity.setScopeDefinitionId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_SCOPE_DEFINITION_ID));
        historicEntityLinkEntity.setParentElementId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_PARENT_ELEMENT_ID));
        historicEntityLinkEntity.setReferenceScopeId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_REF_SCOPE_ID));
        historicEntityLinkEntity.setReferenceScopeType(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_REF_SCOPE_TYPE));
        historicEntityLinkEntity.setReferenceScopeDefinitionId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_REF_SCOPE_DEFINITION_ID));
        historicEntityLinkEntity.setRootScopeId(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_ROOT_SCOPE_ID));
        historicEntityLinkEntity.setRootScopeType(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_ROOT_SCOPE_TYPE));
        historicEntityLinkEntity.setHierarchyType(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_HIERARCHY_TYPE));
        
        historicEntityLinkService.insertHistoricEntityLink(historicEntityLinkEntity, false);
    }

}
