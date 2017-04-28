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
package org.flowable.bpm.model.xml.impl.type.attribute;

import org.flowable.bpm.model.xml.impl.type.reference.ReferenceImpl;
import org.flowable.bpm.model.xml.instance.ModelElementInstance;
import org.flowable.bpm.model.xml.type.ModelElementType;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.reference.Reference;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for implementing primitive value attributes.
 */
public abstract class AttributeImpl<T>
        implements Attribute<T> {

    /** the local name of the attribute */
    private String attributeName;

    /** the namespace for this attribute */
    private String namespaceUri;

    /**
     * the default value for this attribute: the default value is returned by the {@link #getValue(ModelElementInstance)} method in case the attribute
     * is not set on the domElement.
     */
    private T defaultValue;

    private boolean isRequired;

    private boolean isIdAttribute;

    private final List<Reference<?>> outgoingReferences = new ArrayList<>();

    private final List<Reference<?>> incomingReferences = new ArrayList<>();

    private final ModelElementType owningElementType;

    AttributeImpl(ModelElementType owningElementType) {
        this.owningElementType = owningElementType;
    }

    /**
     * to be implemented by subclasses: converts the raw (String) value of the attribute to the type required by the model
     *
     * @return the converted value
     */
    protected abstract T convertXmlValueToModelValue(String rawValue);

    /**
     * to be implemented by subclasses: converts the raw (String) value of the attribute to the type required by the model
     *
     * @return the converted value
     */
    protected abstract String convertModelValueToXmlValue(T modelValue);

    @Override
    public ModelElementType getOwningElementType() {
        return owningElementType;
    }

    /**
     * returns the value of the attribute.
     *
     * @return the value of the attribute.
     */
    @Override
    public T getValue(ModelElementInstance modelElement) {
        String value;
        if (namespaceUri == null) {
            value = modelElement.getAttributeValue(attributeName);
        } else {
            value = modelElement.getAttributeValueNs(namespaceUri, attributeName);
            if (value == null) {
                String alternativeNamespace = owningElementType.getModel().getAlternativeNamespace(namespaceUri);
                if (alternativeNamespace != null) {
                    value = modelElement.getAttributeValueNs(alternativeNamespace, attributeName);
                }
            }
        }

        // default value
        return value == null && defaultValue != null ? defaultValue : convertXmlValueToModelValue(value);
    }

    /**
     * sets the value of the attribute.
     *
     * the value of the attribute.
     */
    @Override
    public void setValue(ModelElementInstance modelElement, T value) {
        String xmlValue = convertModelValueToXmlValue(value);
        if (namespaceUri == null) {
            modelElement.setAttributeValue(attributeName, xmlValue, isIdAttribute);
        } else {
            modelElement.setAttributeValueNs(namespaceUri, attributeName, xmlValue, isIdAttribute);
        }
    }

    public void updateIncomingReferences(ModelElementInstance modelElement, String newIdentifier, String oldIdentifier) {
        if (!incomingReferences.isEmpty()) {
            for (Reference<?> incomingReference : incomingReferences) {
                ((ReferenceImpl<?>) incomingReference).referencedElementUpdated(modelElement, oldIdentifier, newIdentifier);
            }
        }
    }

    @Override
    public T getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
    }


    @Override
    public boolean isRequired() {
        return isRequired;
    }

    /**
     */
    public void setRequired(boolean required) {
        this.isRequired = required;
    }

    /**
     * @param namespaceUri the namespaceUri to set
     */
    public void setNamespaceUri(String namespaceUri) {
        this.namespaceUri = namespaceUri;
    }

    /**
     * @return the namespaceUri
     */
    @Override
    public String getNamespaceUri() {
        return namespaceUri;
    }

    @Override
    public boolean isIdAttribute() {
        return isIdAttribute;
    }

    /**
     * Indicate whether this attribute is an Id attribute
     *
     */
    public void setId() {
        this.isIdAttribute = true;
    }

    /**
     * @return the attributeName
     */
    @Override
    public String getAttributeName() {
        return attributeName;
    }

    /**
     * @param attributeName the attributeName to set
     */
    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public void removeAttribute(ModelElementInstance modelElement) {
        if (namespaceUri == null) {
            modelElement.removeAttribute(attributeName);
        } else {
            modelElement.removeAttributeNs(namespaceUri, attributeName);
        }
    }

    public void unlinkReference(ModelElementInstance modelElement, Object referenceIdentifier) {
        if (!incomingReferences.isEmpty()) {
            for (Reference<?> incomingReference : incomingReferences) {
                ((ReferenceImpl<?>) incomingReference).referencedElementRemoved(modelElement, referenceIdentifier);
            }
        }
    }

    /**
     * @return the incomingReferences
     */
    @Override
    public List<Reference<?>> getIncomingReferences() {
        return incomingReferences;
    }

    /**
     * @return the outgoingReferences
     */
    @Override
    public List<Reference<?>> getOutgoingReferences() {
        return outgoingReferences;
    }

    public void registerOutgoingReference(Reference<?> ref) {
        outgoingReferences.add(ref);
    }

    public void registerIncoming(Reference<?> ref) {
        incomingReferences.add(ref);
    }

}
