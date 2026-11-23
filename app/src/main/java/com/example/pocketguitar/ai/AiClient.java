package com.example.pocketguitar.ai;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

/**
 * Stateless OpenAI-compatible Chat Completions client. Requests are sent with
 * {@code stream=true}; responses are parsed off the caller's thread and the
 * full text is accumulated into the returned future.
 */
public final class AiClient {
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final int MAX_ERROR_BODY_BYTES = 8 * 1024;
    private static final int MAX_NON_STREAM_BODY_BYTES = 2 * 1024 * 1024;
    private static final int MAX_ERROR_DETAIL_CHARS = 300;
    private static final OkHttpClient DEFAULT_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    private final AiModelConfig config;
    private final OkHttpClient client;

    public AiClient(AiModelConfig config) {
        this(config, DEFAULT_CLIENT);
    }

    public AiClient(AiModelConfig config, OkHttpClient client) {
        this.config = Objects.requireNonNull(config, "config");
        this.client = Objects.requireNonNull(client, "client");
    }

    public AiModelConfig config() {
        return config;
    }

    public boolean ready() {
        return config.configured();
    }

    public CompletableFuture<String> complete(String prompt, Consumer<String> onDelta) {
        Objects.requireNonNull(prompt, "prompt");
        Consumer<String> listener = onDelta == null ? ignored -> { } : onDelta;

        if (!ready()) {
            return failed(new AiModelException(
                    AiModelException.Kind.NOT_CONFIGURED, "未配置模型"));
        }
        if (prompt.trim().isEmpty()) {
            return failed(new AiModelException(
                    AiModelException.Kind.INVALID_CONFIG, "提示词为空"));
        }

        HttpUrl url;
        try {
            url = HttpUrl.get(endpointFor(config.baseUrl()));
        } catch (IllegalArgumentException error) {
            return failed(new AiModelException(
                    AiModelException.Kind.INVALID_CONFIG, "模型接口地址无效", error));
        }

        String body = buildRequestBody(config.model(), config.systemPrompt(), prompt);
        Request request;
        try {
            request = new Request.Builder()
                    .url(url)
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Accept", "text/event-stream")
                    .header("Content-Type", "application/json; charset=utf-8")
                    .post(RequestBody.create(body, JSON))
                    .build();
        } catch (IllegalArgumentException error) {
            return failed(new AiModelException(
                    AiModelException.Kind.INVALID_CONFIG, "模型请求配置无效", error));
        }

        CompletableFuture<String> future = new CompletableFuture<>();
        Call call = client.newCall(request);
        future.whenComplete((value, error) -> {
            if (future.isCancelled()) {
                call.cancel();
            }
        });
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException error) {
                if (future.isCancelled()) {
                    return;
                }
                future.completeExceptionally(new AiModelException(
                        AiModelException.Kind.TRANSPORT,
                        "网络请求失败：" + safeMessage(error), error));
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (Response closed = response) {
                    if (!closed.isSuccessful()) {
                        String raw = readLimited(closed.body(), MAX_ERROR_BODY_BYTES);
                        future.completeExceptionally(httpError(closed.code(), raw));
                        return;
                    }
                    ResponseBody responseBody = closed.body();
                    if (responseBody == null) {
                        future.completeExceptionally(new AiModelException(
                                AiModelException.Kind.PROTOCOL, "模型返回了空响应"));
                        return;
                    }
                    readSuccessful(responseBody, future, listener);
                } catch (AiModelException error) {
                    future.completeExceptionally(error);
                } catch (IOException error) {
                    if (!future.isCancelled()) {
                        future.completeExceptionally(new AiModelException(
                                AiModelException.Kind.TRANSPORT,
                                "读取模型响应失败：" + safeMessage(error), error));
                    }
                } catch (RuntimeException error) {
                    if (!future.isCancelled()) {
                        future.completeExceptionally(new AiModelException(
                                AiModelException.Kind.PROTOCOL, "处理模型响应失败", error));
                    }
                }
            }
        });
        return future;
    }

    static String endpointFor(String baseUrl) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("baseUrl must not be blank");
        }
        String endpoint = baseUrl.trim();
        while (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        String lower = endpoint.toLowerCase(Locale.ROOT);
        if (lower.endsWith("/chat/completions")) {
            return endpoint;
        }
        if (lower.endsWith("/v1")) {
            return endpoint + "/chat/completions";
        }
        return endpoint + "/v1/chat/completions";
    }

    static String buildRequestBody(String model, String systemPrompt, String prompt) {
        JsonObject root = new JsonObject();
        root.addProperty("model", model);
        root.addProperty("stream", true);

        JsonArray messages = new JsonArray();
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            JsonObject system = new JsonObject();
            system.addProperty("role", "system");
            system.addProperty("content", systemPrompt);
            messages.add(system);
        }
        JsonObject user = new JsonObject();
        user.addProperty("role", "user");
        user.addProperty("content", prompt);
        messages.add(user);

        root.add("messages", messages);
        return GSON.toJson(root);
    }

    private void readSuccessful(
            ResponseBody body,
            CompletableFuture<String> future,
            Consumer<String> onDelta) throws IOException {
        BufferedSource source = body.source();
        if (looksLikeSse(body, source)) {
            readSse(source, future, onDelta);
            return;
        }
        String payload = readLimited(source, MAX_NON_STREAM_BODY_BYTES);
        String text = ChatCompletionJson.completedText(payload);
        if (text == null || text.trim().isEmpty()) {
            future.completeExceptionally(new AiModelException(
                    AiModelException.Kind.EMPTY_RESPONSE, "模型返回为空"));
            return;
        }
        onDelta.accept(text);
        future.complete(text);
    }

    private void readSse(
            BufferedSource source,
            CompletableFuture<String> future,
            Consumer<String> onDelta) throws IOException {
        ChatSseParser parser = new ChatSseParser();
        StringBuilder answer = new StringBuilder();
        boolean done = false;
        String line;
        while (!done && (line = source.readUtf8Line()) != null) {
            ChatSseParser.Event event = parser.accept(line);
            if (event != null) {
                done = consume(event, answer, onDelta);
            }
        }
        if (!done) {
            ChatSseParser.Event tail = parser.flush();
            if (tail != null) {
                consume(tail, answer, onDelta);
            }
        }
        if (future.isDone()) {
            return;
        }
        if (answer.toString().trim().isEmpty()) {
            future.completeExceptionally(new AiModelException(
                    AiModelException.Kind.EMPTY_RESPONSE, "模型返回为空"));
            return;
        }
        future.complete(answer.toString());
    }

    private boolean consume(
            ChatSseParser.Event event,
            StringBuilder answer,
            Consumer<String> onDelta) {
        if ("[DONE]".equals(event.data.trim())) {
            return true;
        }
        String delta = ChatCompletionJson.deltaText(event.data);
        if (delta != null && !delta.isEmpty()) {
            answer.append(delta);
            onDelta.accept(delta);
        }
        return false;
    }

    private AiModelException httpError(int statusCode, String payload) {
        String detail = ChatCompletionJson.errorMessage(payload);
        if (detail == null || detail.trim().isEmpty()) {
            detail = payload;
        }
        detail = oneLine(detail);
        if (detail.length() > MAX_ERROR_DETAIL_CHARS) {
            detail = detail.substring(0, MAX_ERROR_DETAIL_CHARS) + "…";
        }
        String message = "模型接口返回 HTTP " + statusCode;
        if (!detail.trim().isEmpty()) {
            message += "：" + detail;
        }
        return new AiModelException(AiModelException.Kind.HTTP, statusCode, redact(message));
    }

    private String redact(String value) {
        String key = config.apiKey();
        if (value == null || key.isEmpty()) {
            return value;
        }
        return value.replace(key, "<redacted>");
    }

    private static String oneLine(String value) {
        if (value == null) {
            return "";
        }
        String cleaned = value.replace('\r', ' ').replace('\n', ' ').trim();
        StringBuilder safe = new StringBuilder(cleaned.length());
        for (int i = 0; i < cleaned.length(); i++) {
            char ch = cleaned.charAt(i);
            safe.append(Character.isISOControl(ch) ? ' ' : ch);
        }
        return safe.toString().replaceAll("\\s+", " ").trim();
    }

    private static String safeMessage(Throwable error) {
        String message = error == null ? "" : error.getMessage();
        return message == null || message.trim().isEmpty()
                ? (error == null ? "未知错误" : error.getClass().getSimpleName())
                : message;
    }

    private static String readLimited(ResponseBody body, int maxBytes) throws IOException {
        if (body == null) {
            return "";
        }
        return readLimited(body.source(), maxBytes);
    }

    private static String readLimited(BufferedSource source, int maxBytes) throws IOException {
        Buffer buffer = new Buffer();
        source.read(buffer, maxBytes);
        return buffer.readUtf8();
    }

    private static boolean looksLikeSse(ResponseBody body, BufferedSource source) throws IOException {
        MediaType mediaType = body.contentType();
        if (mediaType != null) {
            if ("text".equals(mediaType.type()) && "event-stream".equals(mediaType.subtype())) {
                return true;
            }
            if ("application".equals(mediaType.type())
                    && ("json".equals(mediaType.subtype())
                    || mediaType.subtype().endsWith("+json"))) {
                return false;
            }
        }
        BufferedSource peek = source.peek();
        for (int i = 0; i < 8; i++) {
            String line = peek.readUtf8Line();
            if (line == null) {
                return false;
            }
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            return trimmed.startsWith("data:")
                    || trimmed.startsWith("event:")
                    || trimmed.startsWith("id:")
                    || trimmed.startsWith("retry:")
                    || trimmed.startsWith(":");
        }
        return false;
    }

    private static CompletableFuture<String> failed(AiModelException error) {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(error);
        return future;
    }
}
