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
package org.flowable.bpm.model.xml.impl.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.flowable.bpm.model.xml.instance.DomDocument;

public final class IoUtil {

    private IoUtil() {}

    public static void closeSilently(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        }
        catch (Exception e) {
            // ignored
        }
    }

    /**
     * Convert an {@link InputStream} to a {@link String}
     *
     * @param inputStream the {@link InputStream} to convert
     * @return the resulting {@link String}
     * @throws IOException
     */
    public static String getStringFromInputStream(InputStream inputStream)
        throws IOException {
        return getStringFromInputStream(inputStream, true);
    }

    /**
     * Convert an {@link InputStream} to a {@link String}
     *
     * @param inputStream the {@link InputStream} to convert
     * @param trim trigger if whitespaces are trimmed in the output
     * @return the resulting {@link String}
     * @throws IOException
     */
    private static String getStringFromInputStream(InputStream inputStream, boolean trim)
        throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (trim) {
                    stringBuilder.append(line.trim());
                } else {
                    stringBuilder.append(line).append(System.lineSeparator());
                }
            }
        }
        return stringBuilder.toString();
    }

    /**
     * Converts a {@link OutputStream} to an {@link InputStream} by coping the data directly. WARNING: Do not use for large data (>100MB). Only for
     * testing purpose.
     *
     * @param outputStream the {@link OutputStream} to convert
     * @return the resulting {@link InputStream}
     */
    public static InputStream convertOutputStreamToInputStream(OutputStream outputStream) {
        byte[] data = ((ByteArrayOutputStream) outputStream).toByteArray();
        return new ByteArrayInputStream(data);
    }

    /**
     * Converts a {@link DomDocument} to its String representation
     *
     * @param document the XML document to convert
     */
    public static String convertXmlDocumentToString(DomDocument document) {
        StringWriter stringWriter = new StringWriter();
        StreamResult result = new StreamResult(stringWriter);
        transformDocumentToXml(document, result);
        return stringWriter.toString();
    }

    /**
     * Writes a {@link DomDocument} to an {@link OutputStream} by transforming the DOM to XML.
     *
     * @param document the DOM document to write
     * @param outputStream the {@link OutputStream} to write to
     */
    public static void writeDocumentToOutputStream(DomDocument document, OutputStream outputStream) {
        StreamResult result = new StreamResult(outputStream);
        transformDocumentToXml(document, result);
    }

    /**
     * Transforms a {@link DomDocument} to XML output.
     *
     * @param document the DOM document to transform
     * @param result the {@link StreamResult} to write to
     */
    public static void transformDocumentToXml(DomDocument document, StreamResult result) {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            synchronized (document) {
                transformer.transform(document.getDomSource(), result);
            }
        }
        catch (TransformerConfigurationException e) {
            throw new ModelIoException("Unable to create a transformer for the model", e);
        }
        catch (TransformerException e) {
            throw new ModelIoException("Unable to transform model to xml", e);
        }
    }

}
