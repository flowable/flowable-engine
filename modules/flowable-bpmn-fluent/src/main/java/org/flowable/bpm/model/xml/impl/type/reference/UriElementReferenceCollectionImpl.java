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

import org.flowable.bpm.model.xml.instance.ModelElementInstance;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;

public class UriElementReferenceCollectionImpl<Target extends ModelElementInstance, Source extends ModelElementInstance>
        extends ElementReferenceCollectionImpl<Target, Source> {

    public UriElementReferenceCollectionImpl(ChildElementCollection<Source> referenceSourceCollection) {
        super(referenceSourceCollection);
    }

    @Override
    public String getReferenceIdentifier(ModelElementInstance referenceSourceElement) {
        // TODO: implement something more robust
        String identifier = referenceSourceElement.getAttributeValue("href");
        if (identifier != null) {
            String[] parts = identifier.split("#");
            return parts[parts.length > 1 ? parts.length - 1 : 0];
        }
        return null;
    }

    @Override
    protected void setReferenceIdentifier(ModelElementInstance referenceSourceElement, String referenceIdentifier) {
        // TODO: implement something more robust
        referenceSourceElement.setAttributeValue("href", '#' + referenceIdentifier);
    }

}
