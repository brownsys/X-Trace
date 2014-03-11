package edu.brown.cs.systems.spans;

public class XTraceSpan {
    // SpanID for this scope (is an xtrace opid)
    private final String span;

    // SpanID for the span that was "current" before this scope was entered
    private final String savedSpan;

    public XTraceSpan(String span, String saved) {
        this.span = span;
        this.savedSpan = saved;
    }

    public String getSpan() {
        return span;
    }

    public String getSavedSpan() {
        return savedSpan;
    }

    public void stop() {
        XTraceSpanTrace.stopSpan(this);
    }
}
