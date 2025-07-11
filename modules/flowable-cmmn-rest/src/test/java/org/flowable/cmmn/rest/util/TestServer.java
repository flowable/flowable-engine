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
package org.flowable.cmmn.rest.util;

import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;
import org.flowable.cmmn.rest.WebConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.web.context.WebApplicationContext;

/**
 * @author Filip Hrisafov
 */
public class TestServer implements SmartLifecycle {

    private final static Logger LOGGER = LoggerFactory.getLogger(TestServer.class);

    private final Object lifeCycleMonitor = new Object();
    protected boolean running;

    protected final Server server;

    public TestServer(WebApplicationContext applicationContext) {
        this.server = new Server(0); // Use 0 to let the server choose a free port
        this.server.setHandler(getServletContextHandler(applicationContext));
    }

    private static ServletContextHandler getServletContextHandler(WebApplicationContext context) {
        ServletContextHandler contextHandler = new ServletContextHandler();
        WebConfigurer configurer = new WebConfigurer(context);
        contextHandler.addEventListener(configurer);
        return contextHandler;
    }

    @Override
    public void start() {
        synchronized (lifeCycleMonitor) {
            if (!isRunning()) {
                try {
                    server.start();
                    running = true;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to start server", e);
                }
            }
        }

    }

    @Override
    public void stop() {
        synchronized (lifeCycleMonitor) {
            if (isRunning()) {
                try {
                    server.stop();
                    running = false;
                } catch (Exception e) {
                    LOGGER.error("Failed to stop server", e);
                }
            }
        }

    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public String getServerUrlPrefix() {
        return "http://localhost:" + server.getURI().getPort() + "/service/";
    }
}
