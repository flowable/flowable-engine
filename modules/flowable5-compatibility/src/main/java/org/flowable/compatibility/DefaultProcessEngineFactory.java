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

package org.flowable.compatibility;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import javax.xml.namespace.QName;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.impl.bpmn.parser.factory.ActivityBehaviorFactory;
import org.activiti.engine.impl.bpmn.parser.factory.ListenerFactory;
import org.activiti.engine.impl.el.ExpressionManager;
import org.activiti.engine.parse.BpmnParseHandler;
import org.flowable.common.engine.impl.EngineDeployer;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.engine.impl.rules.RulesDeployer;

public class DefaultProcessEngineFactory {

    /**
     * Takes in an Flowable 6 process engine config, gives back an Flowable 5 Process engine.
     */
    public ProcessEngine buildProcessEngine(ProcessEngineConfigurationImpl flowable6Configuration) {
        org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl flowable5Configuration = null;
        if (flowable6Configuration instanceof StandaloneProcessEngineConfiguration) {
            flowable5Configuration = new org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration();
            copyConfigItems(flowable6Configuration, flowable5Configuration);
            return flowable5Configuration.buildProcessEngine();
        } else {
            throw new ActivitiException("Unsupported process engine configuration");
        }
    }

    protected void copyConfigItems(ProcessEngineConfigurationImpl flowable6Configuration, org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl flowable5Configuration) {
        flowable5Configuration.setFlowable5CompatibilityHandler(flowable6Configuration.getFlowable5CompatibilityHandler());

        copyJdbcConfig(flowable6Configuration, flowable5Configuration);
        copyHistoryConfig(flowable6Configuration, flowable5Configuration);
        copyMailConfig(flowable6Configuration, flowable5Configuration);
        copyDiagramConfig(flowable6Configuration, flowable5Configuration);
        copyAsyncExecutorConfig(flowable6Configuration, flowable5Configuration);
        copyJpaConfig(flowable6Configuration, flowable5Configuration);
        copyBeans(flowable6Configuration, flowable5Configuration);
        copyCaches(flowable6Configuration, flowable5Configuration);
        copyActivityBehaviorFactory(flowable6Configuration, flowable5Configuration);
        copyExpressionManager(flowable6Configuration, flowable5Configuration);
        copyListenerFactory(flowable6Configuration, flowable5Configuration);
        convertParseHandlers(flowable6Configuration, flowable5Configuration);
        copyCustomMybatisMappers(flowable6Configuration, flowable5Configuration);
        copyWsConfig(flowable6Configuration, flowable5Configuration);
        flowable5Configuration.setEventDispatcher(flowable6Configuration.getEventDispatcher());
        copyPostDeployers(flowable6Configuration, flowable5Configuration);
        flowable5Configuration.setBusinessCalendarManager(flowable6Configuration.getBusinessCalendarManager());
    }

    protected void copyJdbcConfig(ProcessEngineConfigurationImpl flowable6Configuration, org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl flowable5Configuration) {
        flowable5Configuration.setDataSource(flowable6Configuration.getDataSource());

        if (flowable6Configuration.getJdbcDriver() != null) {
            flowable5Configuration.setJdbcDriver(flowable6Configuration.getJdbcDriver());
        }
        if (flowable6Configuration.getJdbcUrl() != null) {
            flowable5Configuration.setJdbcUrl(flowable6Configuration.getJdbcUrl());
        }
        if (flowable6Configuration.getJdbcUsername() != null) {
            flowable5Configuration.setJdbcUsername(flowable6Configuration.getJdbcUsername());
        }
        if (flowable6Configuration.getJdbcPassword() != null) {
            flowable5Configuration.setJdbcPassword(flowable6Configuration.getJdbcPassword());
        }

        if (flowable6Configuration.getIdBlockSize() > 0) {
            flowable5Configuration.setIdBlockSize(flowable6Configuration.getIdBlockSize());
        }

        if (flowable6Configuration.getJdbcMaxActiveConnections() > 0) {
            flowable5Configuration.setJdbcMaxActiveConnections(flowable6Configuration.getJdbcMaxActiveConnections());
        }

        if (flowable6Configuration.getDatabaseTablePrefix() != null) {
            flowable5Configuration.setDatabaseTablePrefix(flowable6Configuration.getDatabaseTablePrefix());
        }
    }

