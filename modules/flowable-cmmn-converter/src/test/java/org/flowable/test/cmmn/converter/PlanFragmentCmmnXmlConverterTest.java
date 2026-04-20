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
package org.flowable.test.cmmn.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Fail;
import org.flowable.cmmn.converter.util.PlanItemUtil;
import org.flowable.cmmn.model.Association;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.GraphicInfo;
import org.flowable.cmmn.model.PlanFragment;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.PlanItemDefinition;
import org.flowable.cmmn.model.Stage;
import org.flowable.test.cmmn.converter.util.CmmnXmlConverterTest;

/**
 * @author Joram Barrez
 */
public class PlanFragmentCmmnXmlConverterTest {


    @CmmnXmlConverterTest("org/flowable/test/cmmn/converter/plan-fragment.cmmn")
    public void validateModel(CmmnModel cmmnModel) {
        Stage planModel = cmmnModel.getPrimaryCase().getPlanModel();
        
        assertThat(cmmnModel.getAssociations()).hasSize(2);
        String entryAssociationId = null;
        String exitAssociationId = null;
        for (Association association : cmmnModel.getAssociations()) {
            if ("planItem2".equals(association.getSourceRef()) && "entryCriterion1".equals(association.getTargetRef())) {
                entryAssociationId = association.getId();
                
            } else if ("planItem3".equals(association.getSourceRef()) && "exitCriterion1".equals(association.getTargetRef())) {
                exitAssociationId = association.getId();
            }
        }
        
        assertThat(entryAssociationId).isNotNull();
        assertThat(cmmnModel.getFlowLocationGraphicInfo(entryAssociationId)).isNotNull();
        List<GraphicInfo> entryInfoList = cmmnModel.getFlowLocationGraphicInfo(entryAssociationId);
        assertThat(entryInfoList).hasSize(4);
        assertThat(exitAssociationId).isNotNull();
        assertThat(cmmnModel.getFlowLocationGraphicInfo(exitAssociationId)).isNotNull();
        List<GraphicInfo> exitInfoList = cmmnModel.getFlowLocationGraphicInfo(exitAssociationId);
        assertThat(exitInfoList).hasSize(4);

        // Assert parent-child relations

        List<PlanItem> rootLevelPlanItems = planModel.getPlanItems();
        assertThat(rootLevelPlanItems).extracting(PlanItem::getName).containsOnly("A", "PF1", "Stage one", "PF4");

        assertNoChildPlanItems(rootLevelPlanItems, "A");

        List<PlanItem> planFragment1ChildPlanItems = getChildPlanItems(rootLevelPlanItems, "PF1");
        assertThat(planFragment1ChildPlanItems).extracting(PlanItem::getName).containsOnly("B", "C");
        assertNoChildPlanItems(planFragment1ChildPlanItems, "B");
        assertNoChildPlanItems(planFragment1ChildPlanItems, "C");
        PlanItem planItemFragment = planModel.getPlanItem("planItem4");
        assertThat(planItemFragment.getDefinitionRef()).isEqualTo("expandedPlanFragment1");
        PlanFragment planFragment = (PlanFragment) planItemFragment.getPlanItemDefinition();
        assertThat(planFragment.getPlanItems()).hasSize(2);
        PlanItem taskPlanItem = planFragment.getPlanItem("planItem3");
        assertThat(taskPlanItem.getEntryCriteria()).hasSize(1);
        Criterion criterion = taskPlanItem.getEntryCriteria().iterator().next();
        assertThat(criterion.getId()).isEqualTo("entryCriterion1");
        assertThat(criterion.getIncomingAssociations()).hasSize(1);

        List<PlanItem> stageOneChildPlanItems = getChildPlanItems(rootLevelPlanItems, "Stage one");
        assertThat(stageOneChildPlanItems).extracting(PlanItem::getName).containsOnly("PF2", "F");
        assertNoChildPlanItems(stageOneChildPlanItems, "F");

        List<PlanItem> planFragment2ChildPlanItems = getChildPlanItems(stageOneChildPlanItems, "PF2");
        assertThat(planFragment2ChildPlanItems).extracting(PlanItem::getName).containsOnly("D", "PF3");
        assertNoChildPlanItems(planFragment2ChildPlanItems, "D");

        List<PlanItem> planFragment3ChildPlanItems = getChildPlanItems(planFragment2ChildPlanItems, "PF3");
        assertThat(planFragment3ChildPlanItems).extracting(PlanItem::getName).containsOnly("E");
        assertNoChildPlanItems(planFragment3ChildPlanItems, "E");

        List<PlanItem> planFragment4ChildPlanItems = getChildPlanItems(rootLevelPlanItems, "PF4");
        assertThat(planFragment4ChildPlanItems).extracting(PlanItem::getName).containsOnly("Stage two");

        List<PlanItem> stageTwoChildPlanItems = getChildPlanItems(planFragment4ChildPlanItems, "Stage two");
        assertThat(stageTwoChildPlanItems).extracting(PlanItem::getName).containsOnly("G", "PF5");
        assertNoChildPlanItems(stageTwoChildPlanItems, "G");

        List<PlanItem> planFragment5ChildPlanItems = getChildPlanItems(stageTwoChildPlanItems, "PF5");
        assertThat(planFragment5ChildPlanItems).extracting(PlanItem::getName).containsOnly("Stage three");

        List<PlanItem> stageThreeChildPlanItems = getChildPlanItems(planFragment5ChildPlanItems, "Stage three");
        assertThat(stageThreeChildPlanItems).extracting(PlanItem::getName).containsOnly("H");
        assertNoChildPlanItems(stageThreeChildPlanItems, "H");

        // Assert lifecycle flag

        for (String planItemName : Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H", "Stage one", "Stage two", "Stage three")) {
            assertPlanItemHasLifecycle(cmmnModel, planItemName, true);
        }

        for (String planFragmentName : Arrays.asList("PF1", "PF2", "PF3", "PF4", "PF5")) {
            assertPlanItemHasLifecycle(cmmnModel, planFragmentName, false);
        }
    }

