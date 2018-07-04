package io.github.lunarwatcher.chatbot.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.helper.HttpConnection;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Uses Jsoup to connect to and handle connections.
 * Originally, this class used Apache Http.
 */
public class HttpHelper {
    public static final int TIMEOUT = 10000;
    public static final String USERAGENT = "Mozilla";
    private static final Pattern response409Regex = Pattern.compile("\\d+");

    public static Response get(@NotNull String url, @NotNull Map<String, String> cookies, String... data) throws IOException {
        return get(url, false, cookies, data);
    }

    public static Response get(@NotNull String url, boolean ignoreHttpErrors, @NotNull Map<String, String> cookies, String... data) throws IOException {
        return send(Method.GET, url, ignoreHttpErrors, cookies, data);
    }

    public static Response post(@NotNull String url, @NotNull Map<String, String> cookies, String... data) throws IOException {
        return post(url, false, cookies, data);
    }

    public static Response post(@NotNull String url, boolean ignoreHttpErrors, @NotNull Map<String, String> cookies, String... data) throws IOException {
        return send(Method.POST, url, ignoreHttpErrors, cookies, data);
    }

    public static JsonNode getForJson(@NotNull String url, boolean ignoreHttpErrors, @NotNull Map<String, String> cookies, String... data) throws IOException {
        return new ObjectMapper().readTree(send(Method.POST, url, ignoreHttpErrors, cookies, data).body());
    }

    public static JsonNode postForJson(@NotNull String url, boolean ignoreHttpErrors, @NotNull Map<String, String> cookies, String... data) throws IOException {
        return new ObjectMapper().readTree(send(Method.POST, url, ignoreHttpErrors, cookies, data).body());
    }


    /**
     * Secondary send method, interfacing with {@link #send(Method, String, boolean, Map, String...)} using default values
     */
    @NotNull
    public static Response send(@NotNull Method method, @NotNull String url, Map<String, String> cookies, String... data) throws IOException{
        return send(method, url, false, cookies, data);
    }

    /**
     * Primary send method. Uses Jsoup.
     * @param method The method to use {@link Method}
     * @param url The URL
     * @param ignoreHttpErrors Whether or not to ignore http errors
     * @param cookies A Map<String, String> containing cookies
     * @param data Optional data
     * @return A Response containing the result of the connection.
     */
    @NotNull
    public static Response send(Method method, @NotNull String url, boolean ignoreHttpErrors, Map<String, String> cookies, String... data) throws IOException {
        Connection connection = Jsoup.connect(url)
                .method(method)
                .cookies(cookies)
                .timeout(TIMEOUT).ignoreContentType(true)
                .ignoreHttpErrors(ignoreHttpErrors)
                .data(data);
        try {
            Response response = connection.execute();
            cookies.putAll(response.cookies());
            return response;
        }catch(IOException e){
            System.out.println(url);
            throw new RuntimeException(e);
        }
    }

}