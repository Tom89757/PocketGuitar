package com.example.pocketguitar.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Pure Chat Completions JSON helpers shared by streaming and non-streaming paths. */
public final class ChatCompletionJson {
    private ChatCompletionJson() {
    }

    public static String deltaText(String payload) {
        JsonObject choice = firstChoice(payload);
        if (choice == null) {
            return null;
        }
        JsonObject delta = objectOrNull(choice.get("delta"));
        String text = delta == null ? null : contentText(delta.get("content"));
        if (text == null) {
            text = primitiveOrNull(choice.get("text"));
        }
        return text;
    }

    public static String completedText(String payload) {
        JsonObject choice = firstChoice(payload);
        if (choice == null) {
            return null;
        }
        JsonObject message = objectOrNull(choice.get("message"));
        String text = message == null ? null : contentText(message.get("content"));
        if (text == null) {
            text = primitiveOrNull(choice.get("text"));
        }
        return text;
    }

    public static String errorMessage(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            return null;
        }
        try {
            JsonElement parsed = JsonParser.parseString(payload);
            if (!parsed.isJsonObject()) {
                return null;
            }
            JsonElement error = parsed.getAsJsonObject().get("error");
            if (error == null || error.isJsonNull()) {
                return null;
            }
            if (error.isJsonObject()) {
                JsonElement message = error.getAsJsonObject().get("message");
                return message == null ? null : primitiveOrNull(message);
            }
            return primitiveOrNull(error);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static JsonObject firstChoice(String payload) {
        if (payload == null || payload.trim().isEmpty()) {
            throw protocol("模型返回了空响应");
        }
        JsonElement parsed;
        try {
            parsed = JsonParser.parseString(payload);
        } catch (RuntimeException error) {
            throw protocol("模型返回格式无法解析", error);
        }
        if (!parsed.isJsonObject()) {
            throw protocol("模型返回格式无法解析");
        }
        JsonObject root = parsed.getAsJsonObject();
        JsonElement error = root.get("error");
        if (error != null && !error.isJsonNull()) {
            String message = errorMessage(payload);
            throw protocol(message == null || message.trim().isEmpty()
                    ? "模型返回错误"
                    : "模型返回错误：" + message);
        }
        JsonElement choices = root.get("choices");
        if (choices == null || !choices.isJsonArray()) {
            return null;
        }
        JsonArray array = choices.getAsJsonArray();
        if (array.size() == 0) {
            return null;
        }
        return objectOrNull(array.get(0));
    }

    private static String contentText(JsonElement content) {
        if (content == null || content.isJsonNull()) {
            return null;
        }
        if (content.isJsonPrimitive()) {
            return content.getAsString();
        }
        if (!content.isJsonArray()) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        for (JsonElement part : content.getAsJsonArray()) {
            JsonObject object = objectOrNull(part);
            if (object == null) {
                continue;
            }
            String piece = primitiveOrNull(object.get("text"));
            if (piece != null) {
                text.append(piece);
            }
        }
        return text.length() == 0 ? null : text.toString();
    }

    private static JsonObject objectOrNull(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static String primitiveOrNull(JsonElement element) {
        return element != null && element.isJsonPrimitive() ? element.getAsString() : null;
    }

    private static AiModelException protocol(String message) {
        return new AiModelException(AiModelException.Kind.PROTOCOL, message);
    }

    private static AiModelException protocol(String message, Throwable cause) {
        return new AiModelException(AiModelException.Kind.PROTOCOL, message, cause);
    }
}
