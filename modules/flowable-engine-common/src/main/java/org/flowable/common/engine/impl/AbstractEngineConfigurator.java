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
package org.flowable.common.engine.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.ibatis.type.TypeAliasRegistry;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.db.DbSqlSessionFactory;
import org.flowable.common.engine.impl.db.MybatisTypeAliasConfigurator;
import org.flowable.common.engine.impl.db.MybatisTypeHandlerConfigurator;
import org.flowable.common.engine.impl.persistence.entity.Entity;
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
public abstract class AbstractEngineConfigurator implements EngineConfigurator {

    protected boolean enableMybatisXmlMappingValidation;

    @Override
    public void beforeInit(AbstractEngineConfiguration engineConfiguration) {
        registerCustomDeployers(engineConfiguration);
        registerCustomMybatisMappings(engineConfiguration);

        List<MybatisTypeAliasConfigurator> typeAliasConfigs = getMybatisTypeAliases();
        if (typeAliasConfigs != null) {
            for (MybatisTypeAliasConfigurator customMybatisTypeAliasConfig : typeAliasConfigs) {
                if (engineConfiguration.getDependentEngineMybatisTypeAliasConfigs() == null) {
                    engineConfiguration.setDependentEngineMybatisTypeAliasConfigs(new ArrayList<>());
                }
                engineConfiguration.getDependentEngineMybatisTypeAliasConfigs().add(customMybatisTypeAliasConfig);
            }
        }

        List<MybatisTypeHandlerConfigurator> typeHandlerConfigs = getMybatisTypeHandlers();
        if (typeHandlerConfigs != null) {
            for (MybatisTypeHandlerConfigurator typeHandler : typeHandlerConfigs) {
                if (engineConfiguration.getDependentEngineMybatisTypeHandlerConfigs() == null) {
                    engineConfiguration.setDependentEngineMybatisTypeHandlerConfigs(new ArrayList<>());
                }
                engineConfiguration.getDependentEngineMybatisTypeHandlerConfigs() .add(typeHandler);
            }
        }
    }

    protected void registerCustomDeployers(AbstractEngineConfiguration engineConfiguration) {
        List<EngineDeployer> deployers = getCustomDeployers();
        if (deployers != null) {
            if (engineConfiguration.getCustomPostDeployers() == null) {
                engineConfiguration.setCustomPostDeployers(new ArrayList<>());
            }
            engineConfiguration.getCustomPostDeployers().addAll(deployers);
        }
    }

    protected abstract List<EngineDeployer> getCustomDeployers();

    /**
     * @return The path to the Mybatis cfg file that's normally used for the engine (so the full cfg, not an individual mapper).
     *         Return null in case no custom mappers should be loaded.
     */
    protected abstract String getMybatisCfgPath();

