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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.StringUtils;

/**
 * Base auto configuration for the different engines.
 *
 * @author Filip Hrisafov
 * @author Javier Casal
 */
public abstract class AbstractEngineAutoConfiguration {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final FlowableProperties flowableProperties;
    protected ResourcePatternResolver resourcePatternResolver;

    public AbstractEngineAutoConfiguration(FlowableProperties flowableProperties) {
        this.flowableProperties = flowableProperties;
    }

    @Autowired
    public void setResourcePatternResolver(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    protected void configureEngine(AbstractEngineConfiguration engineConfiguration, DataSource dataSource) {

        engineConfiguration.setDataSource(dataSource);

        engineConfiguration.setDatabaseSchema(defaultText(flowableProperties.getDatabaseSchema(), engineConfiguration.getDatabaseSchema()));
        engineConfiguration.setDatabaseSchemaUpdate(defaultText(flowableProperties.getDatabaseSchemaUpdate(), engineConfiguration
            .getDatabaseSchemaUpdate()));

        engineConfiguration.setDbHistoryUsed(flowableProperties.isDbHistoryUsed());

        if (flowableProperties.getCustomMybatisMappers() != null) {
            engineConfiguration.setCustomMybatisMappers(getCustomMybatisMapperClasses(flowableProperties.getCustomMybatisMappers()));
        }

        if (flowableProperties.getCustomMybatisXMLMappers() != null) {
            engineConfiguration.setCustomMybatisXMLMappers(new HashSet<>(flowableProperties.getCustomMybatisXMLMappers()));
        }

        if (flowableProperties.getCustomMybatisMappers() != null) {
            engineConfiguration.setCustomMybatisMappers(getCustomMybatisMapperClasses(flowableProperties.getCustomMybatisMappers()));
        }

        if (flowableProperties.getCustomMybatisXMLMappers() != null) {
            engineConfiguration.setCustomMybatisXMLMappers(new HashSet<>(flowableProperties.getCustomMybatisXMLMappers()));
        }
    }

    public List<Resource> discoverDeploymentResources(String prefix, List<String> suffixes, boolean loadResources) throws IOException {
        if (loadResources) {

            List<Resource> result = new ArrayList<>();
            for (String suffix : suffixes) {
                String path = prefix + suffix;
                Resource[] resources = resourcePatternResolver.getResources(path);
                if (resources != null && resources.length > 0) {
                    Collections.addAll(result, resources);
                }
            }

            if (result.isEmpty()) {
                logger.info("No deployment resources were found for autodeployment");
            }

            return result;
        }
        return new ArrayList<>();
    }

    protected Set<Class<?>> getCustomMybatisMapperClasses(List<String> customMyBatisMappers) {
        Set<Class<?>> mybatisMappers = new HashSet<>();
        for (String customMybatisMapperClassName : customMyBatisMappers) {
            try {
                Class customMybatisClass = Class.forName(customMybatisMapperClassName);
                mybatisMappers.add(customMybatisClass);
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Class " + customMybatisMapperClassName + " has not been found.", e);
            }
        }
        return mybatisMappers;
    }

    protected String defaultText(String deploymentName, String defaultName) {
        if (StringUtils.hasText(deploymentName)) {
            return deploymentName;
        }
        return defaultName;
    }

}
