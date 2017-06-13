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
package org.flowable.bpm.model.xml.impl.type.child;

import org.flowable.bpm.model.xml.Model;
import org.flowable.bpm.model.xml.ModelException;
import org.flowable.bpm.model.xml.impl.ModelBuildOperation;
import org.flowable.bpm.model.xml.impl.type.ModelElementTypeImpl;
import org.flowable.bpm.model.xml.impl.type.reference.ElementReferenceCollectionBuilderImpl;
import org.flowable.bpm.model.xml.impl.type.reference.IdsElementReferenceCollectionBuilderImpl;
import org.flowable.bpm.model.xml.impl.type.reference.QNameElementReferenceCollectionBuilderImpl;
import org.flowable.bpm.model.xml.impl.type.reference.UriElementReferenceCollectionBuilderImpl;
import org.flowable.bpm.model.xml.instance.ModelElementInstance;
import org.flowable.bpm.model.xml.type.ModelElementType;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;
import org.flowable.bpm.model.xml.type.child.ChildElementCollectionBuilder;
import org.flowable.bpm.model.xml.type.reference.ElementReferenceCollectionBuilder;

import java.util.ArrayList;
import java.util.List;

public class ChildElementCollectionBuilderImpl<T extends ModelElementInstance>
        implements ChildElementCollectionBuilder<T>, ModelBuildOperation {

    /** The {@link ModelElementType} of the element containing the collection */
    protected final ModelElementTypeImpl parentElementType;
    private final ChildElementCollectionImpl<T> collection;
    protected final Class<T> childElementType;

    private ElementReferenceCollectionBuilder<?, ?> referenceBuilder;

    private final List<ModelBuildOperation> modelBuildOperations = new ArrayList<>();

    public ChildElementCollectionBuilderImpl(Class<T> childElementTypeClass, ModelElementType parentElementType) {
        this.childElementType = childElementTypeClass;
        this.parentElementType = (ModelElementTypeImpl) parentElementType;
        this.collection = createCollectionInstance();

    }

    protected ChildElementCollectionImpl<T> createCollectionInstance() {
        return new ChildElementCollectionImpl<>(childElementType, parentElementType);
    }

    @Override
    public ChildElementCollectionBuilder<T> immutable() {
        collection.setImmutable();
        return this;
    }

    @Override
    public ChildElementCollectionBuilder<T> required() {
        collection.setMinOccurs(1);
        return this;
    }

    @Override
    public ChildElementCollectionBuilder<T> maxOccurs(int i) {
        collection.setMaxOccurs(i);
        return this;
    }

    @Override
    public ChildElementCollectionBuilder<T> minOccurs(int i) {
        collection.setMinOccurs(i);
        return this;
    }

    @Override
    public ChildElementCollection<T> build() {
        return collection;
    }

    @Override
    public <V extends ModelElementInstance> ElementReferenceCollectionBuilder<V, T> qNameElementReferenceCollection(Class<V> referenceTargetType) {
        ChildElementCollectionImpl<T> collection = (ChildElementCollectionImpl<T>) build();
        QNameElementReferenceCollectionBuilderImpl<V, T> builder =
                new QNameElementReferenceCollectionBuilderImpl<>(childElementType, referenceTargetType, collection);
        setReferenceBuilder(builder);
        return builder;
    }

    @Override
    public <V extends ModelElementInstance> ElementReferenceCollectionBuilder<V, T> idElementReferenceCollection(Class<V> referenceTargetType) {
        ChildElementCollectionImpl<T> collection = (ChildElementCollectionImpl<T>) build();
        ElementReferenceCollectionBuilder<V, T> builder = new ElementReferenceCollectionBuilderImpl<>(childElementType, referenceTargetType, collection);
        setReferenceBuilder(builder);
        return builder;
    }

    @Override
    public <V extends ModelElementInstance> ElementReferenceCollectionBuilder<V, T> idsElementReferenceCollection(Class<V> referenceTargetType) {
        ChildElementCollectionImpl<T> collection = (ChildElementCollectionImpl<T>) build();
        ElementReferenceCollectionBuilder<V, T> builder = new IdsElementReferenceCollectionBuilderImpl<>(childElementType, referenceTargetType, collection);
        setReferenceBuilder(builder);
        return builder;
    }

    @Override
    public <V extends ModelElementInstance> ElementReferenceCollectionBuilder<V, T> uriElementReferenceCollection(Class<V> referenceTargetType) {
        ChildElementCollectionImpl<T> collection = (ChildElementCollectionImpl<T>) build();
        ElementReferenceCollectionBuilder<V, T> builder = new UriElementReferenceCollectionBuilderImpl<>(childElementType, referenceTargetType, collection);
        setReferenceBuilder(builder);
        return builder;
    }

    protected void setReferenceBuilder(ElementReferenceCollectionBuilder<?, ?> referenceBuilder) {
        if (this.referenceBuilder != null) {
            throw new ModelException("An collection cannot have more than one reference");
        }
        this.referenceBuilder = referenceBuilder;
        modelBuildOperations.add(referenceBuilder);
    }

    @Override
    public void performModelBuild(Model model) {
        ModelElementType elementType = model.getType(childElementType);
        if (elementType == null) {
            throw new ModelException(parentElementType + " declares undefined child element of type " + childElementType + '.');
        }
        parentElementType.registerChildElementType(elementType);
        parentElementType.registerChildElementCollection(collection);
        for (ModelBuildOperation modelBuildOperation : modelBuildOperations) {
            modelBuildOperation.performModelBuild(model);
        }
    }

}
