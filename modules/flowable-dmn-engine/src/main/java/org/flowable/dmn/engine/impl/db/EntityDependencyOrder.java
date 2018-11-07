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
package org.flowable.dmn.engine.impl.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.dmn.engine.impl.persistence.entity.DecisionTableEntityImpl;
import org.flowable.dmn.engine.impl.persistence.entity.DmnDeploymentEntityImpl;
import org.flowable.dmn.engine.impl.persistence.entity.DmnResourceEntityImpl;
import org.flowable.dmn.engine.impl.persistence.entity.HistoricDecisionExecutionEntityImpl;

public class EntityDependencyOrder {

    public static List<Class<? extends Entity>> DELETE_ORDER = new ArrayList<>();
    public static List<Class<? extends Entity>> INSERT_ORDER = new ArrayList<>();

    static {

        DELETE_ORDER.add(HistoricDecisionExecutionEntityImpl.class);
        DELETE_ORDER.add(DmnResourceEntityImpl.class);
        DELETE_ORDER.add(DmnDeploymentEntityImpl.class);
        DELETE_ORDER.add(DecisionTableEntityImpl.class);
        
        INSERT_ORDER = new ArrayList<>(DELETE_ORDER);
        Collections.reverse(INSERT_ORDER);

    }
    
}
