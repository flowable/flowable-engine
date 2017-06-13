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
package org.flowable.bpm.model.bpmn.instance;

import org.flowable.bpm.model.bpmn.builder.AbstractBaseElementBuilder;
import org.flowable.bpm.model.xml.instance.ModelElementInstance;

/**
 * Interface implemented by all elements in a BPMN Model.
 */
public interface BpmnModelElementInstance
        extends ModelElementInstance {

    /**
     * Returns a new fluent builder for the element if implemented.
     *
     * @return the builder object
     */
    @SuppressWarnings("rawtypes")
    AbstractBaseElementBuilder builder();

    /**
     * Tests if the element is a scope like process or sub-process.
     *
     * @return true if element is scope, otherwise false
     */
    boolean isScope();

    /**
     * Gets the element which is the scope of this element. Like the parent process or sub-process.
     *
     * @return the scope element or null if non is found
     */
    BpmnModelElementInstance getScope();

}
