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
package org.flowable.http.bpmn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
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
import org.flowable.engine.common.impl.util.ReflectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Http Server and API to test HTTP Activity
 *
 * @author Harsha Teja Kanna
 */
public class HttpServiceTaskTestServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpServiceTaskTestServer.class);
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
            contextHandler.addServlet(new ServletHolder(new SimpleHttpServiceTaskTestServlet()), "/test");
            contextHandler.addServlet(new ServletHolder(new HelloServlet()), "/hello");
            server.setHandler(contextHandler);
            server.start();
        } catch (Exception e) {
            LOGGER.error("Error starting server", e);
        }

        // Shutdown hook to close the http server
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (server != null && server.isRunning()) {
                    try {
                        server.stop();
                        LOGGER.info("HTTP server stopped");
                    } catch (Exception e) {
                        LOGGER.error("Could not close http server", e);
                    }
                }
            }
        });
    }

    public static class HttpServiceTaskTestServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;

        public static Map<String, String> headerMap = new HashMap<>();

        private String name = "test servlet";
        private ObjectMapper mapper = new ObjectMapper();

        public HttpServiceTaskTestServlet() {
        }

        public HttpServiceTaskTestServlet(String name) {
            this.name = name;
        }

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            HttpTestData data = parseTestData(req, resp);
            int code = data.getCode();
            if (code >= 200 && code < 300) {
                resp.setStatus(code);
                resp.setContentType("application/json");
                resp.getWriter().println(mapper.convertValue(data, JsonNode.class));

            } else if (code >= 300 && code < 400) {
                resp.sendRedirect("http://www.flowable.org");

            } else if (code >= 400 && code < 500) {
                resp.setStatus(code);

            } else if (code >= 500 && code < 600) {
                resp.sendError(code, "Server Error");
            }
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            headerMap.clear();
            Enumeration<String> headerNames = req.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String header = headerNames.nextElement();
                headerMap.put(header, req.getHeader(header));
            }

            HttpTestData data = parseTestData(req, resp);
            int code = data.getCode();
            if (code >= 200 && code < 300) {
                resp.setStatus(code);
                resp.setContentType("application/json");
                resp.getWriter().println(mapper.convertValue(data, JsonNode.class));

            } else if (code >= 300 && code < 400) {
                resp.sendRedirect("http://www.flowable.org");

            } else if (code >= 400 && code < 500) {
                resp.sendError(code, "Bad Request");

            } else if (code >= 500 && code < 600) {
                resp.sendError(code, "Server Error");
            }
        }

        @Override
        protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            doPost(req, resp);
        }

        @Override
        protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            doPost(req, resp);
        }

        // Parse test data query parameters, headers
        private HttpTestData parseTestData(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

            HttpTestData data = new HttpTestData();
            data.setCode(200);
            data.setOrigin(req.getRemoteAddr());
            data.setUrl(req.getRequestURL().toString());

            Enumeration<String> parameterNames = req.getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String paramName = parameterNames.nextElement();
                String[] paramValues = req.getParameterValues(paramName);
                switch (paramName) {
                    case "code": {
                        data.setCode(Integer.parseInt(paramValues[0]));
                        break;
                    }
                    case "delay": {
                        data.setDelay(Integer.parseInt(paramValues[0]));
                        break;
                    }
                }
                data.getArgs().put(paramName, paramValues);
            }
            Enumeration<String> headerNames = req.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                Enumeration<String> headerValues = req.getHeaders(headerName);
                List<String> headerList = new ArrayList<>();
                while (headerValues.hasMoreElements()) {
                    headerList.add(headerValues.nextElement());
                }
                data.getHeaders().put(headerName, headerList.toArray(new String[]{}));
            }

            data.setBody(IOUtils.toString(req.getReader()));

            if (data.getDelay() > 0) {
                try {
                    Thread.sleep(data.getDelay());
                } catch (InterruptedException e) {
                    //Ignore
                }
            }

            return data;
        }
    }

    private static class SimpleHttpServiceTaskTestServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;

        private ObjectMapper objectMapper = new ObjectMapper();

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setStatus(200);
            resp.setContentType("application/json");

            ObjectNode responseNode = objectMapper.createObjectNode();
            ObjectNode nameNode = responseNode.putObject("name");
            nameNode.put("firstName", "John");
            nameNode.put("lastName", "Doe");

            resp.getWriter().println(responseNode);
        }
    }
    
    private static class HelloServlet extends HttpServlet {

        private static final long serialVersionUID = 1L;

        private ObjectMapper objectMapper = new ObjectMapper();
        
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.setStatus(200);
            resp.setContentType("application/json");
            
            JsonNode body = objectMapper.readTree(req.getInputStream());
            String name = body.get("name").asText();

            ObjectNode responseNode = objectMapper.createObjectNode();
            responseNode.put("result", "Hello " + name);
            resp.getWriter().println(responseNode);
        }

    }

    public static void setUp() {
        // No setup required
    }
}
