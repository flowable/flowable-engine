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
package org.flowable.cmmn.converter.export;

import org.flowable.cmmn.converter.CmmnXmlConstants;
import org.flowable.cmmn.model.CaseTask;
import org.flowable.cmmn.model.DecisionTask;
import org.flowable.cmmn.model.HumanTask;
import org.flowable.cmmn.model.Milestone;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.ProcessTask;
import org.flowable.cmmn.model.ServiceTask;
import org.flowable.cmmn.model.Stage;
import org.flowable.cmmn.model.Task;
import org.flowable.cmmn.model.TimerEventListener;
import org.flowable.cmmn.model.UserEventListener;

import javax.xml.stream.XMLStreamWriter;

public class PlanItemDefinitionExport implements CmmnXmlConstants {

    public static void writePlanItemDefinition(PlanItemDefinition planItemDefinition, XMLStreamWriter xtw) throws Exception {
        if (planItemDefinition instanceof Stage) {
            StageExport.writeStage((Stage) planItemDefinition, xtw);

        } else if (planItemDefinition instanceof HumanTask) {
            HumanTaskExport.writeHumanTask((HumanTask) planItemDefinition, xtw);

        } else if (planItemDefinition instanceof ProcessTask) {
            ProcessTaskExport.writeProcessTask((ProcessTask) planItemDefinition, xtw);

        } else if (planItemDefinition instanceof DecisionTask) {
            DecisionTaskExport.writeDecisionTask((DecisionTask) planItemDefinition, xtw);

        } else if (planItemDefinition instanceof CaseTask) {
            CaseTaskExport.writeCaseTask((CaseTask) planItemDefinition, xtw);

        } else if (planItemDefinition instanceof ServiceTask) {
            ServiceTaskExport.writeTask((ServiceTask) planItemDefinition, xtw);

        } else if (planItemDefinition instanceof Task) {
            TaskExport.writeTask((Task) planItemDefinition, xtw);

        } else if (planItemDefinition instanceof Milestone) {
            MilestoneExport.writeMilestone((Milestone) planItemDefinition, xtw);

        } else if (planItemDefinition instanceof TimerEventListener) {
            TimerEventListenerExport.writeTimerEventListener((TimerEventListener) planItemDefinition, xtw);

        } else if (planItemDefinition instanceof UserEventListener) {
            UserEventListenerExport.writeUserEventListener((UserEventListener) planItemDefinition, xtw);
        }
    }
}
