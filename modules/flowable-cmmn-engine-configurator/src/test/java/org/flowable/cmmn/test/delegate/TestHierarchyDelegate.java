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
package org.flowable.cmmn.test.delegate;

import java.util.List;

import org.flowable.cmmn.api.delegate.DelegatePlanItemInstance;
import org.flowable.cmmn.api.delegate.PlanItemJavaDelegate;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.entitylink.api.EntityLink;
import org.flowable.entitylink.api.EntityLinkService;
import org.flowable.entitylink.api.EntityLinkType;
import org.flowable.entitylink.service.EntityLinkServiceConfiguration;

public class TestHierarchyDelegate implements PlanItemJavaDelegate {

    @Override
    public void execute(DelegatePlanItemInstance planItemInstance) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration();
        EntityLinkServiceConfiguration entityLinkServiceConfiguration = (EntityLinkServiceConfiguration) cmmnEngineConfiguration
                        .getServiceConfigurations().get(EngineConfigurationConstants.KEY_ENTITY_LINK_SERVICE_CONFIG);
        EntityLinkService entityLinkService = entityLinkServiceConfiguration.getEntityLinkService();
        List<EntityLink> entityLinks = entityLinkService.findEntityLinksByReferenceScopeIdAndType(planItemInstance.getCaseInstanceId(), 
                        ScopeTypes.CMMN, EntityLinkType.CHILD);
        planItemInstance.setVariable("linkCount", entityLinks.size());
    }

}
