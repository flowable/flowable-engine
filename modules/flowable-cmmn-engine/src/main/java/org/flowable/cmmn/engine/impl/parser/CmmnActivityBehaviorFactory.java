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
package org.flowable.cmmn.engine.impl.parser;

import org.flowable.cmmn.engine.impl.behavior.CmmnActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.CasePageTaskActivityBehaviour;
import org.flowable.cmmn.engine.impl.behavior.impl.CaseTaskActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.DecisionTaskActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.EventRegistryEventListenerActivityBehaviour;
import org.flowable.cmmn.engine.impl.behavior.impl.ExternalWorkerTaskActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.GenericEventListenerActivityBehaviour;
import org.flowable.cmmn.engine.impl.behavior.impl.HumanTaskActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.MailActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.MilestoneActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.PlanItemDelegateExpressionActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.PlanItemExpressionActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.ProcessTaskActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.ScriptTaskActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.SendEventActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.SignalEventListenerActivityBehaviour;
import org.flowable.cmmn.engine.impl.behavior.impl.StageActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.TaskActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.TimerEventListenerActivityBehaviour;
import org.flowable.cmmn.engine.impl.behavior.impl.UserEventListenerActivityBehaviour;
import org.flowable.cmmn.engine.impl.behavior.impl.VariableEventListenerActivityBehaviour;
import org.flowable.cmmn.engine.impl.delegate.CmmnClassDelegate;
import org.flowable.cmmn.model.CasePageTask;
import org.flowable.cmmn.model.CaseTask;
import org.flowable.cmmn.model.DecisionTask;
import org.flowable.cmmn.model.ExternalWorkerServiceTask;
import org.flowable.cmmn.model.GenericEventListener;
import org.flowable.cmmn.model.HumanTask;
import org.flowable.cmmn.model.Milestone;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.ProcessTask;
import org.flowable.cmmn.model.ScriptServiceTask;
import org.flowable.cmmn.model.SendEventServiceTask;
import org.flowable.cmmn.model.ServiceTask;
import org.flowable.cmmn.model.SignalEventListener;
import org.flowable.cmmn.model.Stage;
import org.flowable.cmmn.model.Task;
import org.flowable.cmmn.model.TimerEventListener;
import org.flowable.cmmn.model.UserEventListener;
import org.flowable.cmmn.model.VariableEventListener;

/**
 * @author Joram Barrez
 */
public interface CmmnActivityBehaviorFactory {

    StageActivityBehavior createStageActivityBehavior(PlanItem planItem, Stage stage);

    MilestoneActivityBehavior createMilestoneActivityBehavior(PlanItem planItem, Milestone milestone);

    TaskActivityBehavior createTaskActivityBehavior(PlanItem planItem, Task task);

    HumanTaskActivityBehavior createHumanTaskActivityBehavior(PlanItem planItem, HumanTask humanTask);

    CaseTaskActivityBehavior createCaseTaskActivityBehavior(PlanItem planItem, CaseTask caseTask);

    ProcessTaskActivityBehavior createProcessTaskActivityBehavior(PlanItem planItem, ProcessTask processTask);

    CmmnClassDelegate createCmmnClassDelegate(PlanItem planItem, ServiceTask task);

    PlanItemExpressionActivityBehavior createPlanItemExpressionActivityBehavior(PlanItem planItem, ServiceTask task);

    PlanItemDelegateExpressionActivityBehavior createPlanItemDelegateExpressionActivityBehavior(PlanItem planItem, ServiceTask task);

    DecisionTaskActivityBehavior createDecisionTaskActivityBehavior(PlanItem planItem, DecisionTask decisionTask);

    CmmnActivityBehavior createHttpActivityBehavior(PlanItem planItem, ServiceTask task);

    MailActivityBehavior createEmailActivityBehavior(PlanItem planItem, ServiceTask task);

    SendEventActivityBehavior createSendEventActivityBehavior(PlanItem planItem, SendEventServiceTask sendEventServiceTask);

    ExternalWorkerTaskActivityBehavior createExternalWorkerActivityBehaviour(PlanItem planItem, ExternalWorkerServiceTask externalWorkerServiceTask);

    TimerEventListenerActivityBehaviour createTimerEventListenerActivityBehavior(PlanItem planItem, TimerEventListener timerEventListener);
    
    ScriptTaskActivityBehavior createScriptTaskActivityBehavior(PlanItem planItem, ScriptServiceTask task);
    
    CasePageTaskActivityBehaviour createCasePageTaskActivityBehaviour(PlanItem planItem, CasePageTask task);

    UserEventListenerActivityBehaviour createUserEventListenerActivityBehavior(PlanItem planItem, UserEventListener userEventListener);
    
    SignalEventListenerActivityBehaviour createSignalEventListenerActivityBehavior(PlanItem planItem, SignalEventListener signalEventListener);
    
    GenericEventListenerActivityBehaviour createGenericEventListenerActivityBehavior(PlanItem planItem, GenericEventListener genericEventListener);

    EventRegistryEventListenerActivityBehaviour createEventRegistryEventListenerActivityBehaviour(PlanItem planItem, GenericEventListener genericEventListener);
    
    VariableEventListenerActivityBehaviour createVariableEventListenerActivityBehaviour(PlanItem planItem, VariableEventListener variableEventListener);

}
