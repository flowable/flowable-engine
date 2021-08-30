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

package org.flowable.common.engine.impl.cfg.mail;

import java.nio.charset.Charset;

/**
 * @author Tom Baeyens
 */
public class MailServerInfo {

    protected String mailServerDefaultFrom;
    protected String mailServerForceTo;
    protected String mailServerHost;
    protected int mailServerPort = 25;
    protected int mailServerSSLPort = 465;
    protected String mailServerUsername;
    protected String mailServerPassword;
    protected boolean mailServerUseSSL;
    protected boolean mailServerUseTLS;
    protected Charset mailServerDefaultCharset;

    public String getMailServerDefaultFrom() {
        return mailServerDefaultFrom;
    }

    public void setMailServerDefaultFrom(String mailServerDefaultFrom) {
        this.mailServerDefaultFrom = mailServerDefaultFrom;
    }

    public String getMailServerForceTo() {
        return mailServerForceTo;
    }

    public void setMailServerForceTo(String mailServerForceTo) {
        this.mailServerForceTo = mailServerForceTo;
    }

    public String getMailServerHost() {
        return mailServerHost;
    }

    public void setMailServerHost(String mailServerHost) {
        this.mailServerHost = mailServerHost;
    }

    public int getMailServerPort() {
        return mailServerPort;
    }

    public void setMailServerPort(int mailServerPort) {
        this.mailServerPort = mailServerPort;
    }

    public int getMailServerSSLPort() {
        return mailServerSSLPort;
    }

    public void setMailServerSSLPort(int mailServerSSLPort) {
        this.mailServerSSLPort = mailServerSSLPort;
    }

    public String getMailServerUsername() {
        return mailServerUsername;
    }

    public void setMailServerUsername(String mailServerUsername) {
        this.mailServerUsername = mailServerUsername;
    }

    public String getMailServerPassword() {
        return mailServerPassword;
    }

    public void setMailServerPassword(String mailServerPassword) {
        this.mailServerPassword = mailServerPassword;
    }

    public boolean isMailServerUseSSL() {
        return mailServerUseSSL;
    }

    public void setMailServerUseSSL(boolean mailServerUseSSL) {
        this.mailServerUseSSL = mailServerUseSSL;
    }

    public boolean isMailServerUseTLS() {
        return mailServerUseTLS;
    }

    public void setMailServerUseTLS(boolean mailServerUseTLS) {
        this.mailServerUseTLS = mailServerUseTLS;
    }

    public Charset getMailServerDefaultCharset() {
        return mailServerDefaultCharset;
    }

    public void setMailServerDefaultCharset(Charset mailServerDefaultCharset) {
        this.mailServerDefaultCharset = mailServerDefaultCharset;
    }
}
