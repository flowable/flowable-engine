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
package org.flowable.cmmn.engine.impl.behavior.impl;

import static org.flowable.common.engine.impl.util.ExceptionUtil.sneakyThrow;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import org.flowable.cmmn.api.delegate.PlanItemFutureJavaDelegate;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.behavior.CoreCmmnActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CmmnLoggingSessionUtil;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.logging.LoggingSessionConstants;

/**
 * @author Filip Hrisafov
 */
public class PlanItemFutureJavaDelegateActivityBehavior extends CoreCmmnActivityBehavior {

    protected PlanItemFutureJavaDelegate<Object> planItemJavaDelegate;

    public PlanItemFutureJavaDelegateActivityBehavior(PlanItemFutureJavaDelegate<?> planItemJavaDelegate) {
        this.planItemJavaDelegate = (PlanItemFutureJavaDelegate<Object>) planItemJavaDelegate;
    }

    @Override
    public void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);
        if (cmmnEngineConfiguration.isLoggingSessionEnabled()) {
            CmmnLoggingSessionUtil.addLoggingData(LoggingSessionConstants.TYPE_SERVICE_TASK_ENTER,
                    "Executing service task with java class " + planItemJavaDelegate.getClass().getName(), 
                    planItemInstanceEntity, cmmnEngineConfiguration.getObjectMapper());
        }

        CompletableFuture<Object> future = planItemJavaDelegate.execute(planItemInstanceEntity, cmmnEngineConfiguration.getAsyncTaskInvoker());

        CommandContextUtil.getAgenda(commandContext)
                .planFutureOperation(future, new DelegateCompleteAction(planItemInstanceEntity, cmmnEngineConfiguration.isLoggingSessionEnabled()));
    }

    protected class DelegateCompleteAction implements BiConsumer<Object, Throwable> {

        protected final PlanItemInstanceEntity planItemInstance;
        protected final boolean loggingSessionEnabled;

        public DelegateCompleteAction(PlanItemInstanceEntity planItemInstance, boolean loggingSessionEnabled) {
            this.planItemInstance = planItemInstance;
            this.loggingSessionEnabled = loggingSessionEnabled;
        }

        @Override
        public void accept(Object value, Throwable throwable) {
            if (throwable == null) {
                planItemJavaDelegate.afterExecution(planItemInstance, value);
                if (loggingSessionEnabled) {
                    CmmnEngineConfiguration cmmnEngineConfiguration = CommandContextUtil.getCmmnEngineConfiguration();
                    CmmnLoggingSessionUtil.addLoggingData(LoggingSessionConstants.TYPE_SERVICE_TASK_EXIT, 
                            "Executed service task with java class " + planItemJavaDelegate.getClass().getName(), 
                            planItemInstance, cmmnEngineConfiguration.getObjectMapper());
                }

                CommandContextUtil.getAgenda().planCompletePlanItemInstanceOperation(planItemInstance);
            } else {
                sneakyThrow(throwable);
            }
        }
    }

}
