/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package datart.data.provider;

import datart.core.data.provider.Dataframe;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.HttpMethod;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.Map;

@Slf4j
public class HttpDataFetcher {

    private static final CloseableHttpClient httpClient;

    private final HttpRequestParam param;

    static {
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(30, TimeUnit.SECONDS)
                .build();
        httpClient = HttpClients.custom()
                .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                        .setDefaultConnectionConfig(connectionConfig)
                        .build())
                .build();
    }

    public HttpDataFetcher(HttpRequestParam param) {
        this.param = param;
    }

    public Dataframe fetchAndParse() throws IOException, URISyntaxException {

        HttpUriRequestBase httpRequest = createHttpRequest(param);

        final HttpResponseParser parser;
        try {
            parser = param.getResponseParser().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return executeWithParser(httpRequest, new ResponseJsonParser());
        }
        return executeWithParser(httpRequest, parser);
    }

    private HttpUriRequestBase createHttpRequest(HttpRequestParam param) throws URISyntaxException {
        HttpEntity entity = createHttpEntity(param);
        URI uri = createUri(param);
        HttpUriRequestBase httpRequest;
        if (HttpMethod.POST.matches(param.getMethod().name())) {
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(entity);
            httpRequest = httpPost;
        } else if (HttpMethod.PUT.matches(param.getMethod().name())) {
            HttpPut httpPut = new HttpPut(uri);
            httpPut.setEntity(entity);
            httpRequest = httpPut;
        } else if (HttpMethod.DELETE.matches(param.getMethod().name())) {
            httpRequest = new HttpDelete(uri);
        } else {
            httpRequest = new HttpGet(uri);
        }
        RequestConfig config = RequestConfig.custom()
                .setResponseTimeout(Timeout.ofMilliseconds(param.getTimeout()))
                .build();

        httpRequest.setConfig(config);

        withHeaders(param, httpRequest);

        if (StringUtils.isNotBlank(param.getUsername()) && StringUtils.isNotBlank(param.getPassword())) {
            String auth = param.getUsername() + ":" + param.getPassword();
            httpRequest.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + Base64.encodeBase64String(auth.getBytes(StandardCharsets.UTF_8)));
        }

        return httpRequest;
    }

    private HttpEntity createHttpEntity(HttpRequestParam param) {
        if (StringUtils.isEmpty(param.getBody())) {
            return null;
        }
        return new StringEntity(param.getBody(), ContentType.parse(param.getContentType()));
    }

    private URI createUri(HttpRequestParam param) throws URISyntaxException {

        URIBuilder uriBuilder = new URIBuilder(param.getUrl());

        if (!CollectionUtils.isEmpty(param.getQueryParam())) {
            for (Map.Entry<String, String> entry : param.getQueryParam().entrySet()) {
                uriBuilder.addParameter(entry.getKey(), entry.getValue());
            }
        }

        return uriBuilder.build();
    }

    private void withHeaders(HttpRequestParam param, HttpUriRequestBase httpRequest) {
        if (CollectionUtils.isEmpty(param.getHeaders())) return;
        for (Map.Entry<String, String> entry : param.getHeaders().entrySet()) {
            httpRequest.addHeader(entry.getKey(), entry.getValue());
        }
    }

    private Dataframe executeWithParser(HttpUriRequestBase httpRequest, HttpResponseParser parser) throws IOException {
        HttpClientResponseHandler<Dataframe> responseHandler =
                response -> parser.parseResponse(param.getTargetPropertyName(), response, param.getColumns());
        return httpClient.execute(httpRequest, responseHandler);
    }


}
