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
package org.flowable.bpm.model.xml.impl.util;

import org.flowable.bpm.model.xml.ModelException;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URISyntaxException;
import java.net.URL;

public abstract class ReflectUtil {

    public static InputStream getResourceAsStream(String name) {
        // Try the current Thread context class loader
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream resourceStream = classLoader.getResourceAsStream(name);
        if (resourceStream == null) {
            // Finally, try the class loader for this class
            classLoader = ReflectUtil.class.getClassLoader();
            resourceStream = classLoader.getResourceAsStream(name);
        }
        return resourceStream;
    }

    public static URL getResource(String name) {
        return getResource(name, null);
    }

    public static URL getResource(String name, ClassLoader classLoader) {
        if (classLoader == null) {
            // Try the current Thread context class loader
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        URL url = classLoader.getResource(name);
        if (url == null) {
            // Finally, try the class loader for this class
            classLoader = ReflectUtil.class.getClassLoader();
            url = classLoader.getResource(name);
        }
        return url;
    }

    public static File getResourceAsFile(String path) {
        URL resource = getResource(path);
        try {
            return new File(resource.toURI());
        }
        catch (URISyntaxException e) {
            throw new ModelException("Exception while loading resource file " + path, e);
        }
    }

    /**
     * Create a new instance of the provided type
     *
     * @param type the class to create a new instance of
     * @param parameters the parameters to pass to the constructor
     * @return the created instance
     */
    public static <T> T createInstance(Class<T> type, Object... parameters) {

        // get types for parameters
        Class<?>[] parameterTypes = new Class<?>[parameters.length];
        for (int idx = 0; idx < parameters.length; idx++) {
            Object parameter = parameters[idx];
            parameterTypes[idx] = parameter.getClass();
        }

        try {
            // create instance
            Constructor<T> constructor = type.getConstructor(parameterTypes);
            return constructor.newInstance(parameters);

        }
        catch (Exception e) {
            throw new ModelException("Exception while creating an instance of type " + type, e);
        }
    }

}
