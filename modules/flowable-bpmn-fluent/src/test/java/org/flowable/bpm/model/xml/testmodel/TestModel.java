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

import org.flowable.bpm.model.xml.Model;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.testmodel.instance.Animal;
import org.flowable.bpm.model.xml.testmodel.instance.AnimalReference;
import org.flowable.bpm.model.xml.testmodel.instance.Animals;
import org.flowable.bpm.model.xml.testmodel.instance.Bird;
import org.flowable.bpm.model.xml.testmodel.instance.ChildRelationshipDefinition;
import org.flowable.bpm.model.xml.testmodel.instance.Description;
import org.flowable.bpm.model.xml.testmodel.instance.Egg;
import org.flowable.bpm.model.xml.testmodel.instance.FlightInstructor;
import org.flowable.bpm.model.xml.testmodel.instance.FlightPartnerRef;
import org.flowable.bpm.model.xml.testmodel.instance.FlyingAnimal;
import org.flowable.bpm.model.xml.testmodel.instance.FriendRelationshipDefinition;
import org.flowable.bpm.model.xml.testmodel.instance.GuardEgg;
import org.flowable.bpm.model.xml.testmodel.instance.Guardian;
import org.flowable.bpm.model.xml.testmodel.instance.Mother;
import org.flowable.bpm.model.xml.testmodel.instance.RelationshipDefinition;
import org.flowable.bpm.model.xml.testmodel.instance.RelationshipDefinitionRef;
import org.flowable.bpm.model.xml.testmodel.instance.SpouseRef;
import org.flowable.bpm.model.xml.testmodel.instance.Wings;

public final class TestModel {

    private static Model model;
    private static ModelBuilder modelBuilder;

    private TestModel() {}

    public static Model getTestModel() {
        if (model == null) {
            ModelBuilder modelBuilder = getModelBuilder();

            Animals.registerType(modelBuilder);
            Animal.registerType(modelBuilder);
            AnimalReference.registerType(modelBuilder);
            Bird.registerType(modelBuilder);
            ChildRelationshipDefinition.registerType(modelBuilder);
            Description.registerType(modelBuilder);
            FlightPartnerRef.registerType(modelBuilder);
            FlyingAnimal.registerType(modelBuilder);
            Guardian.registerType(modelBuilder);
            GuardEgg.registerType(modelBuilder);
            Mother.registerType(modelBuilder);
            SpouseRef.registerType(modelBuilder);
            FriendRelationshipDefinition.registerType(modelBuilder);
            RelationshipDefinition.registerType(modelBuilder);
            RelationshipDefinitionRef.registerType(modelBuilder);
            Egg.registerType(modelBuilder);
            FlightInstructor.registerType(modelBuilder);

            Wings.registerType(modelBuilder);

            model = modelBuilder.build();
        }

        return model;
    }

    public static ModelBuilder getModelBuilder() {
        if (modelBuilder == null) {
            modelBuilder = ModelBuilder.createInstance(TestModelConstants.MODEL_NAME);
        }
        return modelBuilder;
    }

}
