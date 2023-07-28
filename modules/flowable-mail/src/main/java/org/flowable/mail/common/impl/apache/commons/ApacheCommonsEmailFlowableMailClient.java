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
package org.flowable.mail.common.impl.apache.commons;

import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Map;

import javax.activation.DataSource;
import javax.naming.NamingException;

import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.commons.mail.SimpleEmail;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.mail.common.impl.MailDefaultsConfiguration;
import org.flowable.mail.common.impl.MailHostServerConfiguration;
import org.flowable.mail.common.impl.MailJndiServerConfiguration;
import org.flowable.mail.common.api.MailMessage;
import org.flowable.mail.common.api.MailResponse;
import org.flowable.mail.common.impl.MailServerConfiguration;
import org.flowable.mail.common.api.SendMailRequest;
import org.flowable.mail.common.api.client.ExecutableSendMailRequest;
import org.flowable.mail.common.api.client.FlowableMailClient;
import org.flowable.mail.common.impl.FlowableMailException;
import org.flowable.mail.common.impl.SimpleMailResponse;

/**
 * @author Filip Hrisafov
 */
public class ApacheCommonsEmailFlowableMailClient implements FlowableMailClient {

    protected final MailServerConfiguration serverConfiguration;
    protected final MailDefaultsConfiguration defaultsConfiguration;

    public ApacheCommonsEmailFlowableMailClient(MailServerConfiguration serverConfiguration, MailDefaultsConfiguration defaultsConfiguration) {
        this.serverConfiguration = serverConfiguration;
        this.defaultsConfiguration = defaultsConfiguration;
    }

    @Override
    public ExecutableSendMailRequest prepareRequest(SendMailRequest request) {
        MailMessage message = request.message();
        String text = message.getPlainContent();
        String html = message.getHtmlContent();
        Collection<DataSource> attachments = message.getAttachments();

        boolean attachmentsExist = attachments != null && !attachments.isEmpty();
        Email email = createEmail(text, html, attachmentsExist);
        addHeaders(email, message.getHeaders());
        addTo(email, message.getTo());
        setFrom(email, message.getFrom());
        addCc(email, message.getCc());
        addBcc(email, message.getBcc());
        setSubject(email, message.getSubject());
        setCharset(email, message.getCharset());
        if (attachmentsExist) {
            attach(email, attachments);
        }
        setMailServerProperties(email);

        return new ApacheCommonsEmailMailRequest(email);
    }

    protected Email createEmail(String text, String html, boolean attachmentsExist) {
        if (html != null) {
            return createHtmlEmail(text, html);
        } else if (text != null) {
            if (!attachmentsExist) {
                return createTextOnlyEmail(text);
            } else {
                return createMultiPartEmail(text);
            }
        } else {
            throw new FlowableIllegalArgumentException("'html' or 'text' is required to be defined when using the mail activity");
        }
    }

    protected HtmlEmail createHtmlEmail(String text, String html) {
        HtmlEmail email = new HtmlEmail();
        try {
            email.setHtmlMsg(html);
            if (text != null) { // for email clients that don't support html
                email.setTextMsg(text);
            }
            return email;
        } catch (EmailException e) {
            throw new FlowableMailException("Could not create HTML email", e);
        }
    }

    protected SimpleEmail createTextOnlyEmail(String text) {
        SimpleEmail email = new SimpleEmail();
        try {
            email.setMsg(text);
            return email;
        } catch (EmailException e) {
            throw new FlowableException("Could not create text-only email", e);
        }
    }

    protected MultiPartEmail createMultiPartEmail(String text) {
        MultiPartEmail email = new MultiPartEmail();
        try {
            email.setMsg(text);
            return email;
        } catch (EmailException e) {
            throw new FlowableMailException("Could not create text-only email", e);
        }
    }

    protected void addHeaders(Email email, Map<String, String> headers) {
        if (headers != null && !headers.isEmpty()) {
            headers.forEach(email::addHeader);
        }
    }

    protected void addTo(Email email, Collection<String> to) {
        if (to == null || to.isEmpty()) {
            return;
        }
        Collection<String> newTo = to;
        Collection<String> forceTo = defaultsConfiguration.forceTo();
        if (forceTo != null && !forceTo.isEmpty()) {
            newTo = forceTo;
        }
        if (!newTo.isEmpty()) {
            for (String t : newTo) {
                try {
                    email.addTo(t);
                } catch (EmailException e) {
                    throw new FlowableMailException("Could not add " + t + " as recipient", e);
                }
            }
        } else {
            throw new FlowableException("No recipient could be found for sending email");
        }
    }

