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
package org.flowable.spring.boot.aot.dmn;

import java.util.List;

import org.flowable.spring.boot.aot.BaseAutoDeployResourceContribution;
import org.flowable.spring.boot.dmn.FlowableDmnProperties;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;

/**
 * @author Josh Long
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class FlowableDmnAutoDeployBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

    protected final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    @Override
    public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
        if (!ClassUtils.isPresent("org.flowable.dmn.spring.SpringDmnEngineConfiguration", beanFactory.getBeanClassLoader())) {
            return null;
        }

        if (!beanFactory.containsBean("dmnEngineConfiguration")) {
            return null;
        }

        FlowableDmnProperties properties = beanFactory.getBeanProvider(FlowableDmnProperties.class)
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
            return new BaseAutoDeployResourceContribution(locationPrefix, locationSuffixes);
        }

        return null;
    }

}
