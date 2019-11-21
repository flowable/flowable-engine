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

import org.flowable.cmmn.api.listener.CaseInstanceLifecycleListener;
import org.flowable.cmmn.api.listener.PlanItemInstanceLifecycleListener;
import org.flowable.cmmn.model.FlowableListener;
import org.flowable.task.service.delegate.TaskListener;

/**
 * @author Joram Barrez
 */
public interface CmmnListenerFactory {

    TaskListener createClassDelegateTaskListener(FlowableListener listener);

    TaskListener createExpressionTaskListener(FlowableListener listener);

    TaskListener createDelegateExpressionTaskListener(FlowableListener listener);

    PlanItemInstanceLifecycleListener createClassDelegateLifeCycleListener(FlowableListener listener);

    PlanItemInstanceLifecycleListener createExpressionLifeCycleListener(FlowableListener listener);

    PlanItemInstanceLifecycleListener createDelegateExpressionLifeCycleListener(FlowableListener listener);

    CaseInstanceLifecycleListener createClassDelegateCaseLifeCycleListener(FlowableListener listener);

    CaseInstanceLifecycleListener createExpressionCaseLifeCycleListener(FlowableListener listener);

    CaseInstanceLifecycleListener createDelegateExpressionCaseLifeCycleListener(FlowableListener listener);

}