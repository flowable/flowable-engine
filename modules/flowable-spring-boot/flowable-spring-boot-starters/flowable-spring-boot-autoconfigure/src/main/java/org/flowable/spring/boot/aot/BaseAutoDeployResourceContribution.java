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
package org.flowable.spring.boot.aot;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * @author Josh Long
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public class BaseAutoDeployResourceContribution implements BeanFactoryInitializationAotContribution {

    protected final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final String locationPrefix;
    protected final Collection<String> locationSuffixes;

    public BaseAutoDeployResourceContribution(String locationPrefix, Collection<String> locationSuffixes) {
        this.locationPrefix = locationPrefix;
        this.locationSuffixes = locationSuffixes;
    }

    @Override
    public void applyTo(GenerationContext generationContext, BeanFactoryInitializationCode beanFactoryInitializationCode) {
        RuntimeHints runtimeHints = generationContext.getRuntimeHints();
        for (String locationSuffix : locationSuffixes) {
            String path = locationPrefix + locationSuffix;
            try {
                for (Resource resource : resolver.getResources(path)) {
                    ClassPathResource classPathResource = asClasspathResource(resource);
                    if (classPathResource != null && classPathResource.exists()) {
                        logger.debug("Registering hints for {}", classPathResource);
                        applyToResource(classPathResource, runtimeHints);
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to find resources for " + path, e);
            }

        }
    }

    protected void applyToResource(ClassPathResource resource, RuntimeHints hints) {
        hints.resources().registerResource(resource);
    }

    protected ClassPathResource asClasspathResource(Resource resource) {
        if (resource instanceof ClassPathResource) {
            return (ClassPathResource) resource;
        }
        try {
            logger.debug("Transforming {} to a classpath resource", resource);
            String marker = "jar!";
            String externalFormOfUrl = resource.getURL().toExternalForm();
            if (externalFormOfUrl.contains(marker)) {
                String rest = externalFormOfUrl.substring(externalFormOfUrl.lastIndexOf(marker) + marker.length());
                return new ClassPathResource(rest);
            } else {
                // ugh i think this only works for maven? what about gradle?
                var classesSubstring = "classes/";
                var locationOfClassesInUrl = externalFormOfUrl.indexOf(classesSubstring);
                if (locationOfClassesInUrl != -1) {
                    return new ClassPathResource(externalFormOfUrl.substring(locationOfClassesInUrl + classesSubstring.length()));
                }

            }

            logger.error("Could not resolve {} as a classpath resource", resource);
            return null;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
