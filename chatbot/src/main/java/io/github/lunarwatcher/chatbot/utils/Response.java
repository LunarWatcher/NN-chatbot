package io.github.lunarwatcher.chatbot.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class Response {
    private int statusCode;
    private String body;

    public Response(int statusCode, String body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }

    public JsonNode getBodyAsJson() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw e;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}