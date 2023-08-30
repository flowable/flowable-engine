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
package org.flowable.mail.common.impl.jakarta.mail;

import java.io.UnsupportedEncodingException;
import java.net.IDN;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.mail.Authenticator;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeUtility;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.mail.common.api.MailMessage;
import org.flowable.mail.common.api.MailResponse;
import org.flowable.mail.common.api.SendMailRequest;
import org.flowable.mail.common.api.client.ExecutableSendMailRequest;
import org.flowable.mail.common.api.client.FlowableMailClient;
import org.flowable.mail.common.impl.FlowableMailException;
import org.flowable.mail.common.impl.MailDefaultsConfiguration;
import org.flowable.mail.common.impl.MailHostServerConfiguration;
import org.flowable.mail.common.impl.MailJndiServerConfiguration;
import org.flowable.mail.common.impl.MailServerConfiguration;
import org.flowable.mail.common.impl.SimpleMailResponse;

/**
 * @author Filip Hrisafov
 */
public class JakartaMailFlowableMailClient implements FlowableMailClient {
    // The creation of the Jakarta MimeMessage is inspired by Apache Commons Email

    private static final String MAIL_TRANSPORT_PROTOCOL = "mail.transport.protocol";
    private static final String MAIL_TRANSPORT_STARTTLS_ENABLE = "mail.smtp.starttls.enable";
    private static final String MAIL_TRANSPORT_STARTTLS_REQUIRED = "mail.smtp.starttls.false";
    private static final String MAIL_HOST = "mail.smtp.host";
    private static final String MAIL_PORT = "mail.smtp.port";

    private static final String MAIL_SMTP_AUTH = "mail.smtp.auth";
    private static final String MAIL_SMTP_TIMEOUT = "mail.smtp.timeout";
    private static final String MAIL_SMTP_CONNECTIONTIMEOUT = "mail.smtp.connectiontimeout";
    private static final String MAIL_SMTP_SEND_PARTIAL = "mail.smtp.sendpartial";
    private static final String MAIL_SMTPS_SEND_PARTIAL = "mail.smtps.sendpartial";

    private static final String MAIL_SMTP_SOCKET_FACTORY_PORT = "mail.smtp.socketFactory.port";
    private static final String MAIL_SMTP_SOCKET_FACTORY_CLASS = "mail.smtp.socketFactory.class";
    private static final String MAIL_SMTP_SOCKET_FACTORY_FALLBACK = "mail.smtp.socketFactory.fallback";

    private static final String CONTENT_TYPE_TEXT_HTML = "text/html";

    private static final Duration SOCKET_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration SOCKET_CONNECTION_TIMEOUT = Duration.ofSeconds(60);

    protected final MailServerConfiguration serverConfiguration;
    protected final MailDefaultsConfiguration defaultsConfiguration;

    public JakartaMailFlowableMailClient(MailServerConfiguration serverConfiguration, MailDefaultsConfiguration defaultsConfiguration) {
        this.serverConfiguration = serverConfiguration;
        this.defaultsConfiguration = defaultsConfiguration;
    }

    @Override
    public ExecutableSendMailRequest prepareRequest(SendMailRequest request) {
        Session session = createSession();
        try {
            MimeMessage mimeMessage = createMimeMessage(request, session);
            return new JakartaMailSendMailRequest(mimeMessage);
        } catch (MessagingException e) {
            throw new FlowableMailException("Failed to create mime message", e);
        }
    }

    protected MimeMessage createMimeMessage(SendMailRequest request, Session session) throws MessagingException {
        MimeMessage mimeMessage = new MimeMessage(session);

        MailMessage message = request.message();
        Charset charset = getCharset(message);
        setContent(mimeMessage, message, charset != null ? charset.name() : null);
        setSubject(mimeMessage, message.getSubject(), charset);
        addHeaders(mimeMessage, message.getHeaders(), charset);
        addTo(mimeMessage, message.getTo());
        addCc(mimeMessage, message.getCc());
        addBcc(mimeMessage, message.getBcc());

        setFrom(mimeMessage, message.getFrom());
        setSentDate(mimeMessage);

        return mimeMessage;
    }

