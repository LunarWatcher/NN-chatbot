package io.github.lunarwatcher.chatbot.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Http implements Closeable {

    private final CloseableHttpClient client;

    public Http(CloseableHttpClient client) {
        this.client = client;

    }

    public Response get(String uri) throws IOException {
        HttpGet request = new HttpGet(uri);

        return send(request);
    }


    public Response get(String uri, List<Object> params) throws IOException{
        StringBuilder preparedUri = new StringBuilder(uri + "?");
        for(int i = 0; i < params.size(); i += 2){
            preparedUri.append(i == 0 ? "" : "&").append(params.get(i).toString()).append("=").append(params.get(i + 1));
        }

        HttpGet request = new HttpGet(preparedUri.toString());

        return send(request);
    }

    public Response post(String uri, Object... parameters) throws IOException {
        if (parameters.length % 2 != 0) {
            throw new IllegalArgumentException("\"parameters\" vararg must have an even number of values.");
        }

        HttpPost request = new HttpPost(uri);

        if (parameters.length > 0) {
            List<NameValuePair> params = new ArrayList<>(parameters.length / 2);
            for (int i = 0; i < parameters.length; i += 2) {
                params.add(new BasicNameValuePair(parameters[i].toString(), parameters[i + 1].toString()));
            }
            request.setEntity(new UrlEncodedFormEntity(params, Consts.UTF_8));
        }

        return send(request);
    }

    public Response post(String uri, String[] headers, Object... parameters) throws IOException {
        if (parameters.length % 2 != 0) {
            throw new IllegalArgumentException("\"parameters\" vararg must have an even number of values.");
        }
        if (headers.length % 2 != 0) {
            throw new IllegalArgumentException("\"headers\" array must have an even number of values.");
        }

        HttpPost request = new HttpPost(uri);
        if(headers.length > 0){
            List<Header> headersL = new ArrayList<>();
            for(int i = 0; i < headers.length; i+= 2){
                headersL.add(new BasicHeader(headers[i], headers[i + 1]));
            }
            request.setHeaders(headersL.toArray(new Header[0]));
        }
        if (parameters.length > 0) {
            List<NameValuePair> params = new ArrayList<>(parameters.length / 2);
            for (int i = 0; i < parameters.length; i += 2) {
                params.add(new BasicNameValuePair(parameters[i].toString(), parameters[i + 1].toString()));
            }
            request.setEntity(new UrlEncodedFormEntity(params, Consts.UTF_8));
        }

        return send(request);
    }

    private Response send(HttpUriRequest request) throws IOException {
        long sleep = 0;
        int attempts = 0;

        while (attempts < 5) {
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            int statusCode;
            String body;
            try (CloseableHttpResponse response = client.execute(request)) {
                statusCode = response.getStatusLine().getStatusCode();
                body = EntityUtils.toString(response.getEntity());
            }

            if (statusCode == 409) {
                Long waitTime = parse409Response(body);
                sleep = (waitTime == null) ? 5000 : waitTime;

                attempts++;
                continue;
            }

            return new Response(statusCode, body);

        }

        throw new IOException("Request could not be sent after " + attempts + " attempts [request-method=" + request.getMethod() + "; request-URI=" + request.getURI() + "].");
    }

    private static final Pattern response409Regex = Pattern.compile("\\d+");

    private static Long parse409Response(String body) {
        Matcher m = response409Regex.matcher(body);
        if (!m.find()) {
            return null;
        }

        int seconds = Integer.parseInt(m.group(0));
        return TimeUnit.SECONDS.toMillis(seconds);
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

}