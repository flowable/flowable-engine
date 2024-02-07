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
package org.flowable.spring.boot.environment;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.support.FilePatternResourceHintsRegistrar;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * @author Filip Hrisafov
 */
public class FlowableDefaultPropertiesLocationRuntimeHints implements RuntimeHintsRegistrar {
    // This is similar to what is being done in ConfigDataLocationRuntimeHints

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        // This will register all the flowable-default configuration properties
        FilePatternResourceHintsRegistrar.forClassPathLocations("/")
                .withFileExtensions(getExtensions(classLoader))
                .withFilePrefixes("flowable-default")
                .registerHints(hints.resources(), classLoader);

    }

    // The logic below is the same as for the ConfigDataLocationRuntimeHints
    /**
     * Get the application file extensions to consider. A valid extension starts with a
     * dot.
     * @param classLoader the classloader to use
     * @return the configuration file extensions
     */
    protected List<String> getExtensions(ClassLoader classLoader) {
        List<String> extensions = new ArrayList<>();
        List<PropertySourceLoader> propertySourceLoaders = getSpringFactoriesLoader(classLoader)
                .load(PropertySourceLoader.class);
        for (PropertySourceLoader propertySourceLoader : propertySourceLoaders) {
            for (String fileExtension : propertySourceLoader.getFileExtensions()) {
                String candidate = "." + fileExtension;
                if (!extensions.contains(candidate)) {
                    extensions.add(candidate);
                }
            }
        }
        return extensions;
    }

    protected SpringFactoriesLoader getSpringFactoriesLoader(ClassLoader classLoader) {
        return SpringFactoriesLoader.forDefaultResourceLocation(classLoader);
    }
}
