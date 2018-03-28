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
package org.flowable.cmmn.api.runtime;

import org.flowable.cmmn.model.CaseTask;
import org.flowable.cmmn.model.DecisionTask;
import org.flowable.cmmn.model.HttpServiceTask;
import org.flowable.cmmn.model.HumanTask;
import org.flowable.cmmn.model.Milestone;
import org.flowable.cmmn.model.PlanFragment;
import org.flowable.cmmn.model.ProcessTask;
import org.flowable.cmmn.model.ServiceTask;
import org.flowable.cmmn.model.Stage;
import org.flowable.cmmn.model.TimerEventListener;
import org.flowable.cmmn.model.UserEventListener;

/**
 * @author Joram Barrez
 */
public interface PlanItemDefinitionType {

    String STAGE = Stage.class.getSimpleName().toLowerCase();
    
    String PLAN_FRAGMENT = PlanFragment.class.getSimpleName().toLowerCase();
    
    String MILESTONE = Milestone.class.getSimpleName().toLowerCase();

    String TIMER_EVENT_LISTENER = TimerEventListener.class.getSimpleName().toLowerCase();

    String USER_EVENT_LISTENER = UserEventListener.class.getSimpleName().toLowerCase();
    
    String HUMAN_TASK = HumanTask.class.getSimpleName().toLowerCase();
    
    String CASE_TASK = CaseTask.class.getSimpleName().toLowerCase();
    
    String PROCESS_TASK = ProcessTask.class.getSimpleName().toLowerCase();
    
    String DECISION_TASK = DecisionTask.class.getSimpleName().toLowerCase();
    
    String SERVICE_TASK = ServiceTask.class.getSimpleName().toLowerCase();
    
    String HTTP_SERVICE_TASK = HttpServiceTask.class.getSimpleName().toLowerCase();

}
