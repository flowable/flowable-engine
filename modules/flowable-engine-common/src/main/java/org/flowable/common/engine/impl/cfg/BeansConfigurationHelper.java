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

package org.flowable.common.engine.impl.cfg;

import java.io.InputStream;

import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

/**
 * @author Tom Baeyens
 */
public class BeansConfigurationHelper {

    public static AbstractEngineConfiguration parseEngineConfiguration(Resource springResource, String beanName) {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        XmlBeanDefinitionReader xmlBeanDefinitionReader = new XmlBeanDefinitionReader(beanFactory);
        xmlBeanDefinitionReader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
        xmlBeanDefinitionReader.loadBeanDefinitions(springResource);
        AbstractEngineConfiguration engineConfiguration = (AbstractEngineConfiguration) beanFactory.getBean(beanName);
        engineConfiguration.setBeans(new SpringBeanFactoryProxyMap(beanFactory));
        return engineConfiguration;
    }

    public static AbstractEngineConfiguration parseEngineConfigurationFromInputStream(InputStream inputStream, String beanName) {
        Resource springResource = new InputStreamResource(inputStream);
        return parseEngineConfiguration(springResource, beanName);
    }

    public static AbstractEngineConfiguration parseEngineConfigurationFromResource(String resource, String beanName) {
        Resource springResource = new ClassPathResource(resource);
        return parseEngineConfiguration(springResource, beanName);
    }

}
