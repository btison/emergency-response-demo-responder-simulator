package com.redhat.cajun.navy.responder.simulator.tracing;

import java.util.Iterator;
import java.util.Map;

import io.opentracing.propagation.TextMap;

public class HeaderMapExtractAdapter implements TextMap {

    private final Map<String, String> header;

    public HeaderMapExtractAdapter(Map<String, String> header) {
        this.header = header;
    }

    @Override
    public Iterator<Map.Entry<String, String>> iterator() {
        return header.entrySet().iterator();
    }

    @Override
    public void put(String key, String value) {
        throw new UnsupportedOperationException("ExtractAdapter should only be used with Tracer.extract()");
    }
}
