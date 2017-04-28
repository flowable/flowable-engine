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
package org.flowable.bpm.model.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.flowable.bpm.model.xml.testmodel.TestModelConstants.ELEMENT_NAME_BIRD;
import static org.flowable.bpm.model.xml.testmodel.TestModelConstants.MODEL_NAME;
import static org.flowable.bpm.model.xml.testmodel.TestModelConstants.MODEL_NAMESPACE;

import org.flowable.bpm.model.xml.testmodel.TestModel;
import org.flowable.bpm.model.xml.testmodel.instance.Animal;
import org.flowable.bpm.model.xml.testmodel.instance.Animals;
import org.flowable.bpm.model.xml.testmodel.instance.Bird;
import org.flowable.bpm.model.xml.testmodel.instance.FlyingAnimal;
import org.flowable.bpm.model.xml.testmodel.instance.RelationshipDefinition;
import org.flowable.bpm.model.xml.type.ModelElementType;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

public class ModelTest {

    private Model model;

    @Before
    public void createModel() {
        model = TestModel.getTestModel();
    }

    @Test
    public void getTypes() {
        Collection<ModelElementType> types = model.getTypes();
        assertThat(types).isNotEmpty();
        assertThat(types).contains(
                model.getType(Animals.class),
                model.getType(Animal.class),
                model.getType(FlyingAnimal.class),
                model.getType(Bird.class),
                model.getType(RelationshipDefinition.class));
    }

    @Test
    public void getType() {
        ModelElementType flyingAnimalType = model.getType(FlyingAnimal.class);
        assertThat(flyingAnimalType.getInstanceType()).isEqualTo(FlyingAnimal.class);
    }

    @Test
    public void getTypeForName() {
        ModelElementType birdType = model.getTypeForName(ELEMENT_NAME_BIRD);
        assertThat(birdType).isNull();
        birdType = model.getTypeForName(MODEL_NAMESPACE, ELEMENT_NAME_BIRD);
        assertThat(birdType.getInstanceType()).isEqualTo(Bird.class);
    }

    @Test
    public void getModelName() {
        assertThat(model.getModelName()).isEqualTo(MODEL_NAME);
    }

    @Test
    public void equal() {
        assertThat(model).isNotEqualTo(null);
        assertThat(model).isNotEqualTo(new Object());
        Model otherModel = ModelBuilder.createInstance("Other Model").build();
        assertThat(model).isNotEqualTo(otherModel);
        otherModel = ModelBuilder.createInstance(MODEL_NAME).build();
        assertThat(model).isEqualTo(otherModel);
        otherModel = ModelBuilder.createInstance(null).build();
        assertThat(otherModel).isNotEqualTo(model);
        assertThat(model).isEqualTo(model);
    }
}
