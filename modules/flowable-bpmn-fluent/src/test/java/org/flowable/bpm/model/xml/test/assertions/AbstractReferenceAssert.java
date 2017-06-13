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
package org.flowable.bpm.model.xml.test.assertions;

import org.assertj.core.api.AbstractAssert;
import org.flowable.bpm.model.xml.instance.ModelElementInstance;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.reference.Reference;

public abstract class AbstractReferenceAssert<S extends AbstractReferenceAssert<S, T>, T extends Reference<?>>
        extends AbstractAssert<S, T> {

    protected AbstractReferenceAssert(T actual, Class<?> selfType) {
        super(actual, selfType);
    }

    public S hasIdentifier(ModelElementInstance instance, String identifier) {
        isNotNull();

        String actualIdentifier = actual.getReferenceIdentifier(instance);

        if (!identifier.equals(actualIdentifier)) {
            failWithMessage("Expected reference <%s> to have identifier <%s> but was <%s>", actual, identifier, actualIdentifier);
        }

        return myself;
    }

    public S hasTargetElement(ModelElementInstance instance, ModelElementInstance targetElement) {
        isNotNull();

        ModelElementInstance actualTargetElement = actual.getReferenceTargetElement(instance);

        if (!targetElement.equals(actualTargetElement)) {
            failWithMessage("Expected reference <%s> to have target element <%s> but was <%s>", actual, targetElement, actualTargetElement);
        }

        return myself;
    }

    public S hasNoTargetElement(ModelElementInstance instance) {
        isNotNull();

        ModelElementInstance actualTargetElement = actual.getReferenceTargetElement(instance);

        if (actualTargetElement != null) {
            failWithMessage("Expected reference <%s> to have no target element but has <%s>", actualTargetElement, actualTargetElement);
        }

        return myself;
    }

    public S hasTargetAttribute(Attribute<?> targetAttribute) {
        isNotNull();

        Attribute<String> actualTargetAttribute = actual.getReferenceTargetAttribute();

        if (!targetAttribute.equals(actualTargetAttribute)) {
            failWithMessage("Expected reference <%s> to have target attribute <%s> but was <%s>", actual, targetAttribute, actualTargetAttribute);
        }

        return myself;
    }
}
