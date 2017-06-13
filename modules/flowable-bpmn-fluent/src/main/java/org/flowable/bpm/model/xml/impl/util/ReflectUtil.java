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

import java.io.InputStream;
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
}
