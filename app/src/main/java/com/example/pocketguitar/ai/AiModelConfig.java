package com.example.pocketguitar.ai;

/** OpenAI-compatible model connection settings; the API key is never printed. */
public final class AiModelConfig {
    public static final String DEFAULT_SYSTEM_PROMPT =
            "你是吉他谱曲助手。只完成用户当前的谱曲请求，输出严格、可解析的结果。";

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final String systemPrompt;

    public AiModelConfig(String baseUrl, String apiKey, String model, String systemPrompt) {
        this.baseUrl = strip(baseUrl);
        this.apiKey = strip(apiKey);
        this.model = strip(model);
        String prompt = strip(systemPrompt);
        this.systemPrompt = prompt.isEmpty() ? DEFAULT_SYSTEM_PROMPT : prompt;
    }

    public static AiModelConfig unconfigured() {
        return new AiModelConfig("", "", "", "");
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String apiKey() {
        return apiKey;
    }

    public String model() {
        return model;
    }

    public String systemPrompt() {
        return systemPrompt;
    }

    public boolean configured() {
        return !baseUrl.isEmpty() && !apiKey.isEmpty() && !model.isEmpty();
    }

    @Override
    public String toString() {
        return "AiModelConfig{baseUrl='" + baseUrl + "', model='" + model
                + "', apiKey=<redacted>}";
    }

    private static String strip(String value) {
        return value == null ? "" : value.trim();
    }
}
