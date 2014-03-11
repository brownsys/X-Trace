package edu.brown.cs.systems.spans;

import edu.berkeley.xtrace.XTraceContext;
import edu.berkeley.xtrace.XTraceMetadata;

/**
 * Wrap a Runnable with a Span that survives a change in threads.
 */
public class TraceRunnable implements Runnable {

    private final byte[] parent; // xtrace TaskID
    private final Runnable runnable;
    private final String description;

    public TraceRunnable(Runnable runnable) {
        this(XTraceContext.logMerge().pack(), runnable);
    }

    public TraceRunnable(byte[] xtrace, Runnable runnable) {
        this(xtrace, runnable, null);
    }

    public TraceRunnable(byte[] xtrace, Runnable runnable, String description) {
        this.parent = xtrace;
        this.runnable = runnable;
        this.description = description;
    }

    @Override
    public void run() {
        if (parent != null) {
            XTraceContext.joinContext(XTraceMetadata.createFromBytes(parent, 0, parent.length));
            XTraceSpan chunk = XTraceSpanTrace.startSpan(getDescription());

            try {
                runnable.run();
            } finally {
                chunk.stop();
            }
        } else {
            runnable.run();
        }
    }

    private String getDescription() {
        return this.description == null ? Thread.currentThread().getName() : description;
    }
}