    protected void copyHistoryConfig(ProcessEngineConfigurationImpl flowable6Configuration, org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl flowable5Configuration) {
        flowable5Configuration.setHistoryLevel(HistoryLevel.getHistoryLevelForKey(flowable6Configuration.getHistoryLevel().getKey()));
    }

    protected void copyDiagramConfig(ProcessEngineConfigurationImpl flowable6Configuration, org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl flowable5Configuration) {
        flowable5Configuration.setCreateDiagramOnDeploy(flowable6Configuration.isCreateDiagramOnDeploy());
        flowable5Configuration.setActivityFontName(flowable6Configuration.getActivityFontName());
        flowable5Configuration.setLabelFontName(flowable6Configuration.getLabelFontName());
        flowable5Configuration.setAnnotationFontName(flowable6Configuration.getAnnotationFontName());
    }

    protected void copyMailConfig(ProcessEngineConfigurationImpl flowable6Configuration, org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl flowable5Configuration) {
        flowable5Configuration.setMailServerDefaultFrom(flowable6Configuration.getMailServerDefaultFrom());
        flowable5Configuration.setMailServerHost(flowable6Configuration.getMailServerHost());
        flowable5Configuration.setMailServerPassword(flowable6Configuration.getMailServerPassword());
        flowable5Configuration.setMailServerPort(flowable6Configuration.getMailServerPort());
        flowable5Configuration.setMailServerUsername(flowable6Configuration.getMailServerUsername());
        flowable5Configuration.setMailServerUseSSL(flowable6Configuration.getMailServerUseSSL());
        flowable5Configuration.setMailServerUseTLS(flowable6Configuration.getMailServerUseTLS());
        if (flowable6Configuration.getMailServers() != null && flowable6Configuration.getMailServers().size() > 0) {
            flowable5Configuration.getMailServers().putAll(flowable6Configuration.getMailServers());
        }

        if (flowable6Configuration.getMailSessionJndi() != null) {
            flowable5Configuration.setMailSessionJndi(flowable6Configuration.getMailSessionJndi());
        }
    }

    protected void copyAsyncExecutorConfig(ProcessEngineConfigurationImpl flowable6Configuration, org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl flowable5Configuration) {
        if (flowable6Configuration.isAsyncExecutorActivate()) {
            flowable5Configuration.setAsyncExecutorActivate(true);
        }
        flowable5Configuration.setDefaultFailedJobWaitTime(flowable6Configuration.getDefaultFailedJobWaitTime());
        flowable5Configuration.setAsyncFailedJobWaitTime(flowable6Configuration.getAsyncFailedJobWaitTime());
        flowable5Configuration.setAsyncExecutor(flowable6Configuration.getAsyncExecutor());
    }

    protected void copyJpaConfig(ProcessEngineConfigurationImpl flowable6Configuration, org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl flowable5Configuration) {
        flowable5Configuration.setJpaCloseEntityManager(flowable6Configuration.isJpaCloseEntityManager());
        flowable5Configuration.setJpaHandleTransaction(flowable6Configuration.isJpaHandleTransaction());

        // We want to reuse the entity manager factory between the two engines
        if (flowable6Configuration.getJpaEntityManagerFactory() != null) {
            flowable5Configuration.setJpaEntityManagerFactory(flowable6Configuration.getJpaEntityManagerFactory());
        } else {
            flowable5Configuration.setJpaPersistenceUnitName(flowable6Configuration.getJpaPersistenceUnitName());
        }
    }

    protected void copyBeans(ProcessEngineConfigurationImpl flowable6Configuration, org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl flowable5Configuration) {
        if (flowable6Configuration.getBeans() != null) {
            flowable5Configuration.setBeans(flowable6Configuration.getBeans());
        }
    }

    protected void copyCaches(ProcessEngineConfigurationImpl flowable6Configuration, org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl flowable5Configuration) {
        flowable5Configuration.setProcessDefinitionCacheLimit(flowable6Configuration.getProcessDefinitionCacheLimit());
        flowable5Configuration.setEnableProcessDefinitionInfoCache(flowable6Configuration.isEnableProcessDefinitionInfoCache());
        flowable5Configuration.setProcessDefinitionCache(flowable6Configuration.getProcessDefinitionCache());

        flowable5Configuration.setKnowledgeBaseCacheLimit(flowable6Configuration.getKnowledgeBaseCacheLimit());
        flowable5Configuration.setKnowledgeBaseCache(flowable6Configuration.getKnowledgeBaseCache());
    }

