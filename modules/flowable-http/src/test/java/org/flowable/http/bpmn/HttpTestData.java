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

import java.util.SortedMap;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Http Test response
 *
 * @author Harsha Teja Kanna
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HttpTestData {
    private int code;
    private int delay;
    private String body;
    private String origin;
    private String url;
    private SortedMap<String, String[]> args = new TreeMap<>();
    private SortedMap<String, String[]> headers = new TreeMap<>();

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public SortedMap<String, String[]> getArgs() {
        return args;
    }

    public void setArgs(SortedMap<String, String[]> args) {
        this.args = args;
    }

    public SortedMap<String, String[]> getHeaders() {
        return headers;
    }

    public void setHeaders(SortedMap<String, String[]> headers) {
        this.headers = headers;
    }
}
