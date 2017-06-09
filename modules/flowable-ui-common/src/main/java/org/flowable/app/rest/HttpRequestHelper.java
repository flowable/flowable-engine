package org.flowable.app.rest;

import com.google.common.base.Function;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.flowable.app.service.exception.InternalServerErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * @author martin.grofcik
 */
public class HttpRequestHelper {
    private static final Logger log = LoggerFactory.getLogger(HttpRequestHelper.class);

    public static <T> T executeHttpGet(String eventLogApiUrl, String basicAuthUser, String basicAuthPassword, Function<HttpResponse, T> handler) {
        HttpGet httpGet = new HttpGet(eventLogApiUrl);
        httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + new String(
                Base64.encodeBase64((basicAuthUser + ":" + basicAuthPassword).getBytes(Charset.forName("UTF-8")))));

        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        SSLConnectionSocketFactory sslsf = null;
        try {
            SSLContextBuilder builder = new SSLContextBuilder();
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            sslsf = new SSLConnectionSocketFactory(builder.build(), SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            clientBuilder.setSSLSocketFactory(sslsf);
        } catch (Exception e) {
            log.error("Could not configure SSL for http client", e);
            throw new InternalServerErrorException("Could not configure SSL for http client", e);
        }

        CloseableHttpClient client = clientBuilder.build();

        try {
            HttpResponse response = client.execute(httpGet);
            return handler.apply(response);
        } catch (IOException ioe) {
            log.error("Error calling deploy endpoint", ioe);
            throw new InternalServerErrorException("Error calling deploy endpoint: " + ioe.getMessage());
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (IOException e) {
                    log.warn("Exception while closing http client", e);
                }
            }
        }
    }

}
