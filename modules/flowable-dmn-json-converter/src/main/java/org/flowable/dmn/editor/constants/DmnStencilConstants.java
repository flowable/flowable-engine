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
package org.flowable.dmn.editor.constants;

/**
 * @author Yvo Swillens
 */
public interface DmnStencilConstants {

    // stencil items
    final String STENCIL_INFORMATION_REQUIREMENT = "InformationRequirement";
    final String STENCIL_EXPANDED_DECISION_SERVICE = "ExpandedDecisionService";
    final String STENCIL_OUTPUT_DECISIONS = "OutputDecisionsDecisionServiceSection";
    final String STENCIL_ENCAPSULATED_DECISIONS = "EncapsulatedDecisionsDecisionServiceSection";
    final String STENCIL_DECISION = "Decision";

    // stencil properties
    final String PROPERTY_OVERRIDE_ID = "overrideid";
    final String PROPERTY_DRD_ID = "drd_id";
    final String PROPERTY_NAME = "name";
    final String PROPERTY_DOCUMENTATION = "documentation";

    final String PROPERTY_DECISION_TABLE_REFERENCE = "decisiondecisiontablereference";

}
