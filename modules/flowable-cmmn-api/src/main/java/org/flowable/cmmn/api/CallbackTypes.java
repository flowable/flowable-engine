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
package org.flowable.cmmn.api;

import org.flowable.common.engine.api.constant.ReferenceTypes;

/**
 * A callback type is set on an entity that needs to 'call back' to some other entity,
 * typically when the entity is completed or deleted.
 *
 * For example, given a cmmn case with a process task, the process instance will have
 * a call back id (the plan item instance id of the process task) and a call back type
 * indicating that it's a child process.
 *
 * Note that typically a 'reference id' and 'reference type' is also set on the calling
 * side. In this example, the plan item instance would get the id of the process
 * and the same reference type (hence why the reference type constant is also duplicated
 * in the {@link org.flowable.common.engine.api.constant.ReferenceTypes} class.
 *
 * @author Joram Barrez
 */
public interface CallbackTypes {

    // The same constant is used on the entity call back as for the reference on the calling side.

    /**
     * Child case instance of a case instance
     */
    String PLAN_ITEM_CHILD_CASE = ReferenceTypes.PLAN_ITEM_CHILD_CASE;

    /**
     * Child process instance of a case instance
     */
    String PLAN_ITEM_CHILD_PROCESS = ReferenceTypes.PLAN_ITEM_CHILD_PROCESS;

    /**
     * Child case instance of a process instance
     */
    String EXECUTION_CHILD_CASE = ReferenceTypes.EXECUTION_CHILD_CASE;

    String CASE_ADHOC_CHILD = "cmmn-1.1-child";

}
