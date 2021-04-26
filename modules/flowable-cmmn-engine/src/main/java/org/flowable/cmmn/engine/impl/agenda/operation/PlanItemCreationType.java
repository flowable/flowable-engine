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
package org.flowable.cmmn.engine.impl.agenda.operation;

/**
 * The type returned when evaluating a plan items control when creating or reactivating it during case model evaluation.
 *
 * @author Micha Kiener
 */
public class PlanItemCreationType {

    protected boolean typeActivate;
    protected boolean typeIgnore;
    protected boolean typeDefault;

    public boolean isTypeActivate() {
        return typeActivate;
    }
    public void setTypeActivate(boolean typeActivate) {
        this.typeActivate = typeActivate;
    }
    public boolean isTypeIgnore() {
        return typeIgnore;
    }
    public void setTypeIgnore(boolean typeIgnore) {
        this.typeIgnore = typeIgnore;
    }
    public boolean isTypeDefault() {
        return typeDefault;
    }
    public void setTypeDefault(boolean typeDefault) {
        this.typeDefault = typeDefault;
    }

    public static PlanItemCreationType typeActivate() {
        PlanItemCreationType type = new PlanItemCreationType();
        type.setTypeActivate(true);
        return type;
    }

    public static PlanItemCreationType typeIgnore() {
        PlanItemCreationType type = new PlanItemCreationType();
        type.setTypeIgnore(true);
        return type;
    }

    public static PlanItemCreationType typeDefault() {
        PlanItemCreationType type = new PlanItemCreationType();
        type.setTypeDefault(true);
        return type;
    }
}
