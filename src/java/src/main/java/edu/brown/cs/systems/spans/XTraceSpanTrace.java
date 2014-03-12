package edu.brown.cs.systems.spans;

import java.lang.System;
import java.util.Collection;

import org.apache.log4j.Logger;

import edu.berkeley.xtrace.OptionField;
import edu.berkeley.xtrace.TaskID;
import edu.berkeley.xtrace.XTraceContext;
import edu.berkeley.xtrace.XTraceEvent;
import edu.berkeley.xtrace.XTraceMetadata;

import edu.berkeley.xtrace.config.XTraceConfiguration;
import edu.berkeley.xtrace.config.XTraceLogLevel;

public class XTraceSpanTrace {
    // Constants
    public static final byte TRACE_OPTION_FIELD_TYPE = 0x74;
    public static final String PARENT_SPAN_FIELDKEY = "PARENT_SPAN";
    public static final String ROOT_SPAN_STRING = "ROOT_SPAN";
    public static final String START_SPAN_STRING = "S";
    public static final String END_SPAN_STRING = "E";
    public static final String START_SPAN_FIELDKEY = "START_SPAN";
    public static final String END_SPAN_FIELDKEY = "END_SPAN";
    public static final String SPAN_AGENT = "XTRACE_SPAN";
    public static final String NO_DESC_GIVEN = "null";
    public static final String NULL_PARENT = "null";

    private static final Logger LOG = Logger.getLogger(XTraceSpanTrace.class);

    private static boolean isCurrentSpanSetInOptionFields() {
        XTraceMetadata xmd = getFirstCurrentXTraceMetadata();
        OptionField[] curOptions = xmd.getOptions();
        for (int i = 0; i < curOptions.length; i++) {
            if (curOptions[i].getType() == TRACE_OPTION_FIELD_TYPE) {
                return true;
            }
        }
        return false;
    }

    private static String getCurrentSpanFromOptionFields() {
        XTraceMetadata xmd = getFirstCurrentXTraceMetadata();
        OptionField[] curOptions = xmd.getOptions();
        for (int i = 0; i < curOptions.length; i++) {
            if (curOptions[i].getType() == TRACE_OPTION_FIELD_TYPE) {
                return new String(curOptions[i].getPayload());
            }
        }
        return null;
    }

    public static XTraceSpan startSpan(String description, byte[] parent) {
        if (parent != null) {
            XTraceMetadata xmd = XTraceMetadata.createFromBytes(parent,
                                                                0,
                                                                parent.length);
            if (xmd.isValid()) {
                XTraceContext.setThreadContext(xmd);
            }
        }
        return startSpan(description);
    }

    public static XTraceSpan startSpan(String description) {
        XTraceMetadata xmd = getFirstCurrentXTraceMetadata();
        XTraceEvent event = XTraceContext.createEvent(XTraceSpanTrace.class,
                                                      SPAN_AGENT,
                                                      description);
        if (!isCurrentSpanSetInOptionFields()) {
            // TODO: deal with this properly
            LOG.warn("Trying to set span without current span being set in option fields.");
            return null;
        }

        String curSpanString = getCurrentSpanFromOptionFields();
        LOG.trace("CUR_SPAN_STRING: " + curSpanString);
        event.put(START_SPAN_FIELDKEY, START_SPAN_STRING);
        event.put(PARENT_SPAN_FIELDKEY, curSpanString);
        event.sendReport();
        xmd = getFirstCurrentXTraceMetadata();
        String newSpanString = xmd.getOpIdString();
        System.out.println("start SPAN set current span: " + setCurrentSpanInOptions(newSpanString) + ". new: " + newSpanString + ". OLD: " + curSpanString);
        return new XTraceSpan(newSpanString, curSpanString);
    }

