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
package org.flowable.eventregistry.impl;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.flowable.eventregistry.api.CorrelationKeyGenerator;

/**
 * Default implementation of the {@link CorrelationKeyGenerator} interface.
 * This implementation provides a single hash value based on the data passed in.
 * All values in the data are used to generate the key.
 * <p>
 * This implementation is inspired by the Spring Batch DefaultJobKeyGenerator
 *
 * @author Filip Hrisafov
 */
public class DefaultCorrelationKeyGenerator implements CorrelationKeyGenerator<Map<String, Object>> {

    @Override
    public String generateKey(Map<String, Object> source) {
        StringBuilder sb = new StringBuilder();
        List<String> keys = new ArrayList<>(source.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            Object sourceValue = source.get(key);
            String value = sourceValue == null ? "" : sourceValue.toString();
            sb.append(key).append("=").append(value).append(";");
        }

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available. Fatal (should be in the JDK).");
        }

        byte[] bytes = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
        return String.format("%x", new BigInteger(1, bytes));
    }
}
