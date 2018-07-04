package io.github.lunarwatcher.chatbot.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jsoup.Connection;

import java.io.IOException;

public final class JsonUtils {
    private JsonUtils(){}

    public static JsonNode convertToJson(Connection.Response response) throws IOException{
        return convertToJson(response.body());
    }

    public static JsonNode convertToJson(String string) throws IOException {
        return new ObjectMapper().readTree(string);
    }

}
