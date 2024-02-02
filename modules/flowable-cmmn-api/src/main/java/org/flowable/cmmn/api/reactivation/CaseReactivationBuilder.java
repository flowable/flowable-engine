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
package org.flowable.cmmn.api.reactivation;

import java.util.Map;

import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.runtime.CaseInstance;

/**
 * The case reactivation builder is used to create all the necessary and optional information for an archived / finished case to be reactivated. It is obtained
 * through {@link CmmnHistoryService#createCaseReactivationBuilder(String)}.
 *
 * @author Micha Kiener
 */
public interface CaseReactivationBuilder {

    /**
     * Adds a variable to be added to the case before triggering the reactivation event.
     *
     * @param name the name of the variable to be added
     * @param value the value of the variable to be added
     * @return the builder for method chaining
     */
    CaseReactivationBuilder variable(String name, Object value);

    /**
     * Adds the map of variables to the case before triggering the reactivation event.
     *
     * @param variables the map of variables to be added to the case
     * @return the builder for method chaining
     */
    CaseReactivationBuilder variables(Map<String, Object> variables);

    /**
     * Adds a transient variable to the case before triggering the reactivation event which is available only during that first transaction.
     *
     * @param name the name of the variable to be added
     * @param value the value of the variable to be added
     * @return the builder for method chaining
     */
    CaseReactivationBuilder transientVariable(String name, Object value);

    /**
     * Adds a map of transient variables to the case before triggering the reactivation event which are available only during that first transaction.
     *
     * @param variables the map of variables to be added to the case
     * @return the builder for method chaining
     */
    CaseReactivationBuilder transientVariables(Map<String, Object> variables);

    /**
     * After having entered all necessary information for the reactivation, this method actually triggers the reactivation and returns the reactivated case
     * instance from the runtime.
     *
     * @return the reactivated case instance copied back to the runtime
     */
    CaseInstance reactivate();
}
