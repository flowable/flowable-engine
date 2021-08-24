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

package org.flowable.cmmn.engine.impl.behavior.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.activation.DataSource;
import javax.naming.NamingException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.commons.mail.SimpleEmail;
import org.flowable.cmmn.engine.CmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.behavior.CoreCmmnActivityBehavior;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntity;
import org.flowable.cmmn.engine.impl.util.CommandContextUtil;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.cfg.mail.MailServerInfo;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.content.api.ContentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Based on the MailActivityBehavior found in the bpmn engine, adapted for use in cmmn.
 *
 * @author Joram Barrez
 */
public class MailActivityBehavior extends CoreCmmnActivityBehavior {

    // TODO: this should be merged with the bpmn counterpart later

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(MailActivityBehavior.class);

    private static final String NEWLINE_REGEX = "\\r?\\n";

    protected Expression to;
    protected Expression from;
    protected Expression cc;
    protected Expression bcc;
    protected Expression headers;
    protected Expression subject;
    protected Expression text;
    protected Expression textVar;
    protected Expression html;
    protected Expression htmlVar;
    protected Expression charset;
    protected Expression ignoreException;
    protected Expression exceptionVariableName;
    protected Expression attachments;

    @Override
    public void execute(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity) {
        boolean doIgnoreException = Boolean.parseBoolean(getStringFromField(ignoreException, planItemInstanceEntity));
        String exceptionVariable = getStringFromField(exceptionVariableName, planItemInstanceEntity);
        Email email = null;
        try {
            String headersStr = getStringFromField(headers, planItemInstanceEntity);
            String toStr = getStringFromField(to, planItemInstanceEntity);
            String fromStr = getStringFromField(from, planItemInstanceEntity);
            String ccStr = getStringFromField(cc, planItemInstanceEntity);
            String bccStr = getStringFromField(bcc, planItemInstanceEntity);
            String subjectStr = getStringFromField(subject, planItemInstanceEntity);
            String textStr = textVar == null ? getStringFromField(text, planItemInstanceEntity)
                : getStringFromField(getExpression(commandContext, planItemInstanceEntity, textVar), planItemInstanceEntity);
            String htmlStr = htmlVar == null ? getStringFromField(html, planItemInstanceEntity)
                : getStringFromField(getExpression(commandContext, planItemInstanceEntity, htmlVar), planItemInstanceEntity);
            String charSetStr = getStringFromField(charset, planItemInstanceEntity);
            List<File> files = new LinkedList<>();
            List<DataSource> dataSources = new LinkedList<>();
            getFilesFromFields(attachments, planItemInstanceEntity, files, dataSources);

            if (StringUtils.isAllEmpty(toStr, ccStr, bccStr)) {
                throw new FlowableException("No recipient could be found for sending email");
            }

            email = createEmail(textStr, htmlStr, attachmentsExist(files, dataSources));
            addHeader(email, headersStr);
            addTo(commandContext, email, toStr, planItemInstanceEntity.getTenantId());
            setFrom(commandContext, email, fromStr, planItemInstanceEntity.getTenantId());
            addCc(commandContext, email, ccStr, planItemInstanceEntity.getTenantId());
            addBcc(commandContext, email, bccStr, planItemInstanceEntity.getTenantId());
            setSubject(email, subjectStr);
            setMailServerProperties(commandContext, email, planItemInstanceEntity.getTenantId());
            setCharset(email, charSetStr, planItemInstanceEntity.getTenantId());
            attach(email, files, dataSources);

            email.send();

        } catch (FlowableException e) {
            handleException(planItemInstanceEntity, e.getMessage(), e, doIgnoreException, exceptionVariable);
        } catch (EmailException e) {
            handleException(planItemInstanceEntity, "Could not send e-mail for plan item instance "
                + planItemInstanceEntity.getId(), e, doIgnoreException, exceptionVariable);
        }

        CommandContextUtil.getAgenda(commandContext).planCompletePlanItemInstanceOperation(planItemInstanceEntity);
    }

