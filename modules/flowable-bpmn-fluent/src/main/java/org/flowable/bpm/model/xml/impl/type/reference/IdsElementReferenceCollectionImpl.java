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

import org.flowable.bpm.model.xml.ModelException;
import org.flowable.bpm.model.xml.impl.instance.ModelElementInstanceImpl;
import org.flowable.bpm.model.xml.impl.util.StringUtil;
import org.flowable.bpm.model.xml.instance.DomDocument;
import org.flowable.bpm.model.xml.instance.DomElement;
import org.flowable.bpm.model.xml.instance.ModelElementInstance;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class IdsElementReferenceCollectionImpl<Target extends ModelElementInstance, Source extends ModelElementInstance>
        extends ElementReferenceCollectionImpl<Target, Source> {

    protected String separator = " ";

    public IdsElementReferenceCollectionImpl(ChildElementCollection<Source> referenceSourceCollection) {
        super(referenceSourceCollection);
    }

    protected List<String> getReferenceIdentifiers(ModelElementInstance referenceSourceElement) {
        String referenceIdentifiers = getReferenceIdentifier(referenceSourceElement);
        return StringUtil.splitListBySeparator(referenceIdentifiers, separator);
    }

    protected void setReferenceIdentifiers(ModelElementInstance referenceSourceElement, List<String> referenceIdentifiers) {
        String referenceIdentifier = StringUtil.joinList(referenceIdentifiers, separator);
        referenceSourceElement.setTextContent(referenceIdentifier);
    }

    @Override
    protected Collection<DomElement> getView(ModelElementInstanceImpl referenceSourceParentElement) {
        DomDocument document = referenceSourceParentElement.getModelInstance().getDocument();
        Collection<Source> referenceSourceElements = getReferenceSourceCollection().get(referenceSourceParentElement);
        Collection<DomElement> referenceTargetElements = new ArrayList<>();
        for (Source referenceSourceElement : referenceSourceElements) {
            List<String> identifiers = getReferenceIdentifiers(referenceSourceElement);
            for (String identifier : identifiers) {
                DomElement referenceTargetElement = document.getElementById(identifier);
                if (referenceTargetElement != null) {
                    referenceTargetElements.add(referenceTargetElement);
                } else {
                    throw new ModelException("Unable to find a model element instance for id " + identifier);
                }
            }
        }
        return referenceTargetElements;
    }

    @Override
    protected void updateReference(ModelElementInstance referenceSourceElement, String oldIdentifier, String newIdentifier) {
        List<String> referenceIdentifiers = getReferenceIdentifiers(referenceSourceElement);
        if (referenceIdentifiers.contains(oldIdentifier)) {
            int index = referenceIdentifiers.indexOf(oldIdentifier);
            referenceIdentifiers.remove(oldIdentifier);
            referenceIdentifiers.add(index, newIdentifier);
            setReferenceIdentifiers(referenceSourceElement, referenceIdentifiers);
        }
    }

    @Override
    public void referencedElementRemoved(ModelElementInstance referenceTargetElement, Object referenceIdentifier) {
        for (ModelElementInstance referenceSourceElement : findReferenceSourceElements(referenceTargetElement)) {
            List<String> referenceIdentifiers = getReferenceIdentifiers(referenceSourceElement);
            if (referenceIdentifiers.contains(referenceIdentifier)) {
                if (referenceIdentifiers.size() == 1) {
                    // remove whole element
                    removeReference(referenceSourceElement, referenceTargetElement);
                } else {
                    // remove only single identifier
                    referenceIdentifiers.remove(referenceIdentifier);
                    setReferenceIdentifiers(referenceSourceElement, referenceIdentifiers);
                }
            }
        }
    }

}
