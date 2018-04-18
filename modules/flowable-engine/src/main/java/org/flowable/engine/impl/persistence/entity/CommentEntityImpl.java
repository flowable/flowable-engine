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

package org.flowable.engine.impl.persistence.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.flowable.common.engine.impl.persistence.entity.AbstractEntityNoRevision;

/**
 * @author Tom Baeyens
 */
public class CommentEntityImpl extends AbstractEntityNoRevision implements CommentEntity, Serializable {

    private static final long serialVersionUID = 1L;

    // If comments would be removable, revision needs to be added!

    protected String type;
    protected String userId;
    protected Date time;
    protected String taskId;
    protected String processInstanceId;
    protected String action;
    protected String message;
    protected String fullMessage;

    public CommentEntityImpl() {

    }

    @Override
    public Object getPersistentState() {
        return CommentEntityImpl.class;
    }

    @Override
    public byte[] getFullMessageBytes() {
        return (fullMessage != null ? fullMessage.getBytes() : null);
    }

    @Override
    public void setFullMessageBytes(byte[] fullMessageBytes) {
        fullMessage = (fullMessageBytes != null ? new String(fullMessageBytes) : null);
    }

    public static String MESSAGE_PARTS_MARKER = "_|_";
    public static Pattern MESSAGE_PARTS_MARKER_REGEX = Pattern.compile("_\\|_");

    @Override
    public void setMessage(String[] messageParts) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String part : messageParts) {
            if (part != null) {
                stringBuilder.append(part.replace(MESSAGE_PARTS_MARKER, " | "));
                stringBuilder.append(MESSAGE_PARTS_MARKER);
            } else {
                stringBuilder.append("null");
                stringBuilder.append(MESSAGE_PARTS_MARKER);
            }
        }
        for (int i = 0; i < MESSAGE_PARTS_MARKER.length(); i++) {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }
        message = stringBuilder.toString();
    }

    @Override
    public List<String> getMessageParts() {
        if (message == null) {
            return null;
        }
        List<String> messageParts = new ArrayList<>();

        String[] parts = MESSAGE_PARTS_MARKER_REGEX.split(message);
        for (String part : parts) {
            if ("null".equals(part)) {
                messageParts.add(null);
            } else {
                messageParts.add(part);
            }
        }
        return messageParts;
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public String getTaskId() {
        return taskId;
    }

    @Override
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public Date getTime() {
        return time;
    }

    @Override
    public void setTime(Date time) {
        this.time = time;
    }

    @Override
    public String getProcessInstanceId() {
        return processInstanceId;
    }

    @Override
    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getFullMessage() {
        return fullMessage;
    }

    @Override
    public void setFullMessage(String fullMessage) {
        this.fullMessage = fullMessage;
    }

    @Override
    public String getAction() {
        return action;
    }

    @Override
    public void setAction(String action) {
        this.action = action;
    }
}
