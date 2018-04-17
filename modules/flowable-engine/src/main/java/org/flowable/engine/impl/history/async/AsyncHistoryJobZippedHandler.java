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
package org.flowable.engine.impl.history.async;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import org.flowable.common.engine.impl.util.IoUtil;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;

/**
 * @author Joram Barrez
 */
public class AsyncHistoryJobZippedHandler extends AsyncHistoryJobHandler {

    public static final String JOB_TYPE = "async-history-zipped";

    @Override
    public String getType() {
        return JOB_TYPE;
    }
    
    @Override
    protected byte[] getJobBytes(HistoryJobEntity job) {
        byte[] bytes = job.getAdvancedJobHandlerConfigurationByteArrayRef().getBytes();
        bytes = decompress(bytes);
        return bytes;
    }

    protected byte[] decompress(final byte[] compressed) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressed)) {
            try (GZIPInputStream gis = new GZIPInputStream(bais)) {
                return IoUtil.readInputStream(gis, "async-history-configuration");
            }
        } catch (IOException e) {
            throw new RuntimeException("Error while decompressing json bytes", e);
        }
    }

}
