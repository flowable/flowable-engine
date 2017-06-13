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
import org.flowable.bpm.model.xml.type.ModelElementType;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;

public class ChildElementAssert
        extends AbstractAssert<ChildElementAssert, ChildElementCollection<?>> {

    private final Class<? extends ModelElementInstance> typeClass;

    protected ChildElementAssert(ChildElementCollection<?> actual) {
        super(actual, ChildElementAssert.class);
        typeClass = actual.getChildElementTypeClass();
    }

    public ChildElementAssert occursMinimal(int minOccurs) {
        isNotNull();

        int actualMinOccurs = actual.getMinOccurs();

        if (actualMinOccurs != minOccurs) {
            failWithMessage("Expected child element <%s> to have a min occurs of <%s> but was <%s>", typeClass, minOccurs, actualMinOccurs);
        }

        return this;
    }

    public ChildElementAssert occursMaximal(int maxOccurs) {
        isNotNull();

        int actualMaxOccurs = actual.getMaxOccurs();

        if (actualMaxOccurs != maxOccurs) {
            failWithMessage("Expected child element <%s> to have a max occurs of <%s> but was <%s>", typeClass, maxOccurs, actualMaxOccurs);
        }

        return this;
    }

    public ChildElementAssert isOptional() {
        isNotNull();

        int actualMinOccurs = actual.getMinOccurs();

        if (actualMinOccurs != 0) {
            failWithMessage("Expected child element <%s> to be optional but has min occurs of <%s>", typeClass, actualMinOccurs);
        }

        return this;
    }

    public ChildElementAssert isUnbounded() {
        isNotNull();

        int actualMaxOccurs = actual.getMaxOccurs();

        if (actualMaxOccurs != -1) {
            failWithMessage("Expected child element <%s> to be unbounded but has a max occurs of <%s>", typeClass, actualMaxOccurs);
        }

        return this;
    }

    public ChildElementAssert isMutable() {
        isNotNull();

        boolean actualImmutable = actual.isImmutable();

        if (actualImmutable) {
            failWithMessage("Expected child element <%s> to be mutable but was not", typeClass);
        }

        return this;
    }

    public ChildElementAssert isImmutable() {
        isNotNull();

        boolean actualImmutable = actual.isImmutable();

        if (!actualImmutable) {
            failWithMessage("Expected child element <%s> to be immutable but was not", typeClass);
        }

        return this;
    }

    public ChildElementAssert containsType(Class<? extends ModelElementInstance> childElementTypeClass) {
        isNotNull();

        Class<? extends ModelElementInstance> actualChildElementTypeClass = actual.getChildElementTypeClass();

        if (!childElementTypeClass.equals(actualChildElementTypeClass)) {
            failWithMessage("Expected child element <%s> to contain elements of type <%s> but contains elements of type <%s>", typeClass, childElementTypeClass,
                    actualChildElementTypeClass);
        }

        return this;
    }

    public ChildElementAssert hasParentElementType(ModelElementType parentElementType) {
        isNotNull();

        ModelElementType actualParentElementType = actual.getParentElementType();

        if (!parentElementType.equals(actualParentElementType)) {
            failWithMessage("Expected child element <%s> to have parent element type <%s> but has <%s>", typeClass, parentElementType.getTypeName(),
                    actualParentElementType.getTypeName());
        }

        return this;
    }

    public ChildElementAssert isNotEmpty(ModelElementInstance instance) {
        isNotNull();

        int actualNumberOfChildElements = actual.get(instance).size();

        if (actualNumberOfChildElements == 0) {
            failWithMessage("Expected child element <%s> to contain elements but was not", typeClass);
        }

        return this;
    }

    public ChildElementAssert hasSize(ModelElementInstance instance, int numberOfChildElements) {
        isNotNull();

        int actualNumberOfChildElements = actual.get(instance).size();

        if (actualNumberOfChildElements != numberOfChildElements) {
            failWithMessage("Expected child element <%s> to contain <%s> elements but has <%s>", typeClass, numberOfChildElements, actualNumberOfChildElements);
        }

        return this;
    }

    public ChildElementAssert isEmpty(ModelElementInstance instance) {
        isNotNull();

        int actualNumberOfChildElements = actual.get(instance).size();

        if (actualNumberOfChildElements > 0) {
            failWithMessage("Expected child element <%s> to contain no elements but contains <%s> elements", typeClass, actualNumberOfChildElements);
        }

        return this;
    }
}
