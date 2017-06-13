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

public abstract class TestModelConstants {

    public static final String MODEL_NAME = "animals";
    public static final String MODEL_NAMESPACE = "http://flowable.org/animals";
    public static final String NEWER_NAMESPACE = "http://flowable.org/electronic";

    public static final String TYPE_NAME_ANIMAL = "animal";
    public static final String TYPE_NAME_FLYING_ANIMAL = "flyingAnimal";
    public static final String TYPE_NAME_CHILD_RELATIONSHIP_DEFINITION = "childRelationshipDefinition";
    public static final String TYPE_NAME_FRIEND_RELATIONSHIP_DEFINITION = "friendRelationshipDefinition";
    public static final String TYPE_NAME_RELATIONSHIP_DEFINITION = "relationshipDefinition";
    public static final String TYPE_NAME_WINGS = "wings";

    public static final String ELEMENT_NAME_ANIMALS = "animals";
    public static final String ELEMENT_NAME_BIRD = "bird";
    public static final String ELEMENT_NAME_RELATIONSHIP_DEFINITION_REF = "relationshipDefinitionRef";
    public static final String ELEMENT_NAME_FLIGHT_PARTNER_REF = "flightPartnerRef";
    public static final String ELEMENT_NAME_FLIGHT_INSTRUCTOR = "flightInstructor";
    public static final String ELEMENT_NAME_SPOUSE_REF = "spouseRef";
    public static final String ELEMENT_NAME_EGG = "egg";
    public static final String ELEMENT_NAME_ANIMAL_REFERENCE = "animalReference";
    public static final String ELEMENT_NAME_GUARDIAN = "guardian";
    public static final String ELEMENT_NAME_MOTHER = "mother";
    public static final String ELEMENT_NAME_GUARD_EGG = "guardEgg";
    public static final String ELEMENT_NAME_DESCRIPTION = "description";

    public static final String ATTRIBUTE_NAME_ID = "id";
    public static final String ATTRIBUTE_NAME_NAME = "name";
    public static final String ATTRIBUTE_NAME_FATHER = "father";
    public static final String ATTRIBUTE_NAME_MOTHER = "mother";
    public static final String ATTRIBUTE_NAME_IS_ENDANGERED = "isEndangered";
    public static final String ATTRIBUTE_NAME_GENDER = "gender";
    public static final String ATTRIBUTE_NAME_AGE = "age";
    public static final String ATTRIBUTE_NAME_BEST_FRIEND_REFS = "bestFriendRefs";
    public static final String ATTRIBUTE_NAME_ANIMAL_REF = "animalRef";
    public static final String ATTRIBUTE_NAME_WINGSPAN = "wingspan";

    private TestModelConstants() {

    }

}