    // TODO: currently must be called exactly once. fix this.
    public static XTraceSpan startTrace(String agent, String description) {
        if (!XTraceConfiguration.ENABLED) {
            // TODO: have a null xtracespan to return here?
            return null;
        }

        Class<?> msgclass = XTraceLogLevel.DEFAULT;

        if (!XTraceContext.isValid()) {
            TaskID taskId = new TaskID(8);
            XTraceContext.setThreadContext(new XTraceMetadata(taskId, 0L));
            msgclass = XTraceLogLevel.ALWAYS; // always log a proper start event
        }

        XTraceEvent event = XTraceContext.createEvent(msgclass, agent, description);
        if (msgclass==XTraceLogLevel.ALWAYS) {
            event.put("Operation", "starttrace");
        }
        event.put(START_SPAN_FIELDKEY, START_SPAN_STRING);
        event.put(PARENT_SPAN_FIELDKEY, ROOT_SPAN_STRING);
        event.sendReport();

        XTraceMetadata xmd = getFirstCurrentXTraceMetadata();
        String curOpId = xmd.getOpIdString();
        xmd.addOption(createOptionFieldForOpId(curOpId));
        return new XTraceSpan(curOpId, ROOT_SPAN_STRING);
    }

    // TODO: add boolean flag to throw exception if more than one
    // metadata is current for assertion checking.
    private static XTraceMetadata getFirstCurrentXTraceMetadata() {
        Collection<XTraceMetadata> xmdCol = XTraceContext.getThreadContext(null);

        if (xmdCol.size() != 1) {
            LOG.warn("Multiple XTraceMetadata's.");
            System.out.println("MULTIPLE METADATAS");
        }

        return xmdCol.iterator().next();
    }

    private static OptionField createOptionFieldForOpId(String opId) {
        return new OptionField(TRACE_OPTION_FIELD_TYPE, opId.getBytes());
    }



    // removes the OptionField from the current metadata's option
    // fields that holds the current span's opID. returns whether any
    // OptionField was actually removed.
    private static boolean removeCurrentSpanFromOptions() {
        // TODO: could this be a problem if we get the first metadata
        // and the updated option is in another current metadata?
        XTraceMetadata xmd = getFirstCurrentXTraceMetadata();
        OptionField[] curOptions = xmd.getOptions();

        int foundIndex = -1;
        for (int i = 0; i < curOptions.length; i++) {
            if (curOptions[i].getType() == TRACE_OPTION_FIELD_TYPE) {
                foundIndex = i;
                break;
            }
        }

        if (foundIndex == -1) {
            System.out.println("DID NOT FIND. LENGTH: " + curOptions.length);
            return false;
        }

        OptionField[] newOptions = new OptionField[curOptions.length - 1];

        if (newOptions.length > 0) {
            System.arraycopy(curOptions, 0, newOptions, 0, foundIndex);
            System.arraycopy(curOptions, foundIndex + 1,
                             newOptions, foundIndex,
                             newOptions.length - foundIndex - 1);
        }

        // TODO: remove this (test)
        System.out.println("---------------------------------------------------");
        System.out.println("CUR OPTIONS:\n");
        for (OptionField of : curOptions) {
            if (of != null) {
                System.out.println("OPTION FIELD: " + new String(of.getPayload()));
            } else {
                System.out.println("OPTION FIELD NULL");
            }
        }
        System.out.println("NEW OPTIONS:\n");
        for (OptionField of : newOptions) {
            System.out.println("OPTION FIELD: " + of);
        }
        System.out.println("---------------------------------------------------");

        xmd.setOptions(newOptions);
        return true;
    }

    private static boolean setCurrentSpanInOptions(String opId) {
        XTraceMetadata xmd = getFirstCurrentXTraceMetadata();
        System.out.println("size before removing: " + xmd.getNumOptions());
        boolean toReturn = removeCurrentSpanFromOptions();
        System.out.println("size after removing and before adding new: " + xmd.getNumOptions());
        xmd.addOption(createOptionFieldForOpId(opId));
        System.out.println("size after adding new: " + xmd.getNumOptions());
        return toReturn;
    }

    public static void stopSpan(XTraceSpan toStop) {
        XTraceContext.logEvent(SPAN_AGENT,
                               NO_DESC_GIVEN,
                               END_SPAN_FIELDKEY,
                               toStop.getSpan());
        System.out.println("stopSPAN CURRENT: " + setCurrentSpanInOptions(toStop.getSavedSpan()));
    }

    public static Runnable wrap(Runnable runnable) {
        // TODO: check if tracing here and do nothing if not
        return new TraceRunnable(runnable);
    }
}
