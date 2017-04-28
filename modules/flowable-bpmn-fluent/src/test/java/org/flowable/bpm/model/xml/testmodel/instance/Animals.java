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
package org.flowable.bpm.model.xml.testmodel.instance;

import static org.flowable.bpm.model.xml.testmodel.TestModelConstants.ELEMENT_NAME_ANIMALS;
import static org.flowable.bpm.model.xml.testmodel.TestModelConstants.MODEL_NAMESPACE;

import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelElementInstanceImpl;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import org.flowable.bpm.model.xml.type.child.ChildElement;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;

import java.util.Collection;

public class Animals
        extends ModelElementInstanceImpl {

    protected static ChildElement<Description> descriptionChild;
    protected static ChildElementCollection<Animal> animalColl;

    public static void registerType(ModelBuilder modelBuilder) {

        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Animals.class, ELEMENT_NAME_ANIMALS)
                .namespaceUri(MODEL_NAMESPACE)
                .instanceProvider(new ModelTypeInstanceProvider<Animals>() {
                    @Override
                    public Animals newInstance(ModelTypeInstanceContext instanceContext) {
                        return new Animals(instanceContext);
                    }
                });

        SequenceBuilder sequence = typeBuilder.sequence();

        descriptionChild = sequence.element(Description.class)
                .build();

        animalColl = sequence.elementCollection(Animal.class)
                .build();

        typeBuilder.build();

    }

    public Animals(ModelTypeInstanceContext instanceContext) {
        super(instanceContext);
    }

    public Description getDescription() {
        return descriptionChild.getChild(this);
    }

    public void setDescription(Description description) {
        descriptionChild.setChild(this, description);
    }

    public Collection<Animal> getAnimals() {
        return animalColl.get(this);
    }

}
