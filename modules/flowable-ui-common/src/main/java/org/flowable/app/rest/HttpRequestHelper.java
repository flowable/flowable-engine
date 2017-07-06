package org.flowable.app.rest;

import com.google.common.base.Function;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
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

    private HttpRequestHelper() {
    }

    public static <T> T executeHttpGet(String apiUrl, String basicAuthUser, String basicAuthPassword, Function<HttpResponse, T> handler) {
        HttpGet httpGet = new HttpGet(apiUrl);
        httpGet.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + new String(
                Base64.encodeBase64((basicAuthUser + ":" + basicAuthPassword).getBytes(Charset.forName("UTF-8")))));

        CloseableHttpClient client = getHttpClientBuilder().build();

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

    public static void executePostRequest(String url, String basicAuthUser, String basicAuthPassword, HttpEntity data, int expectedResponse) {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader(HttpHeaders.AUTHORIZATION, "Basic " + new String(
                Base64.encodeBase64((basicAuthUser + ":" + basicAuthPassword).getBytes(Charset.forName("UTF-8")))));

        httpPost.setEntity(data);

        CloseableHttpClient client = getHttpClientBuilder().build();

        try {
            HttpResponse response = client.execute(httpPost);
            if (!(response.getStatusLine().getStatusCode() == expectedResponse)) {
                log.error("Invalid request {} result code: {}", url, response.getStatusLine());
                throw new InternalServerErrorException("Invalid request "+ url +" result code: " + response.getStatusLine());
            }
        } catch (IOException ioe) {
            log.error("Error calling endpoint {}", url, ioe);
            throw new InternalServerErrorException("Error calling endpoint "+ url +" : " + ioe.getMessage());
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

    private static HttpClientBuilder getHttpClientBuilder() {
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        SSLConnectionSocketFactory sslsf;
        try {
            SSLContextBuilder builder = new SSLContextBuilder();
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            sslsf = new SSLConnectionSocketFactory(builder.build(), SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            clientBuilder.setSSLSocketFactory(sslsf);
        } catch (Exception e) {
            log.error("Could not configure SSL for http client", e);
            throw new InternalServerErrorException("Could not configure SSL for http client", e);
        }
        return clientBuilder;
    }

}
