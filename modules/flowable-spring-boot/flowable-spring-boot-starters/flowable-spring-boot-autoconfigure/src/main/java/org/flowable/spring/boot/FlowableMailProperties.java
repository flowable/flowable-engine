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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Flowable Mail Properties.
 *
 * @author Filip Hrisafov
 */
@ConfigurationProperties(prefix = "flowable.mail.server")
public class FlowableMailProperties {

    /**
     * The host of the mail server.
     */
    private String host = "localhost";

    /**
     * The port of the mail server.
     */
    private int port = 1025;

    /**
     * The username that needs to be used for the mail server authentication.
     * If empty no authentication would be used.
     */
    private String username;

    /**
     * The password for the mail server authentication.
     */
    private String password;

    /**
     * The default from address that needs to be used when sending emails.
     */
    private String defaultFrom = "flowable@localhost";

    /**
     * The force to address(es) that would be used when sending out emails.
     * IMPORTANT: If this is set then all emails will be send to defined address(es) instead of the address
     * configured in the MailActivity.
     */
    private String forceTo;

    /**
     * Sets whether SSL/TLS encryption should be enabled for the SMTP transport upon connection (SMTPS/POPS).
     */
    private boolean useSsl;

    /**
     * Set or disable the STARTTLS encryption.
     */
    private boolean useTls;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDefaultFrom() {
        return defaultFrom;
    }

    public void setDefaultFrom(String defaultFrom) {
        this.defaultFrom = defaultFrom;
    }

    public String getForceTo() {
        return forceTo;
    }

    public void setForceTo(String forceTo) {
        this.forceTo = forceTo;
    }

    public boolean isUseSsl() {
        return useSsl;
    }

    public void setUseSsl(boolean useSsl) {
        this.useSsl = useSsl;
    }

    public boolean isUseTls() {
        return useTls;
    }

    public void setUseTls(boolean useTls) {
        this.useTls = useTls;
    }
}
