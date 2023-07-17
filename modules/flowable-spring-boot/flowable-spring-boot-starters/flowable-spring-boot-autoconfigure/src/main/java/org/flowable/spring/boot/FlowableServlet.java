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

/**
 * Configuration properties for the Flowable REST Servlets.
 *
 * @author Filip Hrisafov
 */
public class FlowableServlet {

    /**
     * The context path for the rest servlet.
     */
    private String path;

    /**
     * The name of the servlet.
     */
    private String name;

    /**
     * Load on startup of the dispatcher servlet
     */
    private int loadOnStartup = -1;

    public FlowableServlet(String path, String name) {
        this.path = path;
        this.name = name;
    }
    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLoadOnStartup() {
        return loadOnStartup;
    }

    public void setLoadOnStartup(int loadOnStartup) {
        this.loadOnStartup = loadOnStartup;
    }
}
