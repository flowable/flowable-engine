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
package org.flowable.http.common.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;

/**
 * @author Filip Hrisafov
 */
public class HttpHeaders implements Map<String, List<String>> {

    protected final Map<String, List<String>> headers;
    protected final String rawStringHeaders;


    public HttpHeaders() {
        this(null);
    }

    protected HttpHeaders(String rawStringHeaders) {
        this.headers = new LinkedHashMap<>();
        this.rawStringHeaders = rawStringHeaders;
    }

    @Override
    public int size() {
        return headers.size();
    }

    @Override
    public boolean isEmpty() {
        return headers.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return headers.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return headers.containsValue(value);
    }

    @Override
    public List<String> get(Object key) {
        return headers.get(key);
    }

    @Override
    public List<String> put(String key, List<String> value) {
        return headers.put(key, value);
    }

    @Override
    public List<String> remove(Object key) {
        return headers.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends List<String>> m) {
        headers.putAll(m);
    }

    @Override
    public void clear() {
        headers.clear();
    }

    @Override
    public Set<String> keySet() {
        return headers.keySet();
    }

    @Override
    public Collection<List<String>> values() {
        return headers.values();
    }

    @Override
    public Set<Entry<String, List<String>>> entrySet() {
        return headers.entrySet();
    }

    public void add(String headerName, String headerValue) {
        headers.computeIfAbsent(headerName, key -> new ArrayList<>()).add(headerValue);
    }

    public String formatAsString() {
        if (rawStringHeaders != null) {
            return rawStringHeaders;
        }

        StringBuilder sb = new StringBuilder();
        for (Entry<String, List<String>> entry : headers.entrySet()) {
            String headerName = entry.getKey();
            for (String headerValue : entry.getValue()) {
                sb.append(headerName);
                if (headerValue != null) {
                    sb.append(": ").append(headerValue);
                } else {
                    sb.append(":");
                }
                sb.append('\n');
            }
        }

        if (sb.length() > 0) {
            // Delete the last new line (\n)
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString();
    }

    public static HttpHeaders parseFromString(String headersString) {
        HttpHeaders headers = new HttpHeaders(headersString);

        if (StringUtils.isNotEmpty(headersString)) {
            try (BufferedReader reader = new BufferedReader(new StringReader(headersString))) {
                String line = reader.readLine();
                while (line != null) {
                    int colonIndex = line.indexOf(':');
                    if (colonIndex > 0) {
                        String headerName = line.substring(0, colonIndex);
                        if (line.length() > colonIndex + 2) {
                            headers.add(headerName, StringUtils.strip(line.substring(colonIndex + 1)));
                        } else {
                            headers.add(headerName, "");
                        }
                        line = reader.readLine();

                    } else {
                        throw new FlowableIllegalArgumentException("Header line '" + line + "' is invalid");
                    }
                }
            } catch (IOException ex) {
                throw new FlowableException("IO exception occurred", ex);
            }
        }

        return headers;
    }

}