    protected void assertPlanItemHasLifecycle(CmmnModel model, String name, boolean expected) {
        List<PlanItem> allChildPlanItems = PlanItemUtil.getAllChildPlanItems(model.getPrimaryCase().getPlanModel());
        Optional<PlanItem> optionalPlanItem = allChildPlanItems.stream().filter(p -> name.equals(p.getName())).findFirst();
        assertThat(optionalPlanItem.isPresent());
        PlanItem planItem = optionalPlanItem.get();

        assertThat(planItem).isNotNull();
        assertThat(planItem.isInstanceLifecycleEnabled()).isEqualTo(expected);
    }

    protected void assertNoChildPlanItems(List<PlanItem> planItems, String name) {
        PlanItem planItem = getPlanItem(planItems, name);
        assertThat(planItem).isNotNull();

        PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();
        if (planItemDefinition instanceof PlanFragment planFragment) {
            assertThat(planFragment.getPlanItems()).isEmpty();
        }
    }

    protected List<PlanItem> getChildPlanItems(List<PlanItem> planItems, String name) {
        PlanItem planItem = getPlanItem(planItems, name);
        PlanItemDefinition planItemDefinition = planItem.getPlanItemDefinition();
        assertThat(planItemDefinition).isInstanceOf(PlanFragment.class);
        if (planItemDefinition instanceof PlanFragment) {
            return ((PlanFragment) planItemDefinition).getPlanItems();
        }

        return Fail.fail("Programmatic error, should come here");
    }

    protected PlanItem getPlanItem(List<PlanItem> planItems, String name) {
        Optional<PlanItem> optionalPlanItem = planItems.stream().filter(p -> name.equals(p.getName())).findFirst();
        return optionalPlanItem.orElse(null);
    }

}
