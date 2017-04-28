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
package org.flowable.bpm.model.xml.type.reference;

import static org.flowable.bpm.model.xml.test.assertions.ModelAssertions.assertThat;
import static org.junit.Assert.fail;
import static org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

import org.flowable.bpm.model.xml.ModelInstance;
import org.flowable.bpm.model.xml.UnsupportedModelOperationException;
import org.flowable.bpm.model.xml.impl.parser.AbstractModelParser;
import org.flowable.bpm.model.xml.impl.type.reference.AttributeReferenceImpl;
import org.flowable.bpm.model.xml.impl.type.reference.QNameAttributeReferenceImpl;
import org.flowable.bpm.model.xml.testmodel.Gender;
import org.flowable.bpm.model.xml.testmodel.TestModelParser;
import org.flowable.bpm.model.xml.testmodel.TestModelTest;
import org.flowable.bpm.model.xml.testmodel.instance.Animal;
import org.flowable.bpm.model.xml.testmodel.instance.Animals;
import org.flowable.bpm.model.xml.testmodel.instance.Bird;
import org.flowable.bpm.model.xml.testmodel.instance.FlightPartnerRef;
import org.flowable.bpm.model.xml.testmodel.instance.FlyingAnimal;
import org.flowable.bpm.model.xml.type.ModelElementType;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.junit.Before;
import org.junit.Test;