    protected Charset getCharset(MailMessage message) {
        Charset charset = message.getCharset();
        if (charset == null) {
            charset = defaultsConfiguration.defaultCharset();
        }

        return charset;
    }

    protected void setSubject(MimeMessage message, String subject, Charset charset) throws MessagingException {
        if (StringUtils.isNotEmpty(subject)) {
            if (charset != null) {
                message.setSubject(subject, charset.name());
            } else {
                message.setSubject(subject);
            }
        }
    }

    protected void addHeaders(MimeMessage message, Map<String, String> headers, Charset charset) throws MessagingException {
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {

                String name = entry.getKey();
                String value = entry.getValue();

                if (StringUtils.isEmpty(name)) {
                    throw new FlowableMailException("header name cannot be null or empty");
                }

                if (StringUtils.isEmpty(value)) {
                    throw new FlowableMailException("header value cannot be null or empty");
                }

                String foldedHeaderValue = createFoldedHeaderValue(name, value, charset);
                message.addHeader(name, foldedHeaderValue);
            }
        }

    }

    protected String createFoldedHeaderValue(String name, String value, Charset charset) {
        try {
            return MimeUtility.fold(name.length() + 2, MimeUtility.encodeText(value, charset != null ? charset.name() : null, null));
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    protected void addTo(MimeMessage message, Collection<String> to) {
        addRecipient(message, to, Message.RecipientType.TO);
    }

    protected void addCc(MimeMessage message, Collection<String> cc) {
        addRecipient(message, cc, Message.RecipientType.CC);
    }

    protected void addBcc(MimeMessage message, Collection<String> bcc) {
        addRecipient(message, bcc, Message.RecipientType.BCC);
    }

    protected void addRecipient(MimeMessage message, Collection<String> recipients, Message.RecipientType recipientType) {
        if (recipients == null || recipients.isEmpty()) {
            return;
        }
        Collection<String> newRecipients = recipients;
        Collection<String> forceRecipients = defaultsConfiguration.forceTo();
        if (forceRecipients != null && !forceRecipients.isEmpty()) {
            newRecipients = forceRecipients;
        }
        if (!newRecipients.isEmpty()) {
            for (String t : newRecipients) {
                try {
                    message.addRecipient(recipientType, createInternetAddress(t));
                } catch (MessagingException e) {
                    throw new FlowableMailException("Could not add " + t + " as " + recipientType + " recipient", e);
                }
            }
        }
    }

    protected InternetAddress createInternetAddress(String email) {
        try {
            InternetAddress address = new InternetAddress(toASCIIEmail(email));
            address.validate();
            return address;
        } catch (AddressException e) {
            throw new FlowableMailException("Invalid email", e);
        }
    }

    protected String toASCIIEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex < 0) {
            return email;
        }

        // localPart@domainPart
        return email.substring(0, atIndex) + '@' + IDN.toASCII(email.substring(atIndex + 1));

    }

    protected void setFrom(MimeMessage message, String from) {
        String fromAddress;

        if (from != null) {
            fromAddress = from;
        } else { // use default configured from address in defaults configuration
            fromAddress = defaultsConfiguration.defaultFrom();
        }

        try {
            message.setFrom(createInternetAddress(fromAddress));
        } catch (MessagingException e) {
            throw new FlowableMailException("Could not set " + fromAddress + " as from address in email", e);
        }
    }

    protected void setSentDate(MimeMessage message) {
        try {
            if (message.getSentDate() == null) {
                message.setSentDate(new Date());
            }
        } catch (MessagingException e) {
            throw new FlowableMailException("Failed to set send date", e);
        }
    }

    protected void setContent(MimeMessage mimeMessage, MailMessage message, String charset) {
        String text = message.getPlainContent();
        String html = message.getHtmlContent();
        Collection<DataSource> attachments = message.getAttachments();
        boolean attachmentsExists = attachments != null && !attachments.isEmpty();
        if (html == null && text == null) {
            throw new FlowableIllegalArgumentException("'html' or 'text' is required to be defined when sending an email");
        }
        if (html == null && !attachmentsExists) {
            try {
                mimeMessage.setText(text, charset);
            } catch (MessagingException e) {
                throw new FlowableMailException("Could not create text-only email", e);
            }
        } else {
            try {
                mimeMessage.setContent(createMultiPartContent(text, html, charset, attachments));
            } catch (MessagingException e) {
                throw new FlowableMailException("Failed to create multi part email", e);
            }
        }
    }

    protected MimeMultipart createMultiPartContent(String text, String html, String charset, Collection<DataSource> attachments) throws MessagingException {
        boolean attachmentsExists = attachments != null && !attachments.isEmpty();

        MimeMultipart rootContainer = new MimeMultipart();
        MimeMultipart bodyContainer = rootContainer;

        rootContainer.setSubType("mixed");

        if (StringUtils.isNotEmpty(text) && StringUtils.isNotEmpty(html)) {
            if (attachmentsExists) {
                // If both HTML and TEXT bodies are provided, create an alternative
                // container and add it to the root container
                bodyContainer = new MimeMultipart("alternative");
                MimeBodyPart bodyPart = new MimeBodyPart();
                try {
                    bodyPart.setContent(bodyContainer);
                    rootContainer.addBodyPart(bodyPart);
                } catch (MessagingException ex) {
                    throw new FlowableMailException("Failed to add body part", ex);
                }
            } else {
                // no attachments present, change the mimetype
                // of the root container (= body container)
                rootContainer.setSubType("alternative");
            }
        }

        if (StringUtils.isNotEmpty(html)) {
            MimeBodyPart msgHtml = new MimeBodyPart();
            bodyContainer.addBodyPart(msgHtml, 0);
            msgHtml.setText(html, charset, "html");

            String contentType = msgHtml.getContentType();
            if (contentType == null || !contentType.equals(CONTENT_TYPE_TEXT_HTML)) {
                if (StringUtils.isNotEmpty(charset)) {
                    msgHtml.setContent(html, CONTENT_TYPE_TEXT_HTML + "; charset=" + charset);
                } else {
                    msgHtml.setContent(html, CONTENT_TYPE_TEXT_HTML);
                }
            }
        }

        if (StringUtils.isNotEmpty(text)) {
            MimeBodyPart msgText = new MimeBodyPart();
            bodyContainer.addBodyPart(msgText, 0);
            msgText.setText(text, charset);
        }

        if (attachmentsExists) {
            for (DataSource attachment : attachments) {
                BodyPart bodyPart = new MimeBodyPart();
                bodyPart.setDisposition(Part.ATTACHMENT);
                try {
                    bodyPart.setFileName(MimeUtility.encodeText(attachment.getName(), charset, null));
                } catch (UnsupportedEncodingException e) {
                    throw new FlowableMailException("Could not encode attachment file name", e);
                }
                bodyPart.setDataHandler(new DataHandler(attachment));
                rootContainer.addBodyPart(bodyPart);
            }
        }

        return rootContainer;
    }

    protected Session createSession() {
        if (serverConfiguration instanceof MailJndiServerConfiguration jndiServerConfiguration) {
            return createSession(jndiServerConfiguration);
        } else if (serverConfiguration instanceof MailHostServerConfiguration hostServerConfiguration) {
            return createSession(hostServerConfiguration);
        } else {
            throw new FlowableException("Unsupported server configuration " + serverConfiguration);
        }
    }

    protected Session createSession(MailJndiServerConfiguration serverConfiguration) {
        String sessionJndi = serverConfiguration.getSessionJndi();
        if (sessionJndi == null) {
            throw new FlowableIllegalArgumentException("sessionJndi has to be set for " + serverConfiguration);
        }
        try {
            Context ctx;
            if (sessionJndi.startsWith("java:")) {
                ctx = new InitialContext();
            } else {
                ctx = (Context) new InitialContext().lookup("java:comp/env");

            }

            return (Session) ctx.lookup(sessionJndi);
        } catch (NamingException e) {
            throw new FlowableException("Could not send email: Incorrect JNDI configuration", e);
        }
    }

    protected Session createSession(MailHostServerConfiguration serverConfiguration) {
        String host = serverConfiguration.host();
        if (host == null) {
            throw new FlowableException("Could not send email: no SMTP host is configured");
        }

        Properties properties = new Properties(System.getProperties());
        properties.setProperty(MAIL_TRANSPORT_PROTOCOL, "smtp");
        properties.setProperty(MAIL_PORT, String.valueOf(serverConfiguration.port()));
        properties.setProperty(MAIL_HOST, host);

        MailHostServerConfiguration.Transport transport = serverConfiguration.transport();
        properties.setProperty(MAIL_TRANSPORT_STARTTLS_ENABLE, transport == MailHostServerConfiguration.Transport.SMTPS_TLS ? "true" : "false");
        properties.setProperty(MAIL_TRANSPORT_STARTTLS_REQUIRED, "false");

        properties.setProperty(MAIL_SMTP_SEND_PARTIAL, "false");
        properties.setProperty(MAIL_SMTPS_SEND_PARTIAL, "false");

        Authenticator authenticator = getAuthenticator(serverConfiguration);
        if (authenticator != null) {
            properties.setProperty(MAIL_SMTP_AUTH, "true");
        }

        if (transport == MailHostServerConfiguration.Transport.SMTPS || transport == MailHostServerConfiguration.Transport.SMTPS_TLS) {
            properties.setProperty(MAIL_SMTP_SOCKET_FACTORY_PORT, String.valueOf(serverConfiguration.port()));
            properties.setProperty(MAIL_SMTP_SOCKET_FACTORY_CLASS, "javax.net.ssl.SSLSocketFactory");
            properties.setProperty(MAIL_SMTP_SOCKET_FACTORY_FALLBACK, "false");
        }

        properties.setProperty(MAIL_SMTP_TIMEOUT, Long.toString(SOCKET_TIMEOUT.toMillis()));
        properties.setProperty(MAIL_SMTP_CONNECTIONTIMEOUT, Long.toString(SOCKET_CONNECTION_TIMEOUT.toMillis()));

        customizeProperties(properties, authenticator);

        return Session.getInstance(properties, authenticator);

    }

    protected void customizeProperties(Properties properties, Authenticator authenticator) {
        // Nothing to do
    }

    protected Authenticator getAuthenticator(MailHostServerConfiguration serverConfiguration) {
        String user = serverConfiguration.user();
        String password = serverConfiguration.password();
        if (user != null && password != null) {
            PasswordAuthentication passwordAuthentication = new PasswordAuthentication(user, password);
            return new Authenticator() {

                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return passwordAuthentication;
                }
            };
        }

        return null;
    }

    protected static class JakartaMailSendMailRequest implements ExecutableSendMailRequest {

        protected final MimeMessage message;

        protected JakartaMailSendMailRequest(MimeMessage message) {
            this.message = message;
        }

        @Override
        public MailResponse send() {
            try {
                Transport.send(message);
                return new SimpleMailResponse(message.getMessageID());
            } catch (MessagingException e) {
                Session session = message.getSession();
                String host = session.getProperty("mail.smtp.host");
                String port = session.getProperty("mail.smtp.port");
                throw new FlowableMailException("Sending the email to the following server failed : " + host + ":" + port, e);
            }
        }
    }
}
