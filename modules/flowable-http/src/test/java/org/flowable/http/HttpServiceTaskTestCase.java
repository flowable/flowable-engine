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
package org.flowable.http;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.impl.util.ReflectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Http Server and API to test HTTP Activity
 *
 * @author Harsha Teja Kanna
 */
public abstract class HttpServiceTaskTestCase extends PluggableFlowableTestCase {

    private static Logger log = LoggerFactory.getLogger(HttpServiceTaskTestCase.class);
    // These should be fixed and known as we use it in test process templates
    protected static final int HTTP_PORT = 9798;
    protected static final int HTTPS_PORT = 9799;

    protected static Server server;

    static {
        server = new Server();

        // http connector configuration
        HttpConfiguration httpConfig = new HttpConfiguration();

        ServerConnector httpConnector = new ServerConnector(server,
                new HttpConnectionFactory(httpConfig));
        httpConnector.setPort(HTTP_PORT);

        // https connector configuration
        // keytool -selfcert -alias Flowable -keystore keystore -genkey -keyalg RSA -sigalg SHA256withRSA -validity 36500
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath(ReflectUtil.getResource("flowable.keystore").getFile());
        sslContextFactory.setKeyStorePassword("Flowable");

        HttpConfiguration httpsConfig = new HttpConfiguration();

        ServerConnector httpsConnector = new ServerConnector(server,
                new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                new HttpConnectionFactory(httpsConfig));
        httpsConnector.setPort(HTTPS_PORT);

        server.setConnectors(new Connector[]{httpConnector, httpsConnector});

        try {
            ServletContextHandler contextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
            contextHandler.setContextPath("/");
            contextHandler.addServlet(new ServletHolder(new HttpServiceTaskTestServlet()), "/api/*");
            contextHandler.addServlet(new ServletHolder(new HttpServiceTaskAsyncTestServlet()), "/api/async/*");
            contextHandler.addServlet(new ServletHolder(new HttpServiceTaskExampleTestServlet()), "/api/example/*");
            server.setHandler(contextHandler);
            server.start();
        } catch (Exception e) {
            log.error("Error starting server", e);
        }

        // Shutdown hook to close the http server
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (server != null && server.isRunning()) {
                    try {
                        server.stop();
                        log.info("HTTP server stopped");
                    } catch (Exception e) {
                        log.error("Could not close http server", e);
                    }
                }
            }
        });
    }

    private static class HttpServiceTaskTestServlet extends HttpServlet {

        private String name = "test servlet";

        public HttpServiceTaskTestServlet() {
        }

        public HttpServiceTaskTestServlet(String name) {
            this.name = name;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("text/html");
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("<h1>" + name + "</h1>");
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("application/json");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println("{\"name\": \"" + name + "\"");
        }

        @Override
        protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("application/json");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println("{\"name\": \"" + name + "\"");
        }

        @Override
        protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("application/json");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println("{\"status\": \"success\"");
        }
    }

    private static class HttpServiceTaskAsyncTestServlet extends HttpServlet {

        private String name = "async test servlet";

        public HttpServiceTaskAsyncTestServlet() {
        }

        public HttpServiceTaskAsyncTestServlet(String name) {
            this.name = name;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("text/html");
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("<h1>" + name + "</h1>");
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("application/json");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println("{\"name\": \"" + name + "\"");
        }

        @Override
        protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("application/json");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println("{\"name\": \"" + name + "\"");
        }

        @Override
        protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("application/json");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println("{\"status\": \"success\"");
        }
    }

    private static class HttpServiceTaskExampleTestServlet extends HttpServlet {

        private String name = "example test servlet";

        public HttpServiceTaskExampleTestServlet() {
        }

        public HttpServiceTaskExampleTestServlet(String name) {
            this.name = name;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("text/html");
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().println("<h1>" + name + "</h1>");
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("application/json");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println("{\"name\": \"" + name + "\"");
        }

        @Override
        protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("application/json");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println("{\"name\": \"" + name + "\"");
        }

        @Override
        protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setContentType("application/json");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println("{\"status\": \"success\"");
        }
    }
}
