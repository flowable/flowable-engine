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
package org.flowable.spring.boot;

import java.util.Arrays;
import java.util.List;

import org.flowable.common.engine.impl.history.HistoryLevel;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;

/**
 * @author Josh Long
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
@ConfigurationProperties(prefix = "flowable")
public class FlowableProperties {

    /**
     * Whether process definitions need to be auto deployed.
     */
    private boolean checkProcessDefinitions = true;

    /**
     * Whether the async executor should be activated.
     */
    private boolean asyncExecutorActivate = true;
    
    /**
     * Whether the async history executor should be activated.
     */
    private boolean asyncHistoryExecutorActivate = true;
    
    private boolean restApiEnabled;

    /**
     * The name of the auto deployment.
     */
    private String deploymentName = "SpringBootAutoDeployment";

    /**
     * The strategy that should be used for the database schema.
     */
    private String databaseSchemaUpdate = "true";

    /**
     * In some situations you want to set the schema to use for table checks / generation if the database metadata doesn't return that correctly.
     */
    private String databaseSchema;
    /**
     * @deprecated use {@link org.flowable.spring.boot.idm.FlowableIdmProperties#enabled}
     */
    @Deprecated
    private boolean isDbIdentityUsed = true;

    /**
     * Whether db history should be used.
     */
    private boolean isDbHistoryUsed = true;

    /**
     * The history level that needs to be used.
     */
    private HistoryLevel historyLevel = HistoryLevel.AUDIT;

    /**
     * The folder in which processes need to be searched for auto deployment.
     */
    private String processDefinitionLocationPrefix = "classpath*:/processes/";

    /**
     * The suffixes (extensions) of the files that needs to be deployed from the 'processDefinitionLocationPrefix' location.
     */
    private List<String> processDefinitionLocationSuffixes = Arrays.asList("**.bpmn20.xml", "**.bpmn");
    private boolean jpaEnabled = true; // true by default

    /**
     * The FQN of custom Mybatis mappers that need to be added to the engine.
     */
    private List<String> customMybatisMappers;

    /**
     * The location of the custom Mybatis XML Mappers that need to be added to the engine.
     */
    private List<String> customMybatisXMLMappers;

    public boolean isAsyncExecutorActivate() {
        return asyncExecutorActivate;
    }

    public void setAsyncExecutorActivate(boolean asyncExecutorActivate) {
        this.asyncExecutorActivate = asyncExecutorActivate;
    }
    
    public boolean isAsyncHistoryExecutorActivate() {
        return asyncHistoryExecutorActivate;
    }

    public void setAsyncHistoryExecutorActivate(boolean asyncHistoryExecutorActivate) {
        this.asyncHistoryExecutorActivate = asyncHistoryExecutorActivate;
    }

    public boolean isRestApiEnabled() {
        return restApiEnabled;
    }

    public void setRestApiEnabled(boolean restApiEnabled) {
        this.restApiEnabled = restApiEnabled;
    }

    public boolean isJpaEnabled() {
        return jpaEnabled;
    }

    public void setJpaEnabled(boolean jpaEnabled) {
        this.jpaEnabled = jpaEnabled;
    }

    /**
     * @deprecated use {@link org.flowable.spring.boot.process.FlowableProcessProperties#getServlet()#getPath()}
     */
    @DeprecatedConfigurationProperty(replacement = "flowable.process.servlet.path")
    @Deprecated
    public String getRestApiMapping() {
        throw new IllegalStateException("Usage of deprecated property. Use FlowableProcessProperties");
    }

    /**
     * @deprecated use {@link org.flowable.spring.boot.process.FlowableProcessProperties#getServlet()#setPath()}
     */
    @Deprecated
    public void setRestApiMapping(String restApiMapping) {
        throw new IllegalStateException("Usage of deprecated property. Use FlowableProcessProperties");
    }

    /**
     * @deprecated use {@link org.flowable.spring.boot.process.FlowableProcessProperties#getServlet()#getName()}
     */
    @DeprecatedConfigurationProperty(replacement = "flowable.process.servlet.name")
    @Deprecated
    public String getRestApiServletName() {
        throw new IllegalStateException("Usage of deprecated property. Use FlowableProcessProperties");
    }

    /**
     * @deprecated use {@link org.flowable.spring.boot.process.FlowableProcessProperties#getServlet()#setName()}
     */
    @Deprecated
    public void setRestApiServletName(String restApiServletName) {
        throw new IllegalStateException("Usage of deprecated property. Use FlowableProcessProperties");
    }

    public boolean isCheckProcessDefinitions() {
        return checkProcessDefinitions;
    }

    public void setCheckProcessDefinitions(boolean checkProcessDefinitions) {
        this.checkProcessDefinitions = checkProcessDefinitions;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }

    public String getDatabaseSchemaUpdate() {
        return databaseSchemaUpdate;
    }

    public void setDatabaseSchemaUpdate(String databaseSchemaUpdate) {
        this.databaseSchemaUpdate = databaseSchemaUpdate;
    }

    public String getDatabaseSchema() {
        return databaseSchema;
    }

    public void setDatabaseSchema(String databaseSchema) {
        this.databaseSchema = databaseSchema;
    }

    /**
     * @deprecated use {@link org.flowable.spring.boot.idm.FlowableIdmProperties#isEnabled()}
     */
    @DeprecatedConfigurationProperty(replacement = "flowable.idm.enabled")
    @Deprecated
    public boolean isDbIdentityUsed() {
        return isDbIdentityUsed;
    }

    /**
     * @deprecated use {@link org.flowable.spring.boot.idm.FlowableIdmProperties#setEnabled(boolean)}
     */
    @Deprecated
    public void setDbIdentityUsed(boolean isDbIdentityUsed) {
        this.isDbIdentityUsed = isDbIdentityUsed;
    }

    public boolean isDbHistoryUsed() {
        return isDbHistoryUsed;
    }

    public void setDbHistoryUsed(boolean isDbHistoryUsed) {
        this.isDbHistoryUsed = isDbHistoryUsed;
    }

    public HistoryLevel getHistoryLevel() {
        return historyLevel;
    }

    public void setHistoryLevel(HistoryLevel historyLevel) {
        this.historyLevel = historyLevel;
    }

    public String getProcessDefinitionLocationPrefix() {
        return processDefinitionLocationPrefix;
    }

    public void setProcessDefinitionLocationPrefix(
            String processDefinitionLocationPrefix) {
        this.processDefinitionLocationPrefix = processDefinitionLocationPrefix;
    }

    public List<String> getProcessDefinitionLocationSuffixes() {
        return processDefinitionLocationSuffixes;
    }

    public void setProcessDefinitionLocationSuffixes(
            List<String> processDefinitionLocationSuffixes) {
        this.processDefinitionLocationSuffixes = processDefinitionLocationSuffixes;
    }


    public List<String> getCustomMybatisMappers() {
        return customMybatisMappers;
    }

    public void setCustomMybatisMappers(List<String> customMyBatisMappers) {
        this.customMybatisMappers = customMyBatisMappers;
    }

    public List<String> getCustomMybatisXMLMappers() {
        return customMybatisXMLMappers;
    }

    public void setCustomMybatisXMLMappers(List<String> customMybatisXMLMappers) {
        this.customMybatisXMLMappers = customMybatisXMLMappers;
    }
}
