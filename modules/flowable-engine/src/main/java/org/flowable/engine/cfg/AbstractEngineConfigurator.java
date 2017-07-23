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
package org.flowable.engine.cfg;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.flowable.engine.common.AbstractEngineConfiguration;
import org.flowable.engine.common.api.FlowableException;
import org.flowable.engine.common.impl.db.CustomMyBatisTypeHandlerConfig;
import org.flowable.engine.common.impl.db.CustomMybatisTypeAliasConfig;
import org.flowable.engine.common.impl.db.DbSqlSessionFactory;
import org.flowable.engine.common.impl.persistence.entity.Entity;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.deploy.Deployer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Convenience class for external engines (IDM/DMN/Form/...) to work together with the process engine
 * while also sharing as much internal resources as possible.
 * 
 * @author Joram Barrez
 */
public abstract class AbstractEngineConfigurator implements ProcessEngineConfigurator {
    
    protected boolean enableMybatisXmlMappingValidation;
    
    @Override
    public void beforeInit(ProcessEngineConfigurationImpl processEngineConfiguration) {
        registerCustomDeployers(processEngineConfiguration);
        registerCustomMybatisMappings(processEngineConfiguration);

        List<CustomMybatisTypeAliasConfig> typeAliasConfigs = getMybatisTypeAliases();
        if (typeAliasConfigs != null) {
            for (CustomMybatisTypeAliasConfig customMybatisTypeAliasConfig : typeAliasConfigs) {
                if (processEngineConfiguration.getDependentEngineMybatisTypeAliasConfigs() == null) {
                    processEngineConfiguration.setDependentEngineMybatisTypeAliasConfigs(new ArrayList<CustomMybatisTypeAliasConfig>());
                }
                processEngineConfiguration.getDependentEngineMybatisTypeAliasConfigs().add(customMybatisTypeAliasConfig);
            }
        }
        
        List<CustomMyBatisTypeHandlerConfig> typeHandlerConfigs = getMybatisTypeHandlers();
        if (typeHandlerConfigs != null) {
            for (CustomMyBatisTypeHandlerConfig typeHandler : typeHandlerConfigs) {
                if (processEngineConfiguration.getDependentEngineMybatisTypeHandlerConfigs() == null) {
                    processEngineConfiguration.setDependentEngineMybatisTypeHandlerConfigs(new ArrayList<CustomMyBatisTypeHandlerConfig>());
                }
                processEngineConfiguration.getDependentEngineMybatisTypeHandlerConfigs() .add(typeHandler);
            }
        }
    }

    protected void registerCustomDeployers(ProcessEngineConfigurationImpl processEngineConfiguration) {
        List<Deployer> deployers = getCustomDeployers();
        if (deployers != null) {
            if (processEngineConfiguration.getCustomPostDeployers() == null) {
                processEngineConfiguration.setCustomPostDeployers(new ArrayList<Deployer>());
            }
            processEngineConfiguration.getCustomPostDeployers().addAll(deployers);
        }
    }
    
    protected abstract List<Deployer> getCustomDeployers();
    
    /**
     * @return The path to the Mybatis cfg file that's normally used for the engine (so the full cfg, not an individual mapper).
     *         Return null in case no custom mappers should be loaded.
     */
    protected abstract String getMybatisCfgPath();
    
    protected void registerCustomMybatisMappings(ProcessEngineConfigurationImpl processEngineConfiguration) {
        String cfgPath = getMybatisCfgPath();
        if (cfgPath != null) {
            Set<String> resources = new HashSet<>();
            try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(cfgPath)) {
                DocumentBuilderFactory docBuilderFactory = createDocumentBuilderFactory();
                DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
                Document document = docBuilder.parse(inputStream);
                NodeList nodeList = document.getElementsByTagName("mapper");
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Node node = nodeList.item(i);
                    resources.add(node.getAttributes().getNamedItem("resource").getTextContent());
                }
            } catch (IOException e) {
                throw new FlowableException("Could not read IDM Mybatis configuration file", e);
            } catch (ParserConfigurationException | SAXException e) {
                throw new FlowableException("Could not parse Mybatis configuration file", e);
            }
            