    protected void copyActivityBehaviorFactory(ProcessEngineConfigurationImpl flowable6Configuration, org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl flowable5Configuration) {
        if (flowable6Configuration.getFlowable5ActivityBehaviorFactory() != null) {
            flowable5Configuration.setActivityBehaviorFactory((ActivityBehaviorFactory) flowable6Configuration.getFlowable5ActivityBehaviorFactory());
        }
    }

    protected void copyExpressionManager(ProcessEngineConfigurationImpl flowable6Configuration, org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl flowable5Configuration) {
        if (flowable6Configuration.getFlowable5ExpressionManager() != null) {
            flowable5Configuration.setExpressionManager((ExpressionManager) flowable6Configuration.getFlowable5ExpressionManager());
        }
    }

    protected void copyListenerFactory(ProcessEngineConfigurationImpl flowable6Configuration, org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl flowable5Configuration) {
        if (flowable6Configuration.getFlowable5ListenerFactory() != null) {
            flowable5Configuration.setListenerFactory((ListenerFactory) flowable6Configuration.getFlowable5ListenerFactory());
        }
    }

    protected void copyCustomMybatisMappers(ProcessEngineConfigurationImpl flowable6Configuration, org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl flowable5Configuration) {
        if (flowable6Configuration.getFlowable5CustomMybatisMappers() != null) {
            flowable5Configuration.setCustomMybatisMappers(flowable6Configuration.getFlowable5CustomMybatisMappers());
        }

        if (flowable6Configuration.getFlowable5CustomMybatisXMLMappers() != null) {
            flowable5Configuration.setCustomMybatisXMLMappers(flowable6Configuration.getFlowable5CustomMybatisXMLMappers());
        }
    }

    protected void copyWsConfig(ProcessEngineConfigurationImpl flowable6Configuration, org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl flowable5Configuration) {
        if (flowable6Configuration.getWsSyncFactoryClassName() != null) {
            flowable5Configuration.setWsSyncFactoryClassName(flowable6Configuration.getWsSyncFactoryClassName());
        }

        ConcurrentMap<QName, URL> endpointMap = flowable6Configuration.getWsOverridenEndpointAddresses();
        if (endpointMap != null) {
            for (QName endpointQName : endpointMap.keySet()) {
                flowable5Configuration.addWsEndpointAddress(endpointQName, endpointMap.get(endpointQName));
            }
        }
    }

    protected void copyPostDeployers(ProcessEngineConfigurationImpl flowable6Configuration, org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl flowable5Configuration) {
        if (flowable6Configuration.getCustomPostDeployers() != null) {
            List<org.activiti.engine.impl.persistence.deploy.Deployer> activiti5Deployers = new ArrayList<>();
            for (EngineDeployer deployer : flowable6Configuration.getCustomPostDeployers()) {
                if (deployer instanceof RulesDeployer) {
                    activiti5Deployers.add(new org.activiti.engine.impl.rules.RulesDeployer());
                    break;
                }
            }

            if (activiti5Deployers.size() > 0) {
                if (flowable5Configuration.getCustomPostDeployers() != null) {
                    flowable5Configuration.getCustomPostDeployers().addAll(activiti5Deployers);
                } else {
                    flowable5Configuration.setCustomPostDeployers(activiti5Deployers);
                }
            }
        }
    }

    protected void convertParseHandlers(ProcessEngineConfigurationImpl flowable6Configuration, org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl flowable5Configuration) {
        flowable5Configuration.setPreBpmnParseHandlers(convert(flowable6Configuration.getFlowable5PreBpmnParseHandlers()));
        flowable5Configuration.setPostBpmnParseHandlers(convert(flowable6Configuration.getFlowable5PostBpmnParseHandlers()));
        flowable5Configuration.setCustomDefaultBpmnParseHandlers(convert(flowable6Configuration.getFlowable5CustomDefaultBpmnParseHandlers()));
    }

    protected List<BpmnParseHandler> convert(List<Object> activiti5BpmnParseHandlers) {
        if (activiti5BpmnParseHandlers == null) {
            return null;
        }

        List<BpmnParseHandler> parseHandlers = new ArrayList<>(activiti5BpmnParseHandlers.size());
        for (Object activiti6BpmnParseHandler : activiti5BpmnParseHandlers) {
            parseHandlers.add((BpmnParseHandler) activiti6BpmnParseHandler);
        }
        return parseHandlers;
    }

}
