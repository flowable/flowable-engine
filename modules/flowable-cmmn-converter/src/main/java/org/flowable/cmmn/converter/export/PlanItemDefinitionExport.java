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
import org.flowable.engine.common.api.FlowableException;

import javax.xml.stream.XMLStreamWriter;
import java.util.HashMap;
import java.util.Map;

public class PlanItemDefinitionExport implements CmmnXmlConstants {
    protected static Map<String, AbstractPlanItemDefinitionExport> planItemDefinitionExporters = new HashMap<>();

    static {
        addPlanItemDefinitionExport(new StageExport());
        addPlanItemDefinitionExport(new TaskExport());
        addPlanItemDefinitionExport(new HumanTaskExport());
        addPlanItemDefinitionExport(new CaseTaskExport());
        addPlanItemDefinitionExport(new DecisionTaskExport());
        addPlanItemDefinitionExport(new ProcessTaskExport());
        addPlanItemDefinitionExport(new ServiceTaskExport());
        addPlanItemDefinitionExport(new MilestoneExport());
        addPlanItemDefinitionExport(new TimerEventListenerExport());
    }

    public static void addPlanItemDefinitionExport(AbstractPlanItemDefinitionExport exporter) {
        planItemDefinitionExporters.put(exporter.getExportablePlanItemDefinitionClass().getCanonicalName(), exporter);
    }


    public static void writePlanItemDefinition(PlanItemDefinition planItemDefinition, XMLStreamWriter xtw) throws Exception {

        String exporterType = planItemDefinition.getClass().getCanonicalName();
        AbstractPlanItemDefinitionExport exporter = planItemDefinitionExporters.get(exporterType);
        if (exporter == null) {
            throw new FlowableException("Cannot find a PlanItemDefinitionExporter for '"+exporterType+"'");
        }
        exporter.writePlanItemDefinition(planItemDefinition, xtw);
/*
        if (planItemDefinition instanceof Stage) {
            exporterType = Stage.class.getCanonicalName();
//            StageExport.writeStage((Stage) planItemDefinition, xtw);

        } else if (planItemDefinition instanceof HumanTask) {
            exporterType = HumanTask.class.getCanonicalName();
//            HumanTaskExport.writeHumanTask((HumanTask) planItemDefinition, xtw);

        } else if (planItemDefinition instanceof ProcessTask) {
            exporterType = ProcessTask.class.getCanonicalName();
//            ProcessTaskExport.writeProcessTask((ProcessTask) planItemDefinition, xtw);

        } else if (planItemDefinition instanceof DecisionTask) {
            exporterType = DecisionTask.class.getCanonicalName();
//            DecisionTaskExport.writeDecisionTask((DecisionTask) planItemDefinition, xtw);

        } else if (planItemDefinition instanceof CaseTask) {
            exporterType = CaseTask.class.getCanonicalName();
//            CaseTaskExport.writeCaseTask((CaseTask) planItemDefinition, xtw);

        } else if (planItemDefinition instanceof ServiceTask) {
            exporterType = ServiceTask.class.getCanonicalName();
//            ServiceTaskExport.writeTask((ServiceTask) planItemDefinition, xtw);

        } else if (planItemDefinition instanceof Task) {
            exporterType = Task.class.getCanonicalName();
//            TaskExport.writeTask((Task) planItemDefinition, xtw);

        } else if (planItemDefinition instanceof Milestone) {
            exporterType = Milestone.class.getCanonicalName();
//            MilestoneExport.writeMilestone((Milestone) planItemDefinition, xtw);

        } else if (planItemDefinition instanceof TimerEventListener) {
            exporterType = TimerEventListener.class.getCanonicalName();
//            TimerEventListenerExport.writeTimerEventListener((TimerEventListener) planItemDefinition, xtw);

        }
        */
/*
        if (exporterType != null) {
            AbstractPlanItemDefinitionExport abstractPlanItemDefinitionExport = planItemDefinitionExporters.get(exporterType);
            abstractPlanItemDefinitionExport.writePlanItemDefinition(planItemDefinition, xtw);
        } else {
            throw new FlowableException("Cannot find a PlanItemDefinitionExporter for '"+planItemDefinition.getClass()+"'");
        }
*/
    }
}
