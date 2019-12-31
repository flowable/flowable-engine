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
package org.flowable.eventregistry.converter.channel;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.flowable.eventregistry.json.converter.ChannelJsonConverter;
import org.flowable.eventregistry.model.ChannelModel;

/**
 * @author Filip Hrisafov
 */
public abstract class AbstractChannelConverterTest {

    protected ChannelJsonConverter channelConverter = new ChannelJsonConverter();

    protected ChannelModel readJson(String resource) {
        String modelJson = readJsonToString(resource);
        return channelConverter.convertToChannelModel(modelJson);
    }

    protected ChannelModel exportAndReadChannel(ChannelModel channelModel) {
        String modelJson = channelConverter.convertToJson(channelModel);
        return channelConverter.convertToChannelModel(modelJson);
    }

    /* Helper methods */
    protected String readJsonToString(String resource) {
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(resource)) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        } catch (IOException e) {
            fail("Could not read " + resource + " : " + e.getMessage());
            return null;
        }
    }

}
