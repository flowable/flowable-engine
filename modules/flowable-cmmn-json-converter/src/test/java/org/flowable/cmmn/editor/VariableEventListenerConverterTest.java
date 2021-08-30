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
package org.flowable.cmmn.editor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.flowable.cmmn.model.Association;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.GraphicInfo;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.PlanItemTransition;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.SentryOnPart;
import org.flowable.cmmn.model.Stage;
import org.flowable.cmmn.model.VariableEventListener;

public class VariableEventListenerConverterTest extends AbstractConverterTest {

    @Override
    protected String getResource() {
        return "test.variableEventListenerModel.json";
    }

    @Override
    protected void validateModel(CmmnModel cmmnModel) {
        Stage model = cmmnModel.getPrimaryCase().getPlanModel();
        assertThat(model.getPlanItemDefinitionMap())
                .containsOnlyKeys("taskA", "taskB", "startTaskAUserEvent", "stopTaskBUserEvent");

        Map<String, Sentry> sentries = model.getSentries().stream()
                .collect(Collectors.toMap(Sentry::getName, Function.identity(), (s1, s2) -> s1));
        assertThat(sentries).hasSize(2);
        Sentry sentry = sentries.get("entryTaskASentry");
        assertThat(sentry).isNotNull();
        List<SentryOnPart> onParts = sentry.getOnParts();
        assertThat(onParts).hasSize(1);
        SentryOnPart onPart = onParts.get(0);
        assertThat(onPart.getStandardEvent()).isEqualTo(PlanItemTransition.OCCUR);
        assertThat(onPart.getSource().getPlanItemDefinition().getId()).isEqualTo("startTaskAUserEvent");
        PlanItem task = model.findPlanItemInPlanFragmentOrUpwards(model.findPlanItemDefinitionInStageOrUpwards("taskA").getPlanItemRef());
        List<Criterion> criterions = task.getEntryCriteria();
        assertThat(criterions).hasSize(1);
        assertThat(sentry.getId()).isEqualTo(criterions.get(0).getSentryRef());

        sentry = sentries.get("exitTaskBSentry");
        assertThat(sentry).isNotNull();
        onParts = sentry.getOnParts();
        assertThat(onParts).hasSize(1);
        onPart = onParts.get(0);
        assertThat(onPart.getStandardEvent()).isEqualTo(PlanItemTransition.OCCUR);
        assertThat(onPart.getSource().getPlanItemDefinition().getId()).isEqualTo("stopTaskBUserEvent");
        task = model.findPlanItemInPlanFragmentOrUpwards(model.findPlanItemDefinitionInStageOrUpwards("taskB").getPlanItemRef());
        criterions = task.getExitCriteria();
        assertThat(criterions).hasSize(1);
        assertThat(sentry.getId()).isEqualTo(criterions.get(0).getSentryRef());
        
        PlanItemDefinition planItemDefinition = model.findPlanItemDefinitionInStageOrDownwards("startTaskAUserEvent");
        assertThat(planItemDefinition).isInstanceOf(VariableEventListener.class);
        VariableEventListener variableEventListener = (VariableEventListener) planItemDefinition;
        assertThat(variableEventListener.getVariableName()).isEqualTo("test");
        assertThat(variableEventListener.getVariableChangeType()).isEqualTo("create");

        planItemDefinition = model.findPlanItemDefinitionInStageOrDownwards("stopTaskBUserEvent");
        assertThat(planItemDefinition).isInstanceOf(VariableEventListener.class);
        variableEventListener = (VariableEventListener) planItemDefinition;
        assertThat(variableEventListener.getVariableName()).isEqualTo("test2");
        assertThat(variableEventListener.getVariableChangeType()).isEqualTo("all");
        
        List<Association> associations = cmmnModel.getAssociations();
        assertThat(associations.size()).isEqualTo(2);
        Map<String, Association> associationMap = new HashMap<>();
        for (Association association : associations) {
            associationMap.put(association.getId(), association);
        }
        
        Association association = associationMap.get("association1");
        PlanItemDefinition sourceItemDefinition = ((PlanItem) association.getSourceElement()).getPlanItemDefinition();
        assertThat(sourceItemDefinition).isInstanceOf(VariableEventListener.class);
        
        assertThat(association.getTargetElement()).isInstanceOf(Criterion.class);
        
        List<GraphicInfo> graphicInfoList = cmmnModel.getFlowLocationGraphicInfo("association1");
        assertThat(graphicInfoList.size()).isEqualTo(2);
        
        association = associationMap.get("association2");
        
        assertThat(association.getSourceElement()).isInstanceOf(Criterion.class);
        
        PlanItemDefinition targetItemDefinition = ((PlanItem) association.getTargetElement()).getPlanItemDefinition();
        assertThat(targetItemDefinition).isInstanceOf(VariableEventListener.class);
        
        graphicInfoList = cmmnModel.getFlowLocationGraphicInfo("association2");
        assertThat(graphicInfoList.size()).isEqualTo(2);
    }

}