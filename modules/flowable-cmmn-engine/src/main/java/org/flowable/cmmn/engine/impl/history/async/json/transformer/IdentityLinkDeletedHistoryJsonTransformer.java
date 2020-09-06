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

import static org.flowable.job.service.impl.history.async.util.AsyncHistoryJsonUtil.getStringFromJson;

import java.util.Collections;
import java.util.List;

import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.history.async.CmmnAsyncHistoryConstants;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.identitylink.service.HistoricIdentityLinkService;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntity;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Joram Barrez
 */
public class IdentityLinkDeletedHistoryJsonTransformer extends AbstractHistoryJsonTransformer {

    public IdentityLinkDeletedHistoryJsonTransformer(CmmnEngineConfiguration cmmnEngineConfiguration) {
        super(cmmnEngineConfiguration);
    }
    
    @Override
    public List<String> getTypes() {
        return Collections.singletonList(CmmnAsyncHistoryConstants.TYPE_IDENTITY_LINK_DELETED);
    }

    @Override
    public boolean isApplicable(ObjectNode historicalData, CommandContext commandContext) {
        return getHistoricIdentityLinkEntity(historicalData, commandContext) != null;
    }
    
    protected HistoricIdentityLinkEntity getHistoricIdentityLinkEntity(ObjectNode historicalData, CommandContext commandContext) {
        return cmmnEngineConfiguration.getIdentityLinkServiceConfiguration().getHistoricIdentityLinkService()
                .getHistoricIdentityLink(getStringFromJson(historicalData, CmmnAsyncHistoryConstants.FIELD_ID));
    }

    @Override
    public void transformJson(HistoryJobEntity job, ObjectNode historicalData, CommandContext commandContext) {
        HistoricIdentityLinkService historicIdentityLinkService = cmmnEngineConfiguration.getIdentityLinkServiceConfiguration().getHistoricIdentityLinkService();
        HistoricIdentityLinkEntity historicIdentityLinkEntity = getHistoricIdentityLinkEntity(historicalData, commandContext);
        if (historicIdentityLinkEntity != null) {
            historicIdentityLinkService.deleteHistoricIdentityLink(historicIdentityLinkEntity);
        }
    }

}