    protected void setFrom(Email email, String from) {
        String fromAddress;

        if (from != null) {
            fromAddress = from;
        } else { // use default configured from address in defaults configuration
            fromAddress = defaultsConfiguration.defaultFrom();
        }

        try {
            email.setFrom(fromAddress);
        } catch (EmailException e) {
            throw new FlowableMailException("Could not set " + fromAddress + " as from address in email", e);
        }
    }

    protected void addCc(Email email, Collection<String> cc) {
        if (cc == null || cc.isEmpty()) {
            return;
        }
        Collection<String> newCc = cc;

        Collection<String> forceTo = defaultsConfiguration.forceTo();
        if (forceTo != null && !forceTo.isEmpty()) {
            newCc = forceTo;
        }
        if (!newCc.isEmpty()) {
            for (String c : newCc) {
                try {
                    email.addCc(c);
                } catch (EmailException e) {
                    throw new FlowableMailException("Could not add " + c + " as cc recipient", e);
                }
            }
        }
    }

    protected void addBcc(Email email, Collection<String> bcc) {
        if (bcc == null || bcc.isEmpty()) {
            return;
        }
        Collection<String> newBcc = bcc;
        Collection<String> forceTo = defaultsConfiguration.forceTo();
        if (forceTo != null && !forceTo.isEmpty()) {
            newBcc = forceTo;
        }
        if (!newBcc.isEmpty()) {
            for (String b : newBcc) {
                try {
                    email.addBcc(b);
                } catch (EmailException e) {
                    throw new FlowableMailException("Could not add " + b + " as bcc recipient", e);
                }
            }
        }
    }

    protected void setSubject(Email email, String subject) {
        email.setSubject(subject != null ? subject : "");
    }

    protected void setCharset(Email email, Charset charset) {
        if (charset != null) {
            email.setCharset(charset.name());
        } else {
            Charset defaultCharset = defaultsConfiguration.defaultCharset();
            if (defaultCharset != null) {
                email.setCharset(defaultCharset.name());
            }
        }
    }

    protected void attach(Email email, Collection<DataSource> attachments) {
        if (email instanceof MultiPartEmail multiPartEmail) {
            for (DataSource attachment : attachments) {
                try {
                    multiPartEmail.attach(attachment, attachment.getName(), null);
                } catch (EmailException e) {
                    throw new FlowableMailException("Failed to attach " + attachment, e);
                }
            }

        }
    }

    protected void setMailServerProperties(Email email) {
        if (serverConfiguration instanceof MailJndiServerConfiguration jndiServerConfiguration) {
            setMailServerProperties(email, jndiServerConfiguration);
        } else if (serverConfiguration instanceof MailHostServerConfiguration hostServerConfiguration) {
            setMailServerProperties(email, hostServerConfiguration);
        } else {
            throw new FlowableException("Unsupported server configuration " + serverConfiguration);
        }
    }

    protected void setMailServerProperties(Email email, MailJndiServerConfiguration serverConfiguration) {
        String sessionJndi = serverConfiguration.getSessionJndi();
        if (sessionJndi == null) {
            throw new FlowableIllegalArgumentException("sessionJndi has to be set for " + serverConfiguration);
        }
        setEmailSession(email, sessionJndi);
    }

    protected void setMailServerProperties(Email email, MailHostServerConfiguration serverConfiguration) {
        String host = serverConfiguration.host();
        if (host == null) {
            throw new FlowableException("Could not send email: no SMTP host is configured");
        }

        email.setHostName(host);

        MailHostServerConfiguration.Transport transport = serverConfiguration.transport();
        switch (transport) {
            case SMTP -> email.setSmtpPort(serverConfiguration.port());
            case SMTPS -> {
                email.setSslSmtpPort(Integer.toString(serverConfiguration.port()));
                email.setSSLOnConnect(true);
            }
            case SMTPS_TLS -> {
                email.setSslSmtpPort(Integer.toString(serverConfiguration.port()));
                email.setSSLOnConnect(true);
                email.setSSLOnConnect(true);
            }
            default -> throw new FlowableIllegalArgumentException("Unknown transport " + transport);
        }

        String user = serverConfiguration.user();
        String password = serverConfiguration.password();
        if (user != null && password != null) {
            email.setAuthentication(user, password);
        }

    }

    protected void setEmailSession(Email email, String mailSessionJndi) {
        try {
            email.setMailSessionFromJNDI(mailSessionJndi);
        } catch (NamingException e) {
            throw new FlowableException("Could not send email: Incorrect JNDI configuration", e);
        }
    }

    protected static class ApacheCommonsEmailMailRequest implements ExecutableSendMailRequest {

        protected final Email email;

        protected ApacheCommonsEmailMailRequest(Email email) {
            this.email = email;
        }

        @Override
        public MailResponse send() {
            try {
                String messageId = email.send();
                return new SimpleMailResponse(messageId);
            } catch (EmailException e) {
                throw new FlowableMailException("Sending email failed", e);
            }
        }
    }
}
