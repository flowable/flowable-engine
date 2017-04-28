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
package org.flowable.bpm.model.xml.type.child;

import static org.flowable.bpm.model.xml.test.assertions.ModelAssertions.assertThat;
import static org.junit.Assert.fail;
import static org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.flowable.bpm.model.xml.ModelInstance;
import org.flowable.bpm.model.xml.UnsupportedModelOperationException;
import org.flowable.bpm.model.xml.impl.parser.AbstractModelParser;
import org.flowable.bpm.model.xml.impl.type.child.ChildElementCollectionImpl;
import org.flowable.bpm.model.xml.impl.type.child.ChildElementImpl;
import org.flowable.bpm.model.xml.testmodel.Gender;
import org.flowable.bpm.model.xml.testmodel.TestModelParser;
import org.flowable.bpm.model.xml.testmodel.TestModelTest;
import org.flowable.bpm.model.xml.testmodel.instance.Animals;
import org.flowable.bpm.model.xml.testmodel.instance.Bird;
import org.flowable.bpm.model.xml.testmodel.instance.FlightInstructor;
import org.flowable.bpm.model.xml.testmodel.instance.FlightPartnerRef;
import org.flowable.bpm.model.xml.testmodel.instance.FlyingAnimal;
import org.flowable.bpm.model.xml.type.ModelElementType;
import org.junit.Before;
import org.junit.Test;

