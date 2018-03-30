package io.github.lunarwatcher.chatbot;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * This class is the memory core of the bot, it saves all the data it gets into a .json database.
 * .json is used because implementing SQL is overkill and using regular .txt files is a mess.
 */
@SuppressWarnings("unchecked")
public class Database {
    public Path file;
    private Map<String, Object> cache = new HashMap<>();
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DATE_FORMAT);
    private boolean changed = false;
    
    public Database(Path file) throws IOException {
        if(file == null)
            throw new RuntimeException("File cannot be null!");
        this.file = file;
        
        if (Files.exists(file)) {
            try {
                load();
            }catch(Exception e){
                e.printStackTrace();
                System.out.println("Ignored load failing.");
            }
        }else{

            Files.createFile(file);
        }
    }

    /**
     * Loads existing data from the file.
     * @throws IOException if there's a problem reading the file
     */
    private void load() throws IOException {
        JsonNode root;
        ObjectMapper mapper = new ObjectMapper();
        try (Reader reader = Files.newBufferedReader(file)) {
            root = mapper.readTree(reader);
        }

        Iterator<String> it = root.fieldNames();
        while (it.hasNext()) {
            String fieldName = it.next();
            JsonNode field = root.get(fieldName);
            Object value = parseNode(field);
            cache.put(fieldName, value);
        }
    }

    private Object parseNode(JsonNode node) {
        if (node.isArray()) {
            List<Object> list = new ArrayList<>();
            Iterator<JsonNode> it = node.elements();
            while (it.hasNext()) {
                JsonNode element = it.next();
                Object parsedElement = parseNode(element);
                list.add(parsedElement);
            }
            return list;
        }else if (node.isObject()) {
            Map<String, Object> map = new HashMap<>();
            Iterator<String> it = node.fieldNames();
            while (it.hasNext()) {
                String fieldName = it.next();
                JsonNode field = node.get(fieldName);
                Object parsedElement = parseNode(field);
                map.put(fieldName, parsedElement);
            }
            return map;
        }else if (node.isInt()) {
            return node.asInt();
        }else if(node.isLong()){
            return node.asLong();
        }else if(node.isDouble()){
            return node.asDouble();
        }else if(node.isFloat()){
            return node.floatValue();
        }else if(node.isBoolean()) {
            return node.asBoolean();
        }else if (node.isNull()) {
            return null;
        }

        String text = node.asText();

        try {
            return LocalDateTime.parse(text, formatter);
        } catch (DateTimeParseException e) {
            return text;
        }
    }

    /**
     * Get a value
     * @param key The key to retrieve
     * @return a value or null if key not found
     */
    public Object get(String key) {
        return cache.get(key);
    }

    /**
     * Put something into the data. Does not update until {@link #commit()} is called
     * @param key
     * @param value
     */
    public void put(String key, Object value) {
        cache.put(key, value);
        changed = true;

        if(Constants.AUTO_SAVE_WHEN_PUT){
            commit();
        }
    }

    /**
     * Commit the changes and write the data to the file
     */
    public void commit() {
        if (!changed) {
            return;
        }

        StandardOpenOption[] options;
        if (Files.exists(file)) {
            options = new StandardOpenOption[] { StandardOpenOption.TRUNCATE_EXISTING };
        } else {
            options = new StandardOpenOption[] {};
        }

        try (Writer writer = Files.newBufferedWriter(file, options)) {
            JsonFactory factory = new JsonFactory();
            try (JsonGenerator generator = factory.createGenerator(writer)) {
                generator.setPrettyPrinter(new DefaultPrettyPrinter());
                generator.writeStartObject();

                for (Map.Entry<String, Object> entry : cache.entrySet()) {
                    String fieldName = entry.getKey();
                    Object value = entry.getValue();

                    generator.writeFieldName(fieldName);
                    write(generator, value);
                }

                generator.writeEndObject();
            }
        } catch (IOException e) {
            System.err.println("Could not save data!");
        }

        changed = false;
    }

    /**
     * The method for the actual writing, does not include the names of fields with the exception for maps
     * @param generator The generator
     * @param value The value to write
     * @throws IOException If something goes wrong
     */
    private void write(JsonGenerator generator, Object value) throws IOException {
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            generator.writeStartObject();

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String fieldName = entry.getKey().toString();
                Object v = entry.getValue();

                generator.writeFieldName(fieldName);
                write(generator, v);
            }

            generator.writeEndObject();
            return;
        }else if (value instanceof Collection) {
            Collection<?> list = (Collection<?>) value;
            generator.writeStartArray();

            for (Object item : list) {
                write(generator, item);
            }

            generator.writeEndArray();
            return;
        }else if (value instanceof LocalDateTime) {
            LocalDateTime date = (LocalDateTime) value;
            generator.writeString(date.format(formatter));
            return;
        }else if (value instanceof Integer) {
            Integer integer = (Integer) value;
            generator.writeNumber(integer);
            return;
        }else if (value instanceof Long) {
            Long integer = (Long) value;
            generator.writeNumber(integer);
            return;
        }else if(value instanceof Boolean) {
            Boolean bool = (Boolean) value;
            generator.writeBoolean(bool);
            return;
        }else if (value == null) {
            generator.writeNull();
            return;
        }

        String string = value.toString();
        generator.writeString(string);
    }

    public Map<String, Object> getMap(String key){
        return (Map<String, Object>) get(key);
    }


    public List<Object> getList(String key){
        return (List<Object>) get(key);
    }


}
