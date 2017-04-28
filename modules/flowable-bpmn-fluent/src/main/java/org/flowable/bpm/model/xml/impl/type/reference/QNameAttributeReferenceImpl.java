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
package org.flowable.bpm.model.xml.impl.type.reference;

import org.flowable.bpm.model.xml.impl.type.attribute.AttributeImpl;
import org.flowable.bpm.model.xml.impl.util.QName;
import org.flowable.bpm.model.xml.instance.ModelElementInstance;

public class QNameAttributeReferenceImpl<T extends ModelElementInstance>
        extends AttributeReferenceImpl<T> {

    /**
     * Create a new QName reference outgoing from the reference source attribute
     *
     * @param referenceSourceAttribute the reference source attribute
     */
    public QNameAttributeReferenceImpl(AttributeImpl<String> referenceSourceAttribute) {
        super(referenceSourceAttribute);
    }

    @Override
    public String getReferenceIdentifier(ModelElementInstance referenceSourceElement) {
        String identifier = super.getReferenceIdentifier(referenceSourceElement);
        if (identifier != null) {
            QName qName = QName.parseQName(identifier);
            return qName.getLocalName();
        } else {
            return null;
        }
    }

}
