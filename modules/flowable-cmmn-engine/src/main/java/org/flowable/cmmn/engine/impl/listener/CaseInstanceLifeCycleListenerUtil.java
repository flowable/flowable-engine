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
package org.flowable.cmmn.engine.impl.listener;

import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.listener.CaseInstanceLifecycleListener;
import org.flowable.cmmn.api.repository.CaseDefinition;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.engine.impl.deployer.CmmnDeploymentManager;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.cmmn.model.Case;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.FlowableListener;
import org.flowable.common.engine.impl.interceptor.CommandContext;

/**
 * @author martin.grofcik
 * @author Joram Barrez
 */
public class CaseInstanceLifeCycleListenerUtil {

    public static void callLifecycleListeners(CommandContext commandContext, CaseInstance caseInstance, String oldState, String newState) {
        if (Objects.equals(oldState, newState)) {
            return;
        }

        // Lifecycle listeners on the case
        Case caseModel = getCaseModel(caseInstance.getCaseDefinitionId());
        List<FlowableListener> flowableListeners = caseModel.getLifecycleListeners();
        if (flowableListeners != null && !flowableListeners.isEmpty()) {

            CmmnListenerNotificationHelper listenerNotificationHelper = CommandContextUtil.getCmmnEngineConfiguration(commandContext)
                .getListenerNotificationHelper();
            for (FlowableListener flowableListener : flowableListeners) {
                if (stateMatches(flowableListener.getSourceState(), oldState) && stateMatches(flowableListener.getTargetState(), newState)) {
                    CaseInstanceLifecycleListener lifecycleListener = listenerNotificationHelper.createCaseLifecycleListener(flowableListener);
                    executeLifecycleListener(caseInstance, oldState, newState, lifecycleListener);
                }
            }
        }

        // Lifecycle listeners defined on the cmmn engine configuration
        List<CaseInstanceLifecycleListener> caseInstanceLifecycleListeners = CommandContextUtil
            .getCmmnEngineConfiguration(commandContext).getCaseInstanceLifecycleListeners();
        if (caseInstanceLifecycleListeners != null && !caseInstanceLifecycleListeners.isEmpty()) {
            for (CaseInstanceLifecycleListener caseInstanceLifecycleListener : caseInstanceLifecycleListeners) {
                executeLifecycleListener(caseInstance, oldState, newState, caseInstanceLifecycleListener);
            }
        }
    }

    protected static void executeLifecycleListener(CaseInstance caseInstance, String oldState, String newState,
        CaseInstanceLifecycleListener lifecycleListener) {
        if (lifecycleListenerMatches(lifecycleListener, oldState, newState)) {
            lifecycleListener.stateChanged(caseInstance, oldState, newState);
        }
    }

    protected static boolean lifecycleListenerMatches(CaseInstanceLifecycleListener lifecycleListener, String oldState, String newState) {
        return stateMatches(lifecycleListener.getSourceState(), oldState) && stateMatches(lifecycleListener.getTargetState(), newState);
    }

    protected static boolean stateMatches(String listenerExpectedState, String actualState) {
        return StringUtils.isEmpty(listenerExpectedState) || Objects.equals(actualState, listenerExpectedState);
    }

    protected static Case getCaseModel(String caseDefinitionId) {
        CmmnDeploymentManager deploymentManager = CommandContextUtil.getCmmnEngineConfiguration().getDeploymentManager();
        CaseDefinition caseDefinitionEntity = deploymentManager.findDeployedCaseDefinitionById(caseDefinitionId);
        CmmnModel cmmnModel = deploymentManager.resolveCaseDefinition(caseDefinitionEntity).getCmmnModel();
        return cmmnModel.getCaseById(caseDefinitionEntity.getKey());
    }

}
