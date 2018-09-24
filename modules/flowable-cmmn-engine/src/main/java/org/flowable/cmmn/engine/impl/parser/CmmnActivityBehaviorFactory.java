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
import org.flowable.cmmn.engine.impl.behavior.impl.CaseTaskActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.DecisionTaskActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.HumanTaskActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.MilestoneActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.PlanItemDelegateExpressionActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.PlanItemExpressionActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.ProcessTaskActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.ScriptTaskActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.StageActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.TaskActivityBehavior;
import org.flowable.cmmn.engine.impl.behavior.impl.TimerEventListenerActivityBehaviour;
import org.flowable.cmmn.engine.impl.behavior.impl.UserEventListenerActivityBehaviour;
import org.flowable.cmmn.engine.impl.delegate.CmmnClassDelegate;
import org.flowable.cmmn.model.CaseTask;
import org.flowable.cmmn.model.DecisionTask;
import org.flowable.cmmn.model.HumanTask;
import org.flowable.cmmn.model.Milestone;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.ProcessTask;
import org.flowable.cmmn.model.ScriptServiceTask;
import org.flowable.cmmn.model.ServiceTask;
import org.flowable.cmmn.model.Stage;
import org.flowable.cmmn.model.Task;
import org.flowable.cmmn.model.TimerEventListener;
import org.flowable.cmmn.model.UserEventListener;

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

    TimerEventListenerActivityBehaviour createTimerEventListenerActivityBehavior(PlanItem planItem, TimerEventListener timerEventListener);
    
    ScriptTaskActivityBehavior createScriptTaskActivityBehavior(PlanItem planItem, ScriptServiceTask task);

    UserEventListenerActivityBehaviour createUserEventListenerActivityBehavior(PlanItem planItem, UserEventListener userEventListener);

}
