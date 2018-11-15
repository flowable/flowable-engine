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
package org.flowable.cmmn.converter.util;

import java.util.ArrayList;
import java.util.List;

import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.Stage;

/**
 * @author Joram Barrez
 */
public class PlanItemUtil {

    /**
     * Returns all parent plan items, ordered from direct parent plan item to outermost.
     */
    public static List<PlanItem> getAllParentPlanItems(PlanItem planItem) {
        List<PlanItem> parentPlanItems = new ArrayList<>();
        internalGetParentPlanItems(planItem, parentPlanItems);
        return parentPlanItems;
    }

    protected static void internalGetParentPlanItems(PlanItem planItem, List<PlanItem> parentPlanItems) {
        Stage parentStage = planItem.getParentStage();
        if (parentStage != null && !parentStage.isPlanModel()) {
            PlanItem parentPlanItem = parentStage.findPlanItemForPlanItemDefinitionInPlanFragmentOrUpwards(parentStage.getId());
            if (parentPlanItem != null) {
                parentPlanItems.add(parentPlanItem);
                internalGetParentPlanItems(parentPlanItem, parentPlanItems);
            }
        }
    }

}
