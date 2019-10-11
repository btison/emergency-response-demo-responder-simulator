package com.redhat.cajun.navy.responder.simulator.tracing;


import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import io.opentracing.propagation.TextMap;

public class HeaderMapInjectAdapter implements TextMap {

    private final Map<String, String> header;

    HeaderMapInjectAdapter(Map<String, String> header) {
        this.header = header;
    }

    @Override
    public Iterator<Entry<String, String>> iterator() {
        throw new UnsupportedOperationException("iterator should never be used with Tracer.inject()");
    }

    @Override
    public void put(String key, String value) {
        header.put(key, value);
    }
}
