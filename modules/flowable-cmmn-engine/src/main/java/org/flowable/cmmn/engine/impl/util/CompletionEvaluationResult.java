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
package org.flowable.cmmn.engine.impl.util;

import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;

/**
 * A class holding the result from evaluating a stage or case plan model for completion. Evaluation can happen on two modes: with autocompletion in mind or
 * without. The difference being whether there is still optional work available or not.
 *
 * @author Micha Kiener
 */
public class CompletionEvaluationResult {
    protected final boolean isCompletable;
    protected final boolean shouldBeCompleted;
    protected final PlanItemInstanceEntity planItemInstance;

    public CompletionEvaluationResult(boolean isCompletable, boolean shouldBeCompleted,
        PlanItemInstanceEntity planItemInstance) {
        this.isCompletable = isCompletable;
        this.shouldBeCompleted = shouldBeCompleted;
        this.planItemInstance = planItemInstance;
    }

    /**
     * Returns true, if the plan item (most likely a stage or a case plan model) is completable, meaning, there is no more active or required work to be done,
     * but it might still have available or enabled plan items.
     *
     * @return true if the plan item is completable
     */
    public boolean isCompletable() {
        return isCompletable;
    }

    /**
     * Returns true, if the plan item (most likely a stage or case plan model) should be completed according the state of all of its enclosed plan items, their
     * required rules, availability state and the plan items autocomplete mode. If autocomplete is activated, this flag will represent the same as the
     * completable one, if not, it might be different.
     *
     * @return whether the plan item should be completed
     */
    public boolean shouldBeCompleted() {
        return shouldBeCompleted;
    }

    /**
     * Returns the first plan item preventing the stage or case from being completed. This will be null, if {@link #isCompletable()} is true, otherwise it
     * contains the plan item which first prevented it from being completed.
     *
     * @return the plan item first preventing the case or stage from being completable, might be null if it is actually completable
     */
    public PlanItemInstanceEntity getPlanItemInstance() {
        return planItemInstance;
    }
}
