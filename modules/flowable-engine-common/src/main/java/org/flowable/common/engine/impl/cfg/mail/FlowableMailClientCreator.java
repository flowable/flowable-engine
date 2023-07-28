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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

import org.flowable.mail.common.api.client.FlowableMailClient;
import org.flowable.mail.common.impl.BaseMailHostServerConfiguration;
import org.flowable.mail.common.impl.MailDefaultsConfiguration;
import org.flowable.mail.common.impl.MailDefaultsConfigurationImpl;
import org.flowable.mail.common.impl.MailHostServerConfiguration;
import org.flowable.mail.common.impl.MailJndiServerConfiguration;
import org.flowable.mail.common.impl.MailServerConfiguration;
import org.flowable.mail.common.impl.apache.commons.ApacheCommonsEmailFlowableMailClient;

/**
 * @author Filip Hrisafov
 */
public class FlowableMailClientCreator {

    public static FlowableMailClient createSessionClient(String sessionJndi, MailServerInfo serverInfo) {
        return createSessionClient(sessionJndi, serverInfo, serverInfo);
    }

    public static FlowableMailClient createSessionClient(String sessionJndi, MailServerInfo serverInfo, MailServerInfo fallbackServerInfo) {
        MailServerConfiguration serverConfiguration = MailJndiServerConfiguration.of(sessionJndi);
        return createMailClient(serverConfiguration, serverInfo, fallbackServerInfo);
    }

    public static FlowableMailClient createHostClient(String host, MailServerInfo serverInfo) {
        return createHostClient(host, serverInfo, serverInfo);
    }

    public static FlowableMailClient createHostClient(String host, MailServerInfo serverInfo, MailServerInfo fallbackServerInfo) {
        MailServerConfiguration serverConfiguration = createMailHostServerConfiguration(host, serverInfo);
        return createMailClient(serverConfiguration, serverInfo, fallbackServerInfo);
    }

    protected static FlowableMailClient createMailClient(MailServerConfiguration serverConfiguration, MailServerInfo serverInfo,
            MailServerInfo fallbackServerInfo) {
        MailDefaultsConfiguration defaultsConfiguration = createMailDefaultsConfiguration(serverInfo, fallbackServerInfo);
        return new ApacheCommonsEmailFlowableMailClient(serverConfiguration, defaultsConfiguration);
    }

    protected static MailHostServerConfiguration createMailHostServerConfiguration(String host, MailServerInfo mailServer) {
        BaseMailHostServerConfiguration serverConfiguration = new BaseMailHostServerConfiguration();
        serverConfiguration.setHost(host);

        if (mailServer.isMailServerUseSSL()) {
            serverConfiguration.setPort(mailServer.getMailServerSSLPort());
            if (mailServer.isMailServerUseSSL()) {
                serverConfiguration.setTransport(MailHostServerConfiguration.Transport.SMTPS_TLS);
            } else {
                serverConfiguration.setTransport(MailHostServerConfiguration.Transport.SMTPS);
            }
        } else {
            serverConfiguration.setPort(mailServer.getMailServerPort());
            serverConfiguration.setTransport(MailHostServerConfiguration.Transport.SMTP);
        }

        serverConfiguration.setUser(mailServer.getMailServerUsername());
        serverConfiguration.setPassword(mailServer.getMailServerPassword());
        return serverConfiguration;
    }

    protected static MailDefaultsConfiguration createMailDefaultsConfiguration(MailServerInfo serverInfo, MailServerInfo fallbackServerInfo) {
        String defaultFrom = getDefaultValue(serverInfo, fallbackServerInfo, MailServerInfo::getMailServerDefaultFrom);
        Charset defaultCharset = getDefaultValue(serverInfo, fallbackServerInfo, MailServerInfo::getMailServerDefaultCharset);
        Collection<String> forceTo = getForceTo(serverInfo, fallbackServerInfo);
        return new MailDefaultsConfigurationImpl(defaultFrom, defaultCharset, forceTo);
    }

    protected static <T> T getDefaultValue(MailServerInfo serverInfo, MailServerInfo fallbackServerInfo, Function<MailServerInfo, T> valueProvider) {
        T value = null;
        if (serverInfo != null) {
            value = valueProvider.apply(serverInfo);
        }

        if (value == null) {
            value = valueProvider.apply(fallbackServerInfo);
        }
        return value;
    }

    protected static Collection<String> getForceTo(MailServerInfo serverInfo, MailServerInfo fallbackServerInfo) {
        String forceTo = null;
        if (serverInfo != null) {
            forceTo = serverInfo.getMailServerForceTo();
        }

        if (forceTo == null) {
            forceTo = fallbackServerInfo.getMailServerForceTo();
        }

        if (forceTo == null) {
            return Collections.emptyList();
        }

        return Arrays.stream(forceTo.split(",")).map(String::trim).toList();
    }

}
