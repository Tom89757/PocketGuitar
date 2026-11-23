package com.example.pocketguitar.ai;

import java.util.ArrayList;
import java.util.List;

/** Incremental parser for server-sent events used by Chat Completions streams. */
public final class ChatSseParser {
    private final List<String> data = new ArrayList<>();
    private String event = "message";
    private boolean firstLine = true;

    public static final class Event {
        public final String event;
        public final String data;

        public Event(String event, String data) {
            this.event = event == null || event.trim().isEmpty() ? "message" : event;
            this.data = data == null ? "" : data;
        }
    }

    /** Feeds one line without its terminator; returns a complete event or null. */
    public Event accept(String rawLine) {
        String line = rawLine;
        if (line != null && line.endsWith("\r")) {
            line = line.substring(0, line.length() - 1);
        }
        if (line != null && firstLine && line.startsWith("\uFEFF")) {
            line = line.substring(1);
        }
        firstLine = false;
        if (line == null || line.isEmpty()) {
            return dispatch();
        }
        if (line.startsWith(":")) {
            return null;
        }
        int colon = line.indexOf(':');
        String field = colon < 0 ? line : line.substring(0, colon);
        String value = colon < 0 ? "" : line.substring(colon + 1);
        if (value.startsWith(" ")) {
            value = value.substring(1);
        }
        if ("event".equals(field)) {
            event = value;
        } else if ("data".equals(field)) {
            data.add(value);
        }
        return null;
    }

    public Event flush() {
        return dispatch();
    }

    private Event dispatch() {
        if (data.isEmpty()) {
            event = "message";
            return null;
        }
        Event complete = new Event(event, join(data));
        data.clear();
        event = "message";
        return complete;
    }

    private static String join(List<String> values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }
}