public class ReferenceTest
        extends TestModelTest {

    private Bird tweety;
    private Bird daffy;
    private Bird daisy;
    private Bird plucky;
    private Bird birdo;
    private FlightPartnerRef flightPartnerRef;

    private ModelElementType animalType;
    private QNameAttributeReferenceImpl<Animal> fatherReference;
    private AttributeReferenceImpl<Animal> motherReference;
    private ElementReferenceCollection<FlyingAnimal, FlightPartnerRef> flightPartnerRefsColl;

    public ReferenceTest(String testName, ModelInstance testModelInstance, AbstractModelParser modelParser) {
        super(testName, testModelInstance, modelParser);
    }

    @Parameters(name = "Model {0}")
    public static Collection<Object[]> models() {
        Object[][] models = {createModel(), parseModel(ReferenceTest.class)};
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
        createBird(modelInstance, "plucky", Gender.Male);
        createBird(modelInstance, "birdo", Gender.Female);
        tweety.setFather(daffy);
        tweety.setMother(daisy);

        tweety.getFlightPartnerRefs().add(daffy);

        return new Object[] {"created", modelInstance, modelParser};
    }

    @Before
    @SuppressWarnings("unchecked")
    public void copyModelInstance() {
        modelInstance = cloneModelInstance();

        tweety = modelInstance.getModelElementById("tweety");
        daffy = modelInstance.getModelElementById("daffy");
        daisy = modelInstance.getModelElementById("daisy");
        plucky = modelInstance.getModelElementById("plucky");
        birdo = modelInstance.getModelElementById("birdo");

        animalType = modelInstance.getModel().getType(Animal.class);

        // QName attribute reference
        fatherReference = (QNameAttributeReferenceImpl<Animal>) animalType.getAttribute("father").getOutgoingReferences().iterator().next();

        // ID attribute reference
        motherReference = (AttributeReferenceImpl<Animal>) animalType.getAttribute("mother").getOutgoingReferences().iterator().next();

        // ID element reference
        flightPartnerRefsColl = FlyingAnimal.flightPartnerRefsColl;

        ModelElementType flightPartnerRefType = modelInstance.getModel().getType(FlightPartnerRef.class);
        flightPartnerRef = (FlightPartnerRef) modelInstance.getModelElementsByType(flightPartnerRefType).iterator().next();
    }

    @Test
    public void referenceIdentifier() {
        assertThat(fatherReference).hasIdentifier(tweety, daffy.getId());
        assertThat(motherReference).hasIdentifier(tweety, daisy.getId());
        assertThat(flightPartnerRefsColl).hasIdentifier(tweety, daffy.getId());
    }

    @Test
    public void referenceTargetElement() {
        assertThat(fatherReference).hasTargetElement(tweety, daffy);
        assertThat(motherReference).hasTargetElement(tweety, daisy);
        assertThat(flightPartnerRefsColl).hasTargetElement(tweety, daffy);

        fatherReference.setReferenceTargetElement(tweety, plucky);
        motherReference.setReferenceTargetElement(tweety, birdo);
        flightPartnerRefsColl.setReferenceTargetElement(flightPartnerRef, daisy);

        assertThat(fatherReference).hasTargetElement(tweety, plucky);
        assertThat(motherReference).hasTargetElement(tweety, birdo);
        assertThat(flightPartnerRefsColl).hasTargetElement(tweety, daisy);
    }

    @Test
    public void referenceTargetAttribute() {
        Attribute<?> idAttribute = animalType.getAttribute("id");
        assertThat(idAttribute).hasIncomingReferences(fatherReference, motherReference);

        assertThat(fatherReference).hasTargetAttribute(idAttribute);
        assertThat(motherReference).hasTargetAttribute(idAttribute);
        assertThat(flightPartnerRefsColl).hasTargetAttribute(idAttribute);
    }

    @Test
    public void referenceSourceAttribute() {
        Attribute<?> fatherAttribute = animalType.getAttribute("father");
        Attribute<?> motherAttribute = animalType.getAttribute("mother");

        assertThat(fatherReference).hasSourceAttribute(fatherAttribute);
        assertThat(motherReference).hasSourceAttribute(motherAttribute);
    }

    @Test
    public void removeReference() {
        fatherReference.referencedElementRemoved(daffy, daffy.getId());

        assertThat(fatherReference).hasNoTargetElement(tweety);
        assertThat(tweety.getFather()).isNull();

        motherReference.referencedElementRemoved(daisy, daisy.getId());
        assertThat(motherReference).hasNoTargetElement(tweety);
        assertThat(tweety.getMother()).isNull();
    }

    @Test
    public void targetElementsCollection() {
        Collection<FlyingAnimal> referenceTargetElements = flightPartnerRefsColl.getReferenceTargetElements(tweety);
        Collection<FlyingAnimal> flightPartners = Arrays.asList(new FlyingAnimal[] {birdo, daffy, daisy, plucky});

        // directly test collection methods and not use the appropriate assertion methods
        assertThat(referenceTargetElements.size()).isEqualTo(1);
        assertThat(referenceTargetElements.isEmpty()).isFalse();
        assertThat(referenceTargetElements.contains(daffy)).isTrue();
        assertThat(referenceTargetElements.toArray()).isEqualTo(new Object[] {daffy});
        assertThat(referenceTargetElements.toArray(new FlyingAnimal[1])).isEqualTo(new FlyingAnimal[] {daffy});

        assertThat(referenceTargetElements.add(daisy)).isTrue();
        assertThat(referenceTargetElements)
                .hasSize(2)
                .containsOnly(daffy, daisy);

        assertThat(referenceTargetElements.remove(daisy)).isTrue();
        assertThat(referenceTargetElements)
                .hasSize(1)
                .containsOnly(daffy);

        assertThat(referenceTargetElements.addAll(flightPartners)).isTrue();
        assertThat(referenceTargetElements.containsAll(flightPartners)).isTrue();
        assertThat(referenceTargetElements)
                .hasSize(4)
                .containsOnly(daffy, daisy, plucky, birdo);

        assertThat(referenceTargetElements.removeAll(flightPartners)).isTrue();
        assertThat(referenceTargetElements).isEmpty();

        try {
            referenceTargetElements.retainAll(flightPartners);
            fail("retainAll method is not implemented");
        }
        catch (Exception e) {
            assertThat(e).isInstanceOf(UnsupportedModelOperationException.class);
        }

        referenceTargetElements.addAll(flightPartners);
        assertThat(referenceTargetElements).isNotEmpty();
        referenceTargetElements.clear();
        assertThat(referenceTargetElements).isEmpty();
    }

}
