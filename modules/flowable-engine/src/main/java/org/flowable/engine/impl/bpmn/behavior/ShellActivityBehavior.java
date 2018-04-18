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
package org.flowable.engine.impl.bpmn.behavior;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.DelegateExecution;

public class ShellActivityBehavior extends AbstractBpmnActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected Expression command;
    protected Expression wait;
    protected Expression arg1;
    protected Expression arg2;
    protected Expression arg3;
    protected Expression arg4;
    protected Expression arg5;
    protected Expression outputVariable;
    protected Expression errorCodeVariable;
    protected Expression redirectError;
    protected Expression cleanEnv;
    protected Expression directory;

    String commandStr;
    String arg1Str;
    String arg2Str;
    String arg3Str;
    String arg4Str;
    String arg5Str;
    String waitStr;
    String resultVariableStr;
    String errorCodeVariableStr;
    Boolean waitFlag;
    Boolean redirectErrorFlag;
    Boolean cleanEnvBoolean;
    String directoryStr;

    private void readFields(DelegateExecution execution) {
        commandStr = getStringFromField(command, execution);
        arg1Str = getStringFromField(arg1, execution);
        arg2Str = getStringFromField(arg2, execution);
        arg3Str = getStringFromField(arg3, execution);
        arg4Str = getStringFromField(arg4, execution);
        arg5Str = getStringFromField(arg5, execution);
        waitStr = getStringFromField(wait, execution);
        resultVariableStr = getStringFromField(outputVariable, execution);
        errorCodeVariableStr = getStringFromField(errorCodeVariable, execution);

        String redirectErrorStr = getStringFromField(redirectError, execution);
        String cleanEnvStr = getStringFromField(cleanEnv, execution);

        waitFlag = waitStr == null || waitStr.equalsIgnoreCase("true");
        redirectErrorFlag = "true".equalsIgnoreCase(redirectErrorStr);
        cleanEnvBoolean = "true".equalsIgnoreCase(cleanEnvStr);
        directoryStr = getStringFromField(directory, execution);

    }

    @Override
    public void execute(DelegateExecution execution) {

        readFields(execution);

        List<String> argList = new ArrayList<>();
        argList.add(commandStr);

        if (arg1Str != null)
            argList.add(arg1Str);
        if (arg2Str != null)
            argList.add(arg2Str);
        if (arg3Str != null)
            argList.add(arg3Str);
        if (arg4Str != null)
            argList.add(arg4Str);
        if (arg5Str != null)
            argList.add(arg5Str);

        ProcessBuilder processBuilder = new ProcessBuilder(argList);

        try {
            processBuilder.redirectErrorStream(redirectErrorFlag);
            if (cleanEnvBoolean) {
                Map<String, String> env = processBuilder.environment();
                env.clear();
            }
            if (directoryStr != null && directoryStr.length() > 0)
                processBuilder.directory(new File(directoryStr));

            Process process = processBuilder.start();

            if (waitFlag) {
                int errorCode = process.waitFor();

                if (resultVariableStr != null) {
                    String result = convertStreamToStr(process.getInputStream());
                    execution.setVariable(resultVariableStr, result);
                }

                if (errorCodeVariableStr != null) {
                    execution.setVariable(errorCodeVariableStr, Integer.toString(errorCode));

                }

            }
        } catch (Exception e) {
            throw new FlowableException("Could not execute shell command ", e);
        }

        leave(execution);
    }

    public static String convertStreamToStr(InputStream is) throws IOException {

        if (is != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }
            return writer.toString();
        } else {
            return "";
        }
    }

    protected String getStringFromField(Expression expression, DelegateExecution execution) {
        if (expression != null) {
            Object value = expression.getValue(execution);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

}
