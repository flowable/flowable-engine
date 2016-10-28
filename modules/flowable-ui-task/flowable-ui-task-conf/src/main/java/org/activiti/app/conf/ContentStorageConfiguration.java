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
package org.activiti.app.conf;

import java.io.File;

import org.activiti.content.storage.api.ContentStorage;
import org.activiti.content.storage.fs.SimpleFileSystemContentStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class ContentStorageConfiguration {

    private final Logger log = LoggerFactory.getLogger(ContentStorageConfiguration.class);
    
    private static final String PROP_FS_ROOT = "contentstorage.fs.rootFolder";
    private static final String PROP_FS_CREATE_ROOT = "contentstorage.fs.createRoot";
    
    @Autowired
    private Environment env;
    
    @Bean
    public ContentStorage contentStorage() {
        String fsRoot = env.getProperty(PROP_FS_ROOT);
        log.info("Using file-system based content storage (" + fsRoot + ")");

        File root = new File(fsRoot);
        if(env.getProperty(PROP_FS_CREATE_ROOT, Boolean.class, Boolean.FALSE).booleanValue() && !root.exists()) {
           log.info("Creating content storage root and possible missing parents: " + root.getAbsolutePath());
           root.mkdirs();
        }
        if (root != null && root.exists()) {
           log.info("File system root : " + root.getAbsolutePath());
        }
        return new SimpleFileSystemContentStorage(root);
    }
}