    protected void registerCustomMybatisMappings(AbstractEngineConfiguration engineConfiguration) {
        String cfgPath = getMybatisCfgPath();
        if (cfgPath != null) {
            Set<String> resources = new HashSet<>();

            ClassLoader classLoader = engineConfiguration.getClassLoader();
            if (classLoader == null) {
                classLoader = this.getClass().getClassLoader();
            }

            List<MybatisTypeAliasConfigurator> typeAliasConfigurators = new ArrayList<>();
            List<MybatisTypeHandlerConfigurator> typeHandlerConfigurators = new ArrayList<>();
            try (InputStream inputStream = classLoader.getResourceAsStream(cfgPath)) {
                DocumentBuilderFactory docBuilderFactory = createDocumentBuilderFactory();
                DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
                Document document = docBuilder.parse(inputStream);
                
                NodeList typeAliasList = document.getElementsByTagName("typeAlias");
                for (int i = 0; i < typeAliasList.getLength(); i++) {
                    Node node = typeAliasList.item(i);
                    MybatisTypeAliasConfigurator typeAlias = new MybatisTypeAliasConfigurator() {
                        @Override
                        public void configure(TypeAliasRegistry typeAliasRegistry) {
                            try {
                                typeAliasRegistry.registerAlias(node.getAttributes().getNamedItem("alias").getTextContent(), 
                                                Class.forName(node.getAttributes().getNamedItem("type").getTextContent()));
                            } catch (Exception e) {
                                throw new FlowableException("Failed to load type alias class", e);
                            }
                        }
                    };
                    typeAliasConfigurators.add(typeAlias);
                    
                }
                
                NodeList typeHandlerList = document.getElementsByTagName("tagHandler");
                for (int i = 0; i < typeHandlerList.getLength(); i++) {
                    Node node = typeHandlerList.item(i);
                    MybatisTypeHandlerConfigurator typeHandler = new MybatisTypeHandlerConfigurator() {
                        @Override
                        public void configure(TypeHandlerRegistry typeHandlerRegistry) {
                            try {
                                typeHandlerRegistry.register(node.getAttributes().getNamedItem("javaType").getTextContent(),
                                                node.getAttributes().getNamedItem("handler").getTextContent());
                            } catch (Exception e) {
                                throw new FlowableException("Failed to load type handler class", e);
                            }
                        }
                    };
                    typeHandlerConfigurators.add(typeHandler);
                }
                
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
            
            if (typeAliasConfigurators.size() > 0) {
                if (engineConfiguration.getDependentEngineMybatisTypeAliasConfigs() == null) {
                    engineConfiguration.setDependentEngineMybatisTypeAliasConfigs(typeAliasConfigurators);
                    
                } else {
                    engineConfiguration.getDependentEngineMybatisTypeAliasConfigs().addAll(typeAliasConfigurators);
                }
            }
            
            if (typeHandlerConfigurators.size() > 0) {
                if (engineConfiguration.getDependentEngineMybatisTypeHandlerConfigs() == null) {
                    engineConfiguration.setDependentEngineMybatisTypeHandlerConfigs(typeHandlerConfigurators);
                    
                } else {
                    engineConfiguration.getDependentEngineMybatisTypeHandlerConfigs().addAll(typeHandlerConfigurators);
                }
            }

            if (engineConfiguration.getCustomMybatisXMLMappers() == null) {
                engineConfiguration.setCustomMybatisXMLMappers(resources);
            } else {
                engineConfiguration.getCustomMybatisXMLMappers().addAll(resources);
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
    protected List<MybatisTypeAliasConfigurator> getMybatisTypeAliases() {
        return null;
    }

    /**
     * Override when custom type handlers are needed.
     */
    protected List<MybatisTypeHandlerConfigurator> getMybatisTypeHandlers() {
        return null;
    }

    protected void initialiseCommonProperties(AbstractEngineConfiguration engineConfiguration, AbstractEngineConfiguration targetEngineConfiguration) {
        initEngineConfigurations(engineConfiguration, targetEngineConfiguration);
        initCommandContextFactory(engineConfiguration, targetEngineConfiguration);
        initIdGenerator(engineConfiguration, targetEngineConfiguration);

        if (targetEngineConfiguration.isUsingRelationalDatabase()) {
            initDataSource(engineConfiguration, targetEngineConfiguration);
            initDbSqlSessionFactory(engineConfiguration, targetEngineConfiguration);
            initDbProperties(engineConfiguration, targetEngineConfiguration);
        }

        initSessionFactories(engineConfiguration, targetEngineConfiguration);
        initEventDispatcher(engineConfiguration, targetEngineConfiguration);
        initClock(engineConfiguration, targetEngineConfiguration);
    }

    protected void initEngineConfigurations(AbstractEngineConfiguration engineConfiguration, AbstractEngineConfiguration targetEngineConfiguration) {
        targetEngineConfiguration.setEngineConfigurations(engineConfiguration.getEngineConfigurations());
    }

    protected void initServiceConfigurations(AbstractEngineConfiguration engineConfiguration, AbstractEngineConfiguration targetEngineConfiguration) {
        for (String serviceConfigurationKey : engineConfiguration.getServiceConfigurations().keySet()) {
            if (targetEngineConfiguration.getServiceConfigurations() == null
                    || !targetEngineConfiguration.getServiceConfigurations().containsKey(serviceConfigurationKey)) {
                targetEngineConfiguration.addServiceConfiguration(serviceConfigurationKey, engineConfiguration.getServiceConfigurations().get(serviceConfigurationKey));
            }
        }
    }

    protected void initCommandContextFactory(AbstractEngineConfiguration engineConfiguration, AbstractEngineConfiguration targetEngineConfiguration) {
        targetEngineConfiguration.setCommandContextFactory(engineConfiguration.getCommandContextFactory());
    }

    protected void initIdGenerator(AbstractEngineConfiguration engineConfiguration, AbstractEngineConfiguration targetEngineConfiguration) {
        if (targetEngineConfiguration.getIdGenerator() == null) {
            targetEngineConfiguration.setIdGenerator(engineConfiguration.getIdGenerator());
        }
    }

    protected void initDataSource(AbstractEngineConfiguration engineConfiguration, AbstractEngineConfiguration targetEngineConfiguration) {
        if (engineConfiguration.getDataSource() != null) {
            targetEngineConfiguration.setDataSource(engineConfiguration.getDataSource());
        } else {
            throw new FlowableException("A datasource is required for initializing the IDM engine ");
        }
    }

    protected void initDbSqlSessionFactory(AbstractEngineConfiguration engineConfiguration, AbstractEngineConfiguration targetEngineConfiguration) {
        DbSqlSessionFactory dbSqlSessionFactory = engineConfiguration.getDbSqlSessionFactory();
        targetEngineConfiguration.setDbSqlSessionFactory(engineConfiguration.getDbSqlSessionFactory());
        targetEngineConfiguration.setSqlSessionFactory(engineConfiguration.getSqlSessionFactory());

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

    protected void initSessionFactories(AbstractEngineConfiguration engineConfiguration, AbstractEngineConfiguration targetEngineConfiguration) {
        targetEngineConfiguration.setSessionFactories(engineConfiguration.getSessionFactories());
    }

    protected void initDbProperties(AbstractEngineConfiguration engineConfiguration, AbstractEngineConfiguration targetEngineConfiguration) {
        targetEngineConfiguration.setDatabaseType(engineConfiguration.getDatabaseType());
        targetEngineConfiguration.setDatabaseCatalog(engineConfiguration.getDatabaseCatalog());
        targetEngineConfiguration.setDatabaseSchema(engineConfiguration.getDatabaseSchema());
        targetEngineConfiguration.setDatabaseSchemaUpdate(engineConfiguration.getDatabaseSchemaUpdate());
        targetEngineConfiguration.setDatabaseTablePrefix(engineConfiguration.getDatabaseTablePrefix());
        targetEngineConfiguration.setDatabaseWildcardEscapeCharacter(engineConfiguration.getDatabaseWildcardEscapeCharacter());
        targetEngineConfiguration.setDefaultCommandConfig(engineConfiguration.getDefaultCommandConfig());
        targetEngineConfiguration.setSchemaCommandConfig(engineConfiguration.getSchemaCommandConfig());
        targetEngineConfiguration.setTransactionFactory(engineConfiguration.getTransactionFactory());
        targetEngineConfiguration.setTransactionContextFactory(engineConfiguration.getTransactionContextFactory());
        targetEngineConfiguration.setTransactionsExternallyManaged(engineConfiguration.isTransactionsExternallyManaged());
    }

    protected void initEventDispatcher(AbstractEngineConfiguration engineConfiguration, AbstractEngineConfiguration targetEngineConfiguration) {
        if (engineConfiguration.getEventDispatcher() != null) {
            targetEngineConfiguration.setEventDispatcher(engineConfiguration.getEventDispatcher());
        }
    }

    protected void initClock(AbstractEngineConfiguration engineConfiguration, AbstractEngineConfiguration targetEngineConfiguration) {
        targetEngineConfiguration.setClock(engineConfiguration.getClock());
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
