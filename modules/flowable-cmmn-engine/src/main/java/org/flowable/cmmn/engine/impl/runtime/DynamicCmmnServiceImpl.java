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
package org.flowable.cmmn.engine.impl.runtime;

import org.flowable.cmmn.api.DynamicCmmnService;
import org.flowable.cmmn.api.runtime.InjectedPlanItemInstanceBuilder;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.service.CommonEngineServiceImpl;

/**
 * Default implementation for dynamically modify running CMMN based case instances and plan items.
 *
 * @author Micha Kiener
 */
public class DynamicCmmnServiceImpl extends CommonEngineServiceImpl<CmmnEngineConfiguration> implements DynamicCmmnService {

    public DynamicCmmnServiceImpl(CmmnEngineConfiguration engineConfiguration) {
        super(engineConfiguration);
    }

    @Override
    public InjectedPlanItemInstanceBuilder createInjectedPlanItemInstanceBuilder(String stagePlanItemInstanceId) {
        PlanItemInstance planItemInstance = configuration.getPlanItemInstanceEntityManager().createPlanItemInstanceQuery()
            .planItemInstanceId(stagePlanItemInstanceId).singleResult();

        if (planItemInstance == null) {
            throw new FlowableIllegalArgumentException(
                "The stage plan item instance id " + stagePlanItemInstanceId + " could not be found or is no longer active.");
        }
        if (!planItemInstance.isStage()) {
            throw new FlowableIllegalArgumentException("A dynamically created plan item can only be injected into a running stage plan item.");
        }

        return new InjectedPlanItemInstanceBuilderImpl(commandExecutor, planItemInstance);
    }
}
