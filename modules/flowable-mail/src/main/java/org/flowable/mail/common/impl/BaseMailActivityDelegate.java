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
package org.flowable.mail.common.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.api.variable.VariableContainer;
import org.flowable.content.api.ContentItem;
import org.flowable.content.api.ContentService;
import org.flowable.mail.common.api.MailMessage;
import org.flowable.mail.common.api.MailResponse;
import org.flowable.mail.common.api.SendMailRequest;
import org.flowable.mail.common.api.client.ExecutableSendMailRequest;
import org.flowable.mail.common.api.client.FlowableMailClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * @param <V> The type of the variable container
 * @author Filip Hrisafov
 */
public abstract class BaseMailActivityDelegate<V extends VariableContainer> {

    private static final String NEWLINE_REGEX = "\\r?\\n";
    protected final Logger logger = LoggerFactory.getLogger(getClass());

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

    protected void prepareAndExecuteRequest(V variableContainer) {
        try {
            ExecutableSendMailRequest executableSendMailRequest = prepareRequest(variableContainer);
            executableSendMailRequest.send();
        } catch (FlowableMailException ex) {
            handleException(variableContainer, "Cannot send e-mail for " + variableContainer, ex);
        } catch (FlowableException ex) {
            handleException(variableContainer, ex.getMessage(), ex);
        }
    }

    protected ExecutableSendMailRequest prepareRequest(V variableContainer) {
        SendMailRequest request = createMailRequest(variableContainer);
        return getMailClient(variableContainer).prepareRequest(request);
    }

    protected SendMailRequest createMailRequest(V variableContainer) {
        return new SendMailRequest(createMessage(variableContainer));
    }

    protected abstract FlowableMailClient getMailClient(V variableContainer);

    protected abstract Expression createExpression(String expressionText);

    protected abstract ContentService getContentService();

    protected MailMessage createMessage(V variableContainer) {

        String headersStr = getStringFromField(headers, variableContainer);
        Collection<String> toList = parseRecipients(to, variableContainer);
        String fromStr = getStringFromField(from, variableContainer);
        Collection<String> ccList = parseRecipients(cc, variableContainer);
        Collection<String> bccList = parseRecipients(bcc, variableContainer);
        String subjectStr = getStringFromField(subject, variableContainer);
        String textStr = textVar == null ?
                getStringFromField(text, variableContainer) :
                getStringFromField(getExpression(variableContainer, textVar), variableContainer);
        String htmlStr = htmlVar == null ?
                getStringFromField(html, variableContainer) :
                getStringFromField(getExpression(variableContainer, htmlVar), variableContainer);
        String charSetStr = getStringFromField(charset, variableContainer);

        if (toList.isEmpty() && ccList.isEmpty() && bccList.isEmpty()) {
            throw new FlowableException("No recipient could be found for sending email for " + variableContainer);
        }

        if (htmlStr == null && textStr == null) {
            throw new FlowableIllegalArgumentException("'html' or 'text' is required to be defined when using the mail activity");
        }

        MailMessage message = new MailMessage();

        addHeader(message, headersStr);
        message.setTo(toList);
        message.setFrom(fromStr);
        message.setCc(ccList);
        message.setBcc(bccList);
        message.setSubject(subjectStr);
        message.setPlainContent(textStr);
        message.setHtmlContent(htmlStr);
        if (charSetStr != null) {
            message.setCharset(Charset.forName(charSetStr));
        }
        addAttachments(message, variableContainer);

        return message;
    }

    protected void addHeader(MailMessage message, String headersStr) {
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
            message.addHeader(name, value);
        }
    }

    protected void addAttachments(MailMessage message, V variableContainer) {
        if (attachments == null) {
            return;
        }

        Object value = attachments.getValue(variableContainer);
        if (value == null) {
            return;
        }
        if (value instanceof Collection<?> collection) {
            if (!collection.isEmpty()) {
                for (Object object : collection) {
                    addExpressionValueAttachment(message, object, variableContainer);
                }
            }
        } else {
            addExpressionValueAttachment(message, value, variableContainer);
        }

    }

    protected void addExpressionValueAttachment(MailMessage message, Object value, V variableContainer) {
        if (value instanceof File file) {
            if (fileExists(file)) {
                message.addAttachment(new FileDataSource(file));
            }

        } else if (value instanceof String filename) {
            File file = new File(filename);
            if (fileExists(file)) {
                message.addAttachment(new FileDataSource(file));
            }

        } else if (value instanceof File[] files) {
            for (File file : files) {
                addExpressionValueAttachment(message, file, variableContainer);
            }

        } else if (value instanceof String[] filenames) {
            for (String filename : filenames) {
                addExpressionValueAttachment(message, filename, variableContainer);
            }

        } else if (value instanceof DataSource dataSource) {
            message.addAttachment(dataSource);

        } else if (value instanceof DataSource[] dataSources) {
            for (DataSource dataSource : dataSources) {
                addExpressionValueAttachment(message, dataSource, variableContainer);
            }

        } else if (value instanceof ContentItem contentItem) {
            message.addAttachment(new ContentItemDataSourceWrapper(contentItem, getContentService()));

        } else if (value instanceof ContentItem[] contentItems) {
            for (ContentItem contentItem : contentItems) {
                addExpressionValueAttachment(message, contentItem, variableContainer);
            }

        } else {
            throw new FlowableException("Invalid attachment type: " + value.getClass() + " for " + variableContainer);
        }

    }

    protected String getStringFromField(Expression expression, V variableContainer) {
        if (expression != null) {
            Object value = expression.getValue(variableContainer);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    protected Collection<String> parseRecipients(Expression expression, V variableContainer) {
        if (expression == null) {
            return Collections.emptyList();
        }
        Object value = expression.getValue(variableContainer);
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof Collection) {
            return (Collection<String>) value;
        } else if (value instanceof ArrayNode arrayNode) {
            Collection<String> recipients = new ArrayList<>(arrayNode.size());
            for (JsonNode node : arrayNode) {
                recipients.add(node.asText());
            }
            return recipients;
        } else {
            String str = value.toString();
            if (StringUtils.isNotEmpty(str)) {
                return Arrays.asList(value.toString().split("[\\s]*,[\\s]*"));
            }
        }
        return Collections.emptyList();
    }

    protected boolean fileExists(File file) {
        return file != null && file.exists() && file.isFile() && file.canRead();
    }

    protected Expression getExpression(V variableContainer, Expression var) {
        String variable = (String) variableContainer.getVariable(var.getExpressionText());
        return createExpression(variable);
    }

    protected void handleException(V variableContainer, String msg, FlowableException e) {
        boolean doIgnoreException = Boolean.parseBoolean(getStringFromField(ignoreException, variableContainer));
        if (doIgnoreException) {
            logger.info("Ignoring email send error: {}", msg, e);
            String exceptionVariable = getStringFromField(exceptionVariableName, variableContainer);
            if (exceptionVariable != null && exceptionVariable.length() > 0) {
                variableContainer.setVariable(exceptionVariable, msg);
            }
        } else {
            if (e instanceof FlowableMailException) {
                throw new FlowableException(msg, e);
            } else {
                throw e;
            }
        }
    }

    public static class ContentItemDataSourceWrapper implements DataSource {

        protected final ContentItem contentItem;
        protected final ContentService contentService;

        public ContentItemDataSourceWrapper(ContentItem contentItem, ContentService contentService) {
            this.contentItem = contentItem;
            this.contentService = contentService;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return contentService.getContentItemData(contentItem.getId());
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

    public record ExecutionData(MailResponse response, Throwable exception) {

    }
}
