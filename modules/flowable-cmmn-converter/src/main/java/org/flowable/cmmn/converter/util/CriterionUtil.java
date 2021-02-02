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

import java.util.List;

import org.flowable.cmmn.model.Criterion;
import org.flowable.cmmn.model.HasEntryCriteria;
import org.flowable.cmmn.model.HasExitCriteria;
import org.flowable.cmmn.model.PlanItem;
import org.flowable.cmmn.model.Sentry;
import org.flowable.cmmn.model.SentryOnPart;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class CriterionUtil {

    public static String generateEntryCriterionId(HasEntryCriteria hasEntryCriteria) {
        return "entryCriterion_" + hasEntryCriteria.getId() + "_" + (hasEntryCriteria.getEntryCriteria().size() + 1);
    }

    public static String generateExitCriterionId(HasExitCriteria hasExitCriteria) {
        return "exitCriterion_" + hasExitCriteria.getId() + "_" + (hasExitCriteria.getExitCriteria().size() + 1);
    }

    public static boolean planItemHasOneEntryCriterionDependingOnPlanItem(PlanItem planItemToCheck, PlanItem planItem, String event) {
        List<Criterion> entryCriteria = planItemToCheck.getEntryCriteria();
        if (!entryCriteria.isEmpty()) {
            for (Criterion criterion : entryCriteria) {
                if (criterionHasOnPartDependingOnPlanItem(criterion, planItem, event)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean criterionHasOnPartDependingOnPlanItem(Criterion criterion, PlanItem planItem, String event) {
        Sentry sentry = criterion.getSentry();
        if (sentry != null) {
            for (SentryOnPart sentryOnPart : sentry.getOnParts()) {
                if (sentryOnPart.getSource().getId().equals(planItem.getId()) && sentryOnPart.getStandardEvent().equals(event)) {
                    return true;
                }
            }
        }
        return false;
    }

}
