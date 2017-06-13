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
package org.flowable.bpm.model.xml.testmodel;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.flowable.bpm.model.xml.ModelInstance;
import org.flowable.bpm.model.xml.testmodel.instance.Animal;
import org.flowable.bpm.model.xml.testmodel.instance.Animals;
import org.flowable.bpm.model.xml.testmodel.instance.Bird;
import org.junit.Test;

public class TestModelInstanceTest {

    @Test
    public void doClone()
        throws Exception {
        ModelInstance modelInstance = new TestModelParser().getEmptyModel();

        Animals animals = modelInstance.newInstance(Animals.class);
        modelInstance.setDocumentElement(animals);

        Animal animal = modelInstance.newInstance(Bird.class);
        animal.setId("TestId");
        animals.addChildElement(animal);

        ModelInstance cloneInstance = modelInstance.clone();
        getFirstAnimal(cloneInstance).setId("TestId2");

        assertThat(getFirstAnimal(modelInstance).getId(), is(equalTo("TestId")));
        assertThat(getFirstAnimal(cloneInstance).getId(), is(equalTo("TestId2")));
    }

    protected Animal getFirstAnimal(ModelInstance modelInstance) {
        Animals animals = (Animals) modelInstance.getDocumentElement();
        return animals.getAnimals().iterator().next();
    }

}