public class ChildElementCollectionTest
        extends TestModelTest {

    private Bird tweety;
    private Bird daffy;
    private Bird daisy;
    private Bird plucky;
    private Bird birdo;
    private ChildElement<FlightInstructor> flightInstructorChild;
    private ChildElementCollection<FlightPartnerRef> flightPartnerRefCollection;

    public ChildElementCollectionTest(String testName, ModelInstance testModelInstance, AbstractModelParser modelParser) {
        super(testName, testModelInstance, modelParser);
    }

    @Parameters(name = "Model {0}")
    public static Collection<Object[]> models() {
        Object[][] models = {createModel(), parseModel(ChildElementCollectionTest.class)};
        return Arrays.asList(models);
    }

    public static Object[] createModel() {
        TestModelParser modelParser = new TestModelParser();
        ModelInstance modelInstance = modelParser.getEmptyModel();

        Animals animals = modelInstance.newInstance(Animals.class);
        modelInstance.setDocumentElement(animals);

        Bird tweety = createBird(modelInstance, "tweety", Gender.Female);
        Bird daffy = createBird(modelInstance, "daffy", Gender.Male);
        Bird daisy = createBird(modelInstance, "daisy", Gender.Female);
        Bird plucky = createBird(modelInstance, "plucky", Gender.Male);
        createBird(modelInstance, "birdo", Gender.Female);

        tweety.setFlightInstructor(daffy);
        tweety.getFlightPartnerRefs().add(daisy);
        tweety.getFlightPartnerRefs().add(plucky);

        return new Object[] {"created", modelInstance, modelParser};
    }

    @Before
    public void copyModelInstance() {
        modelInstance = cloneModelInstance();

        tweety = modelInstance.getModelElementById("tweety");
        daffy = modelInstance.getModelElementById("daffy");
        daisy = modelInstance.getModelElementById("daisy");
        plucky = modelInstance.getModelElementById("plucky");
        birdo = modelInstance.getModelElementById("birdo");

        flightInstructorChild = (ChildElement<FlightInstructor>) FlyingAnimal.flightInstructorChild.getReferenceSourceCollection();
        flightPartnerRefCollection = FlyingAnimal.flightPartnerRefsColl.getReferenceSourceCollection();
    }

    @Test
    public void immutable() {
        assertThat(flightInstructorChild).isMutable();
        assertThat(flightPartnerRefCollection).isMutable();

        ((ChildElementImpl<FlightInstructor>) flightInstructorChild).setImmutable();
        ((ChildElementCollectionImpl<FlightPartnerRef>) flightPartnerRefCollection).setImmutable();
        assertThat(flightInstructorChild).isImmutable();
        assertThat(flightPartnerRefCollection).isImmutable();

        ((ChildElementImpl<FlightInstructor>) flightInstructorChild).setMutable(true);
        ((ChildElementCollectionImpl<FlightPartnerRef>) flightPartnerRefCollection).setMutable(true);
        assertThat(flightInstructorChild).isMutable();
        assertThat(flightPartnerRefCollection).isMutable();
    }

    @Test
    public void minOccurs() {
        assertThat(flightInstructorChild).isOptional();
        assertThat(flightPartnerRefCollection).isOptional();
    }

    @Test
    public void maxOccurs() {
        assertThat(flightInstructorChild).occursMaximal(1);
        assertThat(flightPartnerRefCollection).isUnbounded();
    }

    @Test
    public void childElementType() {
        assertThat(flightInstructorChild).containsType(FlightInstructor.class);
        assertThat(flightPartnerRefCollection).containsType(FlightPartnerRef.class);
    }

    @Test
    public void parentElementType() {
        ModelElementType flyingAnimalType = modelInstance.getModel().getType(FlyingAnimal.class);

        assertThat(flightInstructorChild).hasParentElementType(flyingAnimalType);
        assertThat(flightPartnerRefCollection).hasParentElementType(flyingAnimalType);
    }

    @Test
    public void getChildElements() {
        assertThat(flightInstructorChild).hasSize(tweety, 1);
        assertThat(flightPartnerRefCollection).hasSize(tweety, 2);

        FlightInstructor flightInstructor = flightInstructorChild.getChild(tweety);
        assertThat(flightInstructor.getTextContent()).isEqualTo(daffy.getId());

        for (FlightPartnerRef flightPartnerRef : flightPartnerRefCollection.get(tweety)) {
            assertThat(flightPartnerRef.getTextContent()).isIn(daisy.getId(), plucky.getId());
        }
    }

    @Test
    public void removeChildElements() {
        assertThat(flightInstructorChild).isNotEmpty(tweety);
        assertThat(flightPartnerRefCollection).isNotEmpty(tweety);

        flightInstructorChild.removeChild(tweety);
        flightPartnerRefCollection.get(tweety).clear();

        assertThat(flightInstructorChild).isEmpty(tweety);
        assertThat(flightPartnerRefCollection).isEmpty(tweety);
    }

    @Test
    public void childElementsCollection() {
        Collection<FlightPartnerRef> flightPartnerRefs = flightPartnerRefCollection.get(tweety);

        Iterator<FlightPartnerRef> iterator = flightPartnerRefs.iterator();
        FlightPartnerRef daisyRef = iterator.next();
        FlightPartnerRef pluckyRef = iterator.next();
        assertThat(daisyRef.getTextContent()).isEqualTo(daisy.getId());
        assertThat(pluckyRef.getTextContent()).isEqualTo(plucky.getId());

        FlightPartnerRef birdoRef = modelInstance.newInstance(FlightPartnerRef.class);
        birdoRef.setTextContent(birdo.getId());

        Collection<FlightPartnerRef> flightPartners = Arrays.asList(birdoRef, daisyRef, pluckyRef);

        // directly test collection methods and not use the appropriate assertion methods
        assertThat(flightPartnerRefs.size()).isEqualTo(2);
        assertThat(flightPartnerRefs.isEmpty()).isFalse();
        assertThat(flightPartnerRefs.contains(daisyRef));
        assertThat(flightPartnerRefs.toArray()).isEqualTo(new Object[] {daisyRef, pluckyRef});
        assertThat(flightPartnerRefs.toArray(new FlightPartnerRef[1])).isEqualTo(new FlightPartnerRef[] {daisyRef, pluckyRef});

        assertThat(flightPartnerRefs.add(birdoRef)).isTrue();
        assertThat(flightPartnerRefs)
                .hasSize(3)
                .containsOnly(birdoRef, daisyRef, pluckyRef);

        assertThat(flightPartnerRefs.remove(daisyRef)).isTrue();
        assertThat(flightPartnerRefs)
                .hasSize(2)
                .containsOnly(birdoRef, pluckyRef);

        assertThat(flightPartnerRefs.addAll(flightPartners)).isTrue();
        assertThat(flightPartnerRefs.containsAll(flightPartners)).isTrue();
        assertThat(flightPartnerRefs)
                .hasSize(3)
                .containsOnly(birdoRef, daisyRef, pluckyRef);

        assertThat(flightPartnerRefs.removeAll(flightPartners)).isTrue();
        assertThat(flightPartnerRefs).isEmpty();

        try {
            flightPartnerRefs.retainAll(flightPartners);
            fail("retainAll method is not implemented");
        }
        catch (Exception e) {
            assertThat(e).isInstanceOf(UnsupportedModelOperationException.class);
        }

        flightPartnerRefs.addAll(flightPartners);
        assertThat(flightPartnerRefs).isNotEmpty();
        flightPartnerRefs.clear();
        assertThat(flightPartnerRefs).isEmpty();
    }
}
