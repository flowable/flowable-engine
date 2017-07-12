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
package org.flowable.engine.impl.cfg;

import javax.sql.DataSource;

import org.flowable.engine.cfg.AbstractProcessEngineConfigurator;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.impl.transaction.TransactionContextAwareDataSource;
import org.flowable.engine.common.impl.transaction.TransactionContextAwareTransactionFactory;
import org.flowable.idm.engine.IdmEngine;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.impl.cfg.StandaloneIdmEngineConfiguration;

/**
 * @author Tijs Rademakers
 */
public class IdmEngineConfigurator extends AbstractProcessEngineConfigurator {

    protected IdmEngineConfiguration idmEngineConfiguration;

    @Override
    public void configure(ProcessEngineConfigurationImpl processEngineConfiguration) {
        if (idmEngineConfiguration == null) {
            idmEngineConfiguration = new StandaloneIdmEngineConfiguration();

            if (processEngineConfiguration.getDataSource() != null) {
                DataSource originalDatasource = processEngineConfiguration.getDataSource();
                if (processEngineConfiguration.isTransactionsExternallyManaged()) {
                    idmEngineConfiguration.setDataSource(originalDatasource);
                } else {
                    idmEngineConfiguration.setDataSource(new TransactionContextAwareDataSource(originalDatasource));
                }

            } else {
                throw new FlowableException("A datasource is required for initializing the IDM engine ");
            }

            idmEngineConfiguration.setDatabaseType(processEngineConfiguration.getDatabaseType());
            idmEngineConfiguration.setDatabaseCatalog(processEngineConfiguration.getDatabaseCatalog());
            idmEngineConfiguration.setDatabaseSchema(processEngineConfiguration.getDatabaseSchema());
            idmEngineConfiguration.setDatabaseSchemaUpdate(processEngineConfiguration.getDatabaseSchemaUpdate());
            idmEngineConfiguration.setDatabaseTablePrefix(processEngineConfiguration.getDatabaseTablePrefix());
            idmEngineConfiguration.setDatabaseWildcardEscapeCharacter(processEngineConfiguration.getDatabaseWildcardEscapeCharacter());

            if (processEngineConfiguration.isTransactionsExternallyManaged()) {
                idmEngineConfiguration.setTransactionsExternallyManaged(true);
            } else {
                idmEngineConfiguration.setTransactionFactory(
                        new TransactionContextAwareTransactionFactory<org.flowable.idm.engine.impl.cfg.TransactionContext>(
                                org.flowable.idm.engine.impl.cfg.TransactionContext.class));
            }

            if (processEngineConfiguration.getEventDispatcher() != null) {
                idmEngineConfiguration.setEventDispatcher(processEngineConfiguration.getEventDispatcher());
            }

        }

        IdmEngine idmEngine = idmEngineConfiguration.buildIdmEngine();

        processEngineConfiguration.setIdmEngineInitialized(true);
        processEngineConfiguration.setIdmIdentityService(idmEngine.getIdmIdentityService());
    }

    @Override
    public void beforeInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        // Nothing to do in the before init: we boot up the IDM engine once the process engine is ready
    }

    public IdmEngineConfiguration getIdmEngineConfiguration() {
        return idmEngineConfiguration;
    }

    public IdmEngineConfigurator setIdmEngineConfiguration(IdmEngineConfiguration idmEngineConfiguration) {
        this.idmEngineConfiguration = idmEngineConfiguration;
        return this;
    }

}
