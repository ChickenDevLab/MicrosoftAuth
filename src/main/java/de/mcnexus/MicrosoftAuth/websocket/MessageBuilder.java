package de.mcnexus.MicrosoftAuth.websocket;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class MessageBuilder {
    private JsonObject object = new JsonObject();

    public MessageBuilder code(String code) {
        object.addProperty("code", code);
        return this;
    }

    public MessageBuilder payload(String payload, String content) {
        object.addProperty(payload, content);
        return this;
    }

    public MessageBuilder payload(String payload, JsonElement element){
        object.add(payload, element);
        return this;
    }

    @Override
    public String toString() {
        return object.toString();
    }
}
