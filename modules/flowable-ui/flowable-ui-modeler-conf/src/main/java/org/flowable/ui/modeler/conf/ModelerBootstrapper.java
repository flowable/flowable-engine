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
package org.flowable.ui.modeler.conf;

import java.util.List;

import org.flowable.ui.modeler.domain.AbstractModel;
import org.flowable.ui.modeler.domain.Model;
import org.flowable.ui.modeler.properties.FlowableModelerAppProperties;
import org.flowable.ui.modeler.repository.ModelRepository;
import org.flowable.ui.modeler.repository.ModelSort;
import org.flowable.ui.modeler.service.DecisionTableModelConversionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * Responsible for executing all action required after booting up the Spring container.
 *
 * @author Yvo Swillens
 */
@Component
public class ModelerBootstrapper implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    protected ModelRepository modelRepository;

    @Autowired
    private FlowableModelerAppProperties modelerAppProperties;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() == null) { // Using Spring MVC, there are multiple child contexts. We only care about the root

            if (modelerAppProperties == null || modelerAppProperties.isDecisionTableMigrationEnabled()) {
                migrateDecisionTables();
            }
        }
    }

    public void migrateDecisionTables() {
        List<Model> decisionTableModels = modelRepository.findByModelType(AbstractModel.MODEL_TYPE_DECISION_TABLE, ModelSort.NAME_ASC);

        decisionTableModels.forEach(decisionTableModel -> {
            if (DecisionTableModelConversionUtil.convertModelToV3(decisionTableModel)) {
                modelRepository.save(decisionTableModel);
            }
        });
    }
}
