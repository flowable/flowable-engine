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
package org.flowable.cmmn.engine.impl.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnDeploymentEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnResourceEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricMilestoneInstanceEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntityImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.SentryPartInstanceEntityImpl;
import org.flowable.engine.common.impl.persistence.entity.Entity;
import org.flowable.identitylink.service.impl.persistence.entity.HistoricIdentityLinkEntityImpl;
import org.flowable.identitylink.service.impl.persistence.entity.IdentityLinkEntityImpl;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntityImpl;
import org.flowable.variable.service.impl.persistence.entity.VariableByteArrayEntityImpl;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntityImpl;

/**
 * @author Joram Barrez
 */
public class EntityDependencyOrder {

    public static List<Class<? extends Entity>> DELETE_ORDER = new ArrayList<>();
    public static List<Class<? extends Entity>> INSERT_ORDER = new ArrayList<>();

    static {

        DELETE_ORDER.add(HistoricIdentityLinkEntityImpl.class);
        DELETE_ORDER.add(HistoricMilestoneInstanceEntityImpl.class);
        DELETE_ORDER.add(HistoricCaseInstanceEntityImpl.class);
        DELETE_ORDER.add(VariableInstanceEntityImpl.class);
        DELETE_ORDER.add(VariableByteArrayEntityImpl.class);
        DELETE_ORDER.add(HistoricVariableInstanceEntityImpl.class);
        DELETE_ORDER.add(IdentityLinkEntityImpl.class);
        DELETE_ORDER.add(MilestoneInstanceEntityImpl.class);
        DELETE_ORDER.add(SentryPartInstanceEntityImpl.class);
        DELETE_ORDER.add(PlanItemInstanceEntityImpl.class);
        DELETE_ORDER.add(CaseInstanceEntityImpl.class);
        DELETE_ORDER.add(CaseDefinitionEntityImpl.class);
        DELETE_ORDER.add(CmmnResourceEntityImpl.class);
        DELETE_ORDER.add(CmmnDeploymentEntityImpl.class);
        
        INSERT_ORDER = new ArrayList<>(DELETE_ORDER);
        Collections.reverse(INSERT_ORDER);

    }
    
}