            if (processEngineConfiguration.getCustomMybatisXMLMappers() == null) {
                processEngineConfiguration.setCustomMybatisXMLMappers(resources);
            } else {
                processEngineConfiguration.getCustomMybatisXMLMappers().addAll(resources);
            }
        }
    }
    
    protected DocumentBuilderFactory createDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        if (!enableMybatisXmlMappingValidation) {
            docBuilderFactory.setValidating(false);
            docBuilderFactory.setNamespaceAware(false);
            docBuilderFactory.setExpandEntityReferences(false);
            docBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            docBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        }
        return docBuilderFactory;
    }
    
    /**
     * Override when custom type aliases are needed.
     */
    protected List<CustomMybatisTypeAliasConfig> getMybatisTypeAliases() {
        return null;
    }
    
    /**
     * Override when custom type handlers are needed.
     */
    protected List<CustomMyBatisTypeHandlerConfig> getMybatisTypeHandlers() {
        return null;
    }

    protected void initialiseCommonProperties(ProcessEngineConfigurationImpl processEngineConfiguration, AbstractEngineConfiguration targetEngineConfiguration, String engineKey) {
        initEngineConfigurations(processEngineConfiguration, targetEngineConfiguration, engineKey);
        initCommandContextFactory(processEngineConfiguration, targetEngineConfiguration);
        initDataSource(processEngineConfiguration, targetEngineConfiguration);
        initDbSqlSessionFactory(processEngineConfiguration, targetEngineConfiguration);
        initSessionFactories(processEngineConfiguration, targetEngineConfiguration);
        initDbProperties(processEngineConfiguration, targetEngineConfiguration);
        initEventDispatcher(processEngineConfiguration, targetEngineConfiguration);
        initClock(processEngineConfiguration, targetEngineConfiguration);
    }

    protected void initEngineConfigurations(ProcessEngineConfigurationImpl processEngineConfiguration, AbstractEngineConfiguration targetEngineConfiguration, String engineKey) {
        targetEngineConfiguration.setEngineConfigurations(processEngineConfiguration.getEngineConfigurations());
    }

    protected void initCommandContextFactory(ProcessEngineConfigurationImpl processEngineConfiguration, AbstractEngineConfiguration targetEngineConfiguration) {
        targetEngineConfiguration.setCommandContextFactory(processEngineConfiguration.getCommandContextFactory());
    }

    protected void initDataSource(ProcessEngineConfigurationImpl processEngineConfiguration,
            AbstractEngineConfiguration targetEngineConfiguration) {
        if (processEngineConfiguration.getDataSource() != null) {
            targetEngineConfiguration.setDataSource(processEngineConfiguration.getDataSource());
        } else {
            throw new FlowableException("A datasource is required for initializing the IDM engine ");
        }
    }

    protected void initDbSqlSessionFactory(ProcessEngineConfigurationImpl processEngineConfiguration, AbstractEngineConfiguration targetEngineConfiguration) {
        DbSqlSessionFactory dbSqlSessionFactory = processEngineConfiguration.getDbSqlSessionFactory();
        targetEngineConfiguration.setDbSqlSessionFactory(processEngineConfiguration.getDbSqlSessionFactory());
        targetEngineConfiguration.setSqlSessionFactory(processEngineConfiguration.getSqlSessionFactory());
        
        if (getEntityInsertionOrder() != null) {
            for (Class<? extends Entity> clazz : getEntityInsertionOrder()) {
                dbSqlSessionFactory.getInsertionOrder().add(clazz);
            }
        }
        
        if (getEntityDeletionOrder() != null) {
            for (Class<? extends Entity> clazz : getEntityDeletionOrder()) {
                dbSqlSessionFactory.getDeletionOrder().add(clazz);
            }
        }
    }

    protected void initSessionFactories(ProcessEngineConfigurationImpl processEngineConfiguration, AbstractEngineConfiguration targetEngineConfiguration) {
        targetEngineConfiguration.setSessionFactories(processEngineConfiguration.getSessionFactories());
    }

    protected void initDbProperties(ProcessEngineConfigurationImpl processEngineConfiguration, AbstractEngineConfiguration targetEngineConfiguration) {
        targetEngineConfiguration.setDatabaseType(processEngineConfiguration.getDatabaseType());
        targetEngineConfiguration.setDatabaseCatalog(processEngineConfiguration.getDatabaseCatalog());
        targetEngineConfiguration.setDatabaseSchema(processEngineConfiguration.getDatabaseSchema());
        targetEngineConfiguration.setDatabaseSchemaUpdate(processEngineConfiguration.getDatabaseSchemaUpdate());
        targetEngineConfiguration.setDatabaseTablePrefix(processEngineConfiguration.getDatabaseTablePrefix());
        targetEngineConfiguration.setDatabaseWildcardEscapeCharacter(processEngineConfiguration.getDatabaseWildcardEscapeCharacter());
        targetEngineConfiguration.setDefaultCommandConfig(processEngineConfiguration.getDefaultCommandConfig());
        targetEngineConfiguration.setSchemaCommandConfig(processEngineConfiguration.getSchemaCommandConfig());
        targetEngineConfiguration.setTransactionFactory(processEngineConfiguration.getTransactionFactory());
        targetEngineConfiguration.setTransactionContextFactory(processEngineConfiguration.getTransactionContextFactory());
        targetEngineConfiguration.setTransactionsExternallyManaged(processEngineConfiguration.isTransactionsExternallyManaged());
    }

    protected void initEventDispatcher(ProcessEngineConfigurationImpl processEngineConfiguration, AbstractEngineConfiguration targetEngineConfiguration) {
        if (processEngineConfiguration.getEventDispatcher() != null) {
            targetEngineConfiguration.setEventDispatcher(processEngineConfiguration.getEventDispatcher());
        }
    }
    
    protected void initClock(ProcessEngineConfigurationImpl processEngineConfiguration, AbstractEngineConfiguration targetEngineConfiguration) {
        targetEngineConfiguration.setClock(processEngineConfiguration.getClock());
    }
    
    protected abstract List<Class<? extends Entity>> getEntityInsertionOrder();
    
    protected abstract List<Class<? extends Entity>> getEntityDeletionOrder();

    public boolean isEnableMybatisXmlMappingValidation() {
        return enableMybatisXmlMappingValidation;
    }

    public void setEnableMybatisXmlMappingValidation(boolean enableMybatisXmlMappingValidation) {
        this.enableMybatisXmlMappingValidation = enableMybatisXmlMappingValidation;
    }
    
}
