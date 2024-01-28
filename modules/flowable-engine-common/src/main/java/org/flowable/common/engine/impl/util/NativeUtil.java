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
package org.flowable.common.engine.impl.util;

/**
 * A util for detecting a GraalVM native environment.
 * This is inspired by the Spring {@code NativeDetector}
 *
 * @author Filip Hrisafov
 */
public class NativeUtil {

    // See https://github.com/oracle/graal/blob/master/sdk/src/org.graalvm.nativeimage/src/org/graalvm/nativeimage/ImageInfo.java
    private static final String imageCode = System.getProperty("org.graalvm.nativeimage.imagecode");

    private static final boolean inNativeImage = (imageCode != null);

    /**
     * Returns {@code true} if running in a native image context (for example
     * {@code buildtime}, {@code runtime}, or {@code agent}) expressed by setting the
     * {@code org.graalvm.nativeimage.imagecode} system property to any value.
     */
    public static boolean inNativeImage() {
        return inNativeImage;
    }

}