    protected void addHeader(Email email, String headersStr) {
        if (headersStr == null) {
            return;
        }
        for (String headerEntry : headersStr.split(NEWLINE_REGEX)) {
            String[] split = headerEntry.split(":");
            if (split.length != 2) {
                throw new FlowableIllegalArgumentException("When using email headers name and value must be defined colon separated. (e.g. X-Attribute: value");
            }
            String name = split[0].trim();
            String value = split[1].trim();
            email.addHeader(name, value);
        }
    }

    private boolean attachmentsExist(List<File> files, List<DataSource> dataSources) {
        return !((files == null || files.isEmpty()) && (dataSources == null || dataSources.isEmpty()));
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
            throw new FlowableException("Could not create HTML email", e);
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
            throw new FlowableException("Could not create text-only email", e);
        }
    }

    protected void addTo(CommandContext commandContext, Email email, String to, String tenantId) {
        if (to == null) {
            return;
        }
        String newTo = getForceTo(commandContext, tenantId);
        if (newTo == null) {
            newTo = to;
        }
        String[] tos = splitAndTrim(newTo);
        if (tos != null) {
            for (String t : tos) {
                try {
                    email.addTo(t);
                } catch (EmailException e) {
                    throw new FlowableException("Could not add " + t + " as recipient", e);
                }
            }
        } else {
            throw new FlowableException("No recipient could be found for sending email");
        }
    }

    protected void setFrom(CommandContext commandContext, Email email, String from, String tenantId) {
        String fromAddress = null;

        if (from != null) {
            fromAddress = from;
        } else { // use default configured from address in process engine config
            if (tenantId != null && tenantId.length() > 0) {
                Map<String, MailServerInfo> mailServers = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getMailServers();
                if (mailServers != null && mailServers.containsKey(tenantId)) {
                    MailServerInfo mailServerInfo = mailServers.get(tenantId);
                    fromAddress = mailServerInfo.getMailServerDefaultFrom();
                }
            }

            if (fromAddress == null) {
                fromAddress = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getMailServerDefaultFrom();
            }
        }

        try {
            email.setFrom(fromAddress);
        } catch (EmailException e) {
            throw new FlowableException("Could not set " + from + " as from address in email", e);
        }
    }

    protected void addCc(CommandContext commandContext, Email email, String cc, String tenantId) {
        if (cc == null) {
            return;
        }

        String newCc = getForceTo(commandContext, tenantId);
        if (newCc == null) {
            newCc = cc;
        }
        String[] ccs = splitAndTrim(newCc);
        if (ccs != null) {
            for (String c : ccs) {
                try {
                    email.addCc(c);
                } catch (EmailException e) {
                    throw new FlowableException("Could not add " + c + " as cc recipient", e);
                }
            }
        }
    }

    protected void addBcc(CommandContext commandContext, Email email, String bcc, String tenantId) {
        if (bcc == null) {
            return;
        }
        String newBcc = getForceTo(commandContext, tenantId);
        if (newBcc == null) {
            newBcc = bcc;
        }
        String[] bccs = splitAndTrim(newBcc);
        if (bccs != null) {
            for (String b : bccs) {
                try {
                    email.addBcc(b);
                } catch (EmailException e) {
                    throw new FlowableException("Could not add " + b + " as bcc recipient", e);
                }
            }
        }
    }

    protected void attach(Email email, List<File> files, List<DataSource> dataSources) throws EmailException {
        if (!(email instanceof MultiPartEmail && attachmentsExist(files, dataSources))) {
            return;
        }
        MultiPartEmail mpEmail = (MultiPartEmail) email;
        for (File file : files) {
            mpEmail.attach(file);
        }
        for (DataSource ds : dataSources) {
            if (ds != null) {
                mpEmail.attach(ds, ds.getName(), null);
            }
        }
    }

    protected void setSubject(Email email, String subject) {
        email.setSubject(subject != null ? subject : "");
    }

    protected void setMailServerProperties(CommandContext commandContext, Email email, String tenantId) {
        CmmnEngineConfiguration engineConfiguration = CommandContextUtil.getCmmnEngineConfiguration(commandContext);

        boolean isMailServerSet = false;
        if (tenantId != null && tenantId.length() > 0) {
            if (engineConfiguration.getMailSessionJndi(tenantId) != null) {
                setEmailSession(email, engineConfiguration.getMailSessionJndi(tenantId));
                isMailServerSet = true;

            } else if (engineConfiguration.getMailServer(tenantId) != null) {
                MailServerInfo mailServerInfo = engineConfiguration.getMailServer(tenantId);
                String host = mailServerInfo.getMailServerHost();
                if (host == null) {
                    throw new FlowableException("Could not send email: no SMTP host is configured for tenantId " + tenantId);
                }
                email.setHostName(host);

                email.setSmtpPort(mailServerInfo.getMailServerPort());
                email.setSslSmtpPort(Integer.toString(mailServerInfo.getMailServerSSLPort()));

                email.setSSLOnConnect(mailServerInfo.isMailServerUseSSL());
                email.setStartTLSEnabled(mailServerInfo.isMailServerUseTLS());

                String user = mailServerInfo.getMailServerUsername();
                String password = mailServerInfo.getMailServerPassword();
                if (user != null && password != null) {
                    email.setAuthentication(user, password);
                }

                isMailServerSet = true;
            }
        }

        if (!isMailServerSet) {
            String mailSessionJndi = engineConfiguration.getMailSessionJndi();
            if (mailSessionJndi != null) {
                setEmailSession(email, mailSessionJndi);

            } else {
                String host = engineConfiguration.getMailServerHost();
                if (host == null) {
                    throw new FlowableException("Could not send email: no SMTP host is configured");
                }
                email.setHostName(host);

                int port = engineConfiguration.getMailServerPort();
                email.setSmtpPort(port);
                email.setSslSmtpPort(Integer.toString(engineConfiguration.getMailServerSSLPort()));

                email.setSSLOnConnect(engineConfiguration.getMailServerUseSSL());
                email.setStartTLSEnabled(engineConfiguration.getMailServerUseTLS());

                String user = engineConfiguration.getMailServerUsername();
                String password = engineConfiguration.getMailServerPassword();
                if (user != null && password != null) {
                    email.setAuthentication(user, password);
                }
            }
        }
    }

    protected void setEmailSession(Email email, String mailSessionJndi) {
        try {
            email.setMailSessionFromJNDI(mailSessionJndi);
        } catch (NamingException e) {
            throw new FlowableException("Could not send email: Incorrect JNDI configuration", e);
        }
    }

    protected void setCharset(Email email, String charSetStr, String tenantId) {
        if (charset != null) {
            email.setCharset(charSetStr);
        } else {
            Charset mailServerDefaultCharset = getDefaultCharset(tenantId);
            if (mailServerDefaultCharset != null) {
                email.setCharset(mailServerDefaultCharset.name());
            }
        }
    }

    protected String[] splitAndTrim(String str) {
        if (str != null) {
            String[] splittedStrings = str.split(",");
            for (int i = 0; i < splittedStrings.length; i++) {
                splittedStrings[i] = splittedStrings[i].trim();
            }
            return splittedStrings;
        }
        return null;
    }

    protected String getStringFromField(Expression expression, PlanItemInstanceEntity planItemInstanceEntity) {
        if (expression != null) {
            Object value = expression.getValue(planItemInstanceEntity);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    protected void getFilesFromFields(Expression expression, PlanItemInstanceEntity planItemInstanceEntity, List<File> files, List<DataSource> dataSources) {

        if (expression == null) {
            return;
        }

        Object value = expression.getValue(planItemInstanceEntity);
        if (value != null) {

            if (value instanceof Collection) {
                Collection collection = (Collection) value;
                if (!collection.isEmpty()) {
                    for (Object object : collection) {
                        addExpressionValueToAttachments(object, files, dataSources);
                    }
                }

            } else {
                addExpressionValueToAttachments(value, files, dataSources);

            }

            files.removeIf(file -> !fileExists(file));
        }
    }

    protected void addExpressionValueToAttachments(Object value, List<File> files, List<DataSource> dataSources) {
        if (value instanceof File) {
            files.add((File) value);

        } else if (value instanceof String) {
            files.add(new File((String) value));

        } else if (value instanceof File[]) {
            Collections.addAll(files, (File[]) value);

        } else if (value instanceof String[]) {
            String[] paths = (String[]) value;
            for (String path : paths) {
                files.add(new File(path));
            }

        } else if (value instanceof DataSource) {
            dataSources.add((DataSource) value);

        } else if (value instanceof DataSource[]) {
            for (DataSource ds : (DataSource[]) value) {
                if (ds != null) {
                    dataSources.add(ds);
                }
            }

        } else if (value instanceof ContentItem) {
            dataSources.add(new ContentItemDataSourceWrapper((ContentItem) value));

        } else if (value instanceof ContentItem[]) {
            for (ContentItem contentItem : (ContentItem[]) value) {
                dataSources.add(new ContentItemDataSourceWrapper(contentItem));
            }

        } else {
            throw new FlowableException("Invalid attachment type: " + value.getClass());

        }
    }

    protected boolean fileExists(File file) {
        return file != null && file.exists() && file.isFile() && file.canRead();
    }

    protected Expression getExpression(CommandContext commandContext, PlanItemInstanceEntity planItemInstanceEntity, Expression var) {
        String variable = (String) planItemInstanceEntity.getVariable(var.getExpressionText());
        return CommandContextUtil.getCmmnEngineConfiguration(commandContext).getExpressionManager().createExpression(variable);
    }

    protected void handleException(PlanItemInstanceEntity planItemInstanceEntity, String msg, Exception e, boolean doIgnoreException, String exceptionVariable) {
        if (doIgnoreException) {
            LOGGER.info("Ignoring email send error: {}", msg, e);
            if (exceptionVariable != null && exceptionVariable.length() > 0) {
                planItemInstanceEntity.setVariable(exceptionVariable, msg);
            }
        } else {
            if (e instanceof FlowableException) {
                throw (FlowableException) e;
            } else {
                throw new FlowableException(msg, e);
            }
        }
    }

    protected String getForceTo(CommandContext commandContext, String tenantId) {
        String forceTo = null;
        if (tenantId != null && tenantId.length() > 0) {
            Map<String, MailServerInfo> mailServers = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getMailServers();
            if (mailServers != null && mailServers.containsKey(tenantId)) {
                MailServerInfo mailServerInfo = mailServers.get(tenantId);
                forceTo = mailServerInfo.getMailServerForceTo();
            }
        }

        if (forceTo == null) {
            forceTo = CommandContextUtil.getCmmnEngineConfiguration(commandContext).getMailServerForceTo();
        }

        return forceTo;
    }

    protected Charset getDefaultCharset(String tenantId) {
        Charset defaultCharset = null;
        if (StringUtils.isNotBlank(tenantId)) {
            MailServerInfo mailServerInfo = CommandContextUtil.getCmmnEngineConfiguration().getMailServer(tenantId);
            if (mailServerInfo != null) {
                defaultCharset = mailServerInfo.getMailServerDefaultCharset();
            }

        }

        if (defaultCharset == null) {
            defaultCharset = CommandContextUtil.getCmmnEngineConfiguration().getMailServerDefaultCharset();
        }

        return defaultCharset;
    }

    public static class ContentItemDataSourceWrapper implements DataSource {

        protected ContentItem contentItem;

        public ContentItemDataSourceWrapper(ContentItem contentItem) {
            this.contentItem = contentItem;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return CommandContextUtil.getContentService().getContentItemData(contentItem.getId());
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            // Not needed for mail attachment
            return null;
        }

        @Override
        public String getContentType() {
            return contentItem.getMimeType();
        }

        @Override
        public String getName() {
            return contentItem.getName();
        }

    }

}
