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

import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamWriter;

import org.flowable.cmmn.converter.CmmnXmlConstants;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.common.engine.api.FlowableException;

public class PlanItemDefinitionExport implements CmmnXmlConstants {
    protected static Map<String, AbstractPlanItemDefinitionExport> planItemDefinitionExporters = new HashMap<>();

    static {
        addPlanItemDefinitionExport(StageExport.getInstance());
        addPlanItemDefinitionExport(new TaskExport());
        addPlanItemDefinitionExport(new HumanTaskExport());
        addPlanItemDefinitionExport(new CaseTaskExport());
        addPlanItemDefinitionExport(new DecisionTaskExport());
        addPlanItemDefinitionExport(new ProcessTaskExport());
        addPlanItemDefinitionExport(new AbstractServiceTaskExport.ServiceTaskExport());
        addPlanItemDefinitionExport(new AbstractServiceTaskExport.HttpServiceTaskExport());
        addPlanItemDefinitionExport(new AbstractServiceTaskExport.ScriptServiceTaskExport());
        addPlanItemDefinitionExport(new CasePageTaskExport());
        addPlanItemDefinitionExport(new ExternalWorkerServiceTaskExport());
        addPlanItemDefinitionExport(new MilestoneExport());
        addPlanItemDefinitionExport(new GenericEventListenerExport());
        addPlanItemDefinitionExport(new SignalEventListenerExport());
        addPlanItemDefinitionExport(new TimerEventListenerExport());
        addPlanItemDefinitionExport(new UserEventListenerExport());
    }

    public static void addPlanItemDefinitionExport(AbstractPlanItemDefinitionExport exporter) {
        planItemDefinitionExporters.put(exporter.getExportablePlanItemDefinitionClass().getCanonicalName(), exporter);
    }

    public static void writePlanItemDefinition(CmmnModel model, PlanItemDefinition planItemDefinition, XMLStreamWriter xtw) throws Exception {
        AbstractPlanItemDefinitionExport exporter = determineExporter(planItemDefinition);
        if (exporter == null) {
            throw new FlowableException("Cannot find a PlanItemDefinitionExporter for '" + planItemDefinition.getClass().getCanonicalName() + "'");
        }
        exporter.writePlanItemDefinition(model, planItemDefinition, xtw);
    }

    protected static AbstractPlanItemDefinitionExport determineExporter(PlanItemDefinition planItemDefinition) {

        AbstractPlanItemDefinitionExport exporter = null;
        Class currentPlanItemDefinitionClass = planItemDefinition.getClass();

        while (exporter == null && !currentPlanItemDefinitionClass.equals(PlanItemDefinition.class)) {
            String exporterType = currentPlanItemDefinitionClass.getCanonicalName();
            exporter = planItemDefinitionExporters.get(exporterType);

            currentPlanItemDefinitionClass = currentPlanItemDefinitionClass.getSuperclass(); // loop will stop once PlanItemDefinition is reached, so only child hierarchies will be checked
        }

        return exporter;
    }
}
