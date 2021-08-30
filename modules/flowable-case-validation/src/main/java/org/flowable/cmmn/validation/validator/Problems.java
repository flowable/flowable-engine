/*
 * Licensed under the Apache License, Version 2.0 (the "License");
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
 *
 */

package org.flowable.cmmn.validation.validator;

/**
 * @author Calin Cerchez
 */
public interface Problems {

    String DECISION_TASK_REFERENCE_MISSING = "flowable-decision-task-missing-decision-table-or-decision-service";

    String HUMAN_TASK_LISTENER_IMPLEMENTATION_MISSING = "flowable-humantask-listener-implementation-missing";

    String PLAN_MODEL_EMPTY = "flowable-plan-model-empty";
}
