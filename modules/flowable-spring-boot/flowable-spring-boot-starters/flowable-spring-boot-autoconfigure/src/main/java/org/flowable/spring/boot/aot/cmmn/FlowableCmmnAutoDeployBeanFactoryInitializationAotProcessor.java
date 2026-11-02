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
package org.flowable.spring.boot.aot.cmmn;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.converter.CmmnXmlConverter;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.cmmn.model.ImplementationType;
import org.flowable.cmmn.model.ServiceTask;
import org.flowable.spring.boot.aot.BaseAutoDeployResourceContribution;
import org.flowable.spring.boot.cmmn.FlowableCmmnProperties;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;

/**
 * @author Josh Long
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class FlowableCmmnAutoDeployBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

    protected final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @Override
    public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
        if (!ClassUtils.isPresent("org.flowable.cmmn.spring.SpringCmmnEngineConfiguration", beanFactory.getBeanClassLoader())) {
            return null;
        }

        if (!beanFactory.containsBean("cmmnEngineConfiguration")) {
            return null;
        }

        FlowableCmmnProperties properties = beanFactory.getBeanProvider(FlowableCmmnProperties.class)
                .getIfAvailable();
        if (properties == null || !properties.isDeployResources()) {
            return null;
        }
        List<String> locationSuffixes = properties.getResourceSuffixes();
        if (locationSuffixes.isEmpty()) {
            return null;
        }
        String locationPrefix = properties.getResourceLocation();
        if (locationPrefix.startsWith(ResourceUtils.CLASSPATH_URL_PREFIX) || locationPrefix.startsWith(ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX)) {
            return new CmmnAutoDeployResourceContribution(locationPrefix, locationSuffixes, beanFactory);
        }

        return null;
    }

    static class CmmnAutoDeployResourceContribution extends BaseAutoDeployResourceContribution {

        protected final ConfigurableListableBeanFactory beanFactory;

        public CmmnAutoDeployResourceContribution(String locationPrefix, Collection<String> locationSuffixes, ConfigurableListableBeanFactory beanFactory) {
            super(locationPrefix, locationSuffixes);
            this.beanFactory = beanFactory;
        }

        @Override
        protected void applyToResource(ClassPathResource resource, RuntimeHints hints) {
            super.applyToResource(resource, hints);
            CmmnXmlConverter cmmXmlConverter = new CmmnXmlConverter();
            CmmnModel cmmnModel = cmmXmlConverter.convertToCmmnModel(() -> {
                try {
                    return resource.getInputStream();
                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to read resource " + resource, e);
                }
            }, false, false);
            Collection<ServiceTask> serviceTasks = cmmnModel.getPrimaryCase().findPlanItemDefinitionsOfType(ServiceTask.class);
            for (ServiceTask serviceTask : serviceTasks) {
                if (ImplementationType.IMPLEMENTATION_TYPE_DELEGATEEXPRESSION.equals(serviceTask.getImplementationType())) {
                    String expression = serviceTask.getImplementation();
                    String expressionWithoutDelimiters = expression.substring(2);
                    expressionWithoutDelimiters = expressionWithoutDelimiters.substring(0, expressionWithoutDelimiters.length() - 1);
                    String beanName = expressionWithoutDelimiters;
                    try {
                        BeanDefinition beanDefinition = beanFactory.getBeanDefinition(beanName);
                        String beanClassName = beanDefinition.getBeanClassName();
                        if (StringUtils.isNotEmpty(beanClassName)) {
                            hints.reflection().registerType(TypeReference.of(beanClassName), MemberCategory.values());
                            logger.debug("Registering hint for bean name [{}] for service task {} in {}", beanName, serviceTask.getId(), resource);
                        } else {
                            logger.debug("No bean class name for bean name [{}] for service task {} in {}", beanName, serviceTask.getId(), resource);
                        }

                    } catch (Throwable throwable) {
                        logger.error("Couldn't find bean named [{}] for service task {} in {}", beanName, serviceTask.getId(), resource);
                    }

                }
            }

        }
    }
}
