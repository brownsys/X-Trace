/*
 * Copyright (c) 2005,2006,2007 The Regents of the University of California.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the University of California, nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE UNIVERSITY OF CALIFORNIA ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package edu.berkeley.xtrace;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;

/**
 * High-level API for maintaining a per-thread X-Trace context (task and
 * operation ID) and reporting events.
 * 
 * Usage:
 * <ul>
 * <li>When communication is received, set the context using
 * <code>XTraceContext.setThreadContext()</code>.
 * <li>To record an operation, call <code>XTraceContext.logEvent()</code>. Or,
 * to add extra fields to the event report, call
 * <code>XTraceContext.createEvent()</code>, add fields to the returned
 * {@link XTraceEvent} object, and send it using
 * {@link XTraceEvent#sendReport()}.
 * <li>When calling another service, get the current context's metadata using
 * <code>XTraceContext.getThreadContext()</code> and send it to the destination
 * service as a field in your network protocol. After receiving a reply, add an
 * edge from both the reply's metadata and the current context in the report for
 * the reply.
 * <li>Clear the context using <code>XTraceContext.clearThreadContext()</code>.
 * </ul>
 * 
 * @author Matei Zaharia <matei@berkeley.edu>
 */
public class XTraceContext {	

	/** When the XTraceContext class is loaded, it will check to see whether there is 
	 * an environment variable set.  If there is, it assumes that this process is the
	 * continuation of some previous trace, and sets the thread context accordingly. **/
	public static final String XTRACE_CONTEXT_ENV_VARIABLE = "XTRACE_STARTING_CONTEXT";
	
	
	/** Thread-local current operation context(s), used in logEvent. **/
	private static ThreadLocal<Collection<XTraceMetadata>> contexts = new ThreadLocal<Collection<XTraceMetadata>>() {
		@Override
		protected Collection<XTraceMetadata> initialValue() {
			return new XTraceMetadataCollection();
		}
	};

	/** Cached hostname of the current machine. **/
	private static String hostname = null;
	static int defaultOpIdLength = 8;

	/**
	 * Set the X-Trace context for the current thread, to link it causally to
	 * events that may have happened in a different thread or on a different
	 * host.
	 * 
	 * @param ctx
	 *            the new context
	 */
	public static void setThreadContext(XTraceMetadata ctx) {
		clearThreadContext();
		joinContext(ctx);
	}

	/**
	 * Set the X-Trace contexts for the current thread, to link it causally to
	 * events that may have happened in a different thread or on a different
	 * host.
	 * 
	 * @param ctx
	 *            the new context
	 */
	public static void setThreadContext(Collection<XTraceMetadata> ctxs) {
		clearThreadContext();
		joinContext(ctxs);
	}
	
	/**
	 * Adds the provided X-Trace context to the current contexts.  The
	 * next event will be causally linked to all of the current contexts.
	 * This is typically used when two threads are converging
	 * @param ctx
	 * 			  a context to add
	 */
	public static void joinContext(XTraceMetadata ctx) {
		if (ctx==null || !ctx.isValid()) {
			return;
		}
		contexts.get().add(ctx);
	}
	
	/**
	 * Adds the provided X-Trace contexts to the current contexts.  The
	 * next event will be causally linked to all of the current contexts.
	 * This is typically used when two threads are converging
	 * @param ctxs
	 * 			  prior context(s) to add
	 */
	public static void joinContext(Collection<XTraceMetadata> ctxs) {
		if (ctxs == null) {
			return;
		}
		contexts.get().addAll(ctxs);
	}

	/**
	 * Get the current thread's X-Trace context, that is, the metadata for the
	 * last event to have been logged by this thread.
	 * If there is no context or the context is not valid, this method returns
	 * null (as opposed to an empty collection)
	 * 
	 * @return a *copy* of the collection of this thread's current context
	 */
	public static Collection<XTraceMetadata> getThreadContext() {
		if (XTraceContext.isValid()) {
			return new ArrayList<XTraceMetadata>(contexts.get());
		}
		return null;
	}

	/**
	 * Adds the current thread's X-Trace context to the provided collection
	 * If the current context is not valid, this method does nothing
	 * For convenience, returns the provided collection
	 * @param ctx a collection to add the current X-Trace context to.  If ctx
	 * is null, a new collection is created
	 * @return the provided collection if it was not null, otherwise a new collection
	 */
	public static Collection<XTraceMetadata> getThreadContext(Collection<XTraceMetadata> ctx) {
		if (!XTraceContext.isValid()) {
			return ctx;
		}
		if (ctx==null) {
			ctx = new XTraceMetadataCollection();
		}
		ctx.addAll(contexts.get());
		return ctx;
	}

	/**
	 * Clear current thread's X-Trace context.
	 */
	public static void clearThreadContext() {
		contexts.get().clear();
	}

	
	/**
	 * This method ensures that the current thread context consists of only a single
	 * XTraceMetadata instance.  It checks to see the number of parent XTraceMetadata
	 * and does the following:
	 *  - If we are currently invalid, returns null
	 *  - If there is only one parent XTraceMetadata, then no further actions are
	 *    performed and that parent XTraceMetadata is returned.
	 *  - If there are multiple parent XTraceMetadata, a new event is logged, and the
	 *    subsequent single XTraceMetadata is returned.
	 */
	public static XTraceMetadata logMerge() {
		if (!isValid()) {
			return null;
		}

		Collection<XTraceMetadata> metadatas = contexts.get();
		if (metadatas.size()==1) {
			// Don't need to create a new event if we are already at a single metadata
			return metadatas.iterator().next();
		}
		
		int opIdLength = defaultOpIdLength;
		if (metadatas.size()!=0) {
			opIdLength = metadatas.iterator().next().getOpIdLength();
		}
		
		XTraceEvent event = new XTraceEvent(opIdLength);

		for (XTraceMetadata m : metadatas) {
			event.addEdge(m);
		}

		try {
			if (hostname == null) {
				hostname = InetAddress.getLocalHost().getHostName();
			}
		} catch (UnknownHostException e) {
			hostname = "unknown";
		}

		event.put("Host", hostname);
		event.put("Operation", "merge");

		XTraceMetadata newcontext = event.getNewMetadata();
		setThreadContext(newcontext);
		event.sendReport();
		return newcontext;
	}
	
		
	/**
	 * Creates a new task context, adds an edge from the current thread's
	 * context, sets the new context, and reports it to the X-Trace server.
	 * 
	 * @param agent
	 *            name of current agent
	 * @param label
	 *            description of the task
	 */
	public static void logEvent(String agent, String label) {
		logEvent(XTraceLogLevel.DEFAULT, agent, label);
	}

	/**
	 * Creates a new task context, adds an edge from the current thread's
	 * context, sets the new context, and reports it to the X-Trace server.
	 * 
	 * @param msgclass
	 * 			  optional class that can be turned on/off
	 * @param agent
	 *            name of current agent
	 * @param label
	 *            description of the task
	 */
	public static void logEvent(Class<?> cls, String agent, String label) {
		if (!isValid() || !XTraceLogLevel.isOn(cls)) {
			return;
		}
		createEvent(cls, agent, label).sendReport();
	}

	/**
	 * Creates a new task context, adds an edge from the current thread's
	 * context, sets the new context, and reports it to the X-Trace server. This
	 * version of this function allows extra event fields to be specified as
	 * variable arguments after the agent and label. For example, to add a field
	 * called "DataSize" with value 4320, use
	 * 
	 * <code>XTraceContext.logEvent("agent", "label", "DataSize" 4320)</code>
	 * 
	 * @param agent
	 *            name of current agent
	 * @param label
	 *            description of the task
	 */
	public static void logEvent(String agent, String label, Object... args) {
		if (!isValid()) {
			return;
		}
		if (args.length % 2 != 0) {
			throw new IllegalArgumentException(
					"XTraceContext.logEvent requires an even number of arguments.");
		}
		XTraceEvent event = createEvent(XTraceLogLevel.DEFAULT, agent, label);
		for (int i = 0; i < args.length / 2; i++) {
			String key = args[2 * i].toString();
			String value = args[2 * i + 1].toString();
			event.put(key, value);
		}
		event.sendReport();
	}

	/**
	 * Creates a new task context, adds an edge from the current thread's
	 * context, sets the new context, and reports it to the X-Trace server. This
	 * version of this function allows extra event fields to be specified as
	 * variable arguments after the agent and label. For example, to add a field
	 * called "DataSize" with value 4320, use
	 * 
	 * <code>XTraceContext.logEvent("agent", "label", "DataSize" 4320)</code>
	 * 
	 * @param msgclass
	 * 			  arbitrary class value that can be used to control logging visibility
	 * @param agent
	 *            name of current agent
	 * @param label
	 *            description of the task
	 */
	public static void logEvent(Class<?> msgclass, String agent, String label, Object... args) {
		if (!isValid() || !XTraceLogLevel.isOn(msgclass)) {
			return;
		}
		if (args.length % 2 != 0) {
			throw new IllegalArgumentException(
					"XTraceContext.logEvent requires an even number of arguments.");
		}
		XTraceEvent event = createEvent(msgclass, agent, label);
		for (int i = 0; i < args.length / 2; i++) {
			String key = args[2 * i].toString();
			String value = args[2 * i + 1].toString();
			event.put(key, value);
		}
		event.sendReport();
	}

	/**
	 * Creates a new event context, adds an edge from the current thread's
	 * context, and sets the new context. Returns the newly created event
	 * without reporting it. If there is no current thread context, nothing is
	 * done.
	 * 
	 * The returned event can be sent with {@link XTraceEvent#sendReport()}.
	 * 
	 * @param agent
	 *            name of current agent
	 * @param label
	 *            description of the task
	 */
	public static XTraceEvent createEvent(String agent, String label) {
		return createEvent(XTraceLogLevel.DEFAULT, agent, label);
	}


	/**
	 * Creates a new event context, adds an edge from the current thread's
	 * context, and sets the new context. Returns the newly created event
	 * without reporting it. If there is no current thread context, nothing is
	 * done.
	 * 
	 * The returned event can be sent with {@link XTraceEvent#sendReport()}.
	 * 
	 * @param msgclass
	 * 			  arbitrary class value that can be used to control logging visibility
	 * @param agent
	 *            name of current agent
	 * @param label
	 *            description of the task
	 */
	public static XTraceEvent createEvent(Class<?> msgclass, String agent, String label) {
		if (!isValid()) {
			return null;
		}

		Collection<XTraceMetadata> oldContext = contexts.get();
		int opIdLength = defaultOpIdLength;
		if (oldContext.size()!=0) {
			opIdLength = oldContext.iterator().next().getOpIdLength();
		}
		
		XTraceEvent event = new XTraceEvent(msgclass, opIdLength);

		for (XTraceMetadata m : oldContext) {
			event.addEdge(m);
		}

		try {
			if (hostname == null) {
				hostname = InetAddress.getLocalHost().getHostName();
			}
		} catch (UnknownHostException e) {
			hostname = "unknown";
		}

		event.put("Host", hostname);
		event.put("Agent", agent);
		event.put("Label", label);

		if (XTraceLogLevel.isOn(msgclass)) {
			setThreadContext(event.getNewMetadata());
		}
		return event;		
	}

	/**
	 * Is there a context set for the current thread?
	 * 
	 * @return true if there is a current context
	 */
	public static boolean isValid() {
		Collection<XTraceMetadata> ctxs = contexts.get();
		return ctxs!=null && !ctxs.isEmpty();
	}

	/**
	 * Begin a "process", which will be ended with
	 * {@link #endProcess(XTraceProcess)}, by creating an event with the given
	 * agent and label strings. This function returns an XtrMetadata object that
	 * must be passed into {@link #endProcess(XTraceProcess)} or
	 * {@link #failProcess(XTraceProcess, Throwable)} to create the
	 * corresponding process-end event.
	 * 
	 * Example usage:
	 * 
	 * <pre>
	 * XtraceProcess process = XTrace.startProcess("node", "action start");
	 * ...
	 * XTrace.endProcess(process);
	 * </pre>
	 * 
	 * The call to {@link #endProcess(XTraceProcess)} will create an edge from
	 * both the start context and the current X-Trace context, forming a
	 * subprocess box on the X-Trace graph.
	 * 
	 * @param agent
	 *            name of current agent
	 * @param process
	 *            name of process
	 * @return the process object created
	 */
	public static XTraceProcess startProcess(String agent, String process,
			Object... args) {
		logEvent(agent, process + " start", args);
		return new XTraceProcess(contexts.get(), agent, process);
	}

	/**
	 * Begin a "process", which will be ended with
	 * {@link #endProcess(XTraceProcess)}, by creating an event with the given
	 * agent and label strings. This function returns an XtrMetadata object that
	 * must be passed into {@link #endProcess(XTraceProcess)} or
	 * {@link #failProcess(XTraceProcess, Throwable)} to create the
	 * corresponding process-end event.
	 * 
	 * Example usage:
	 * 
	 * <pre>
	 * XtraceProcess process = XTrace.startProcess("node", "action start");
	 * ...
	 * XTrace.endProcess(process);
	 * </pre>
	 * 
	 * The call to {@link #endProcess(XTraceProcess)} will create an edge from
	 * both the start context and the current X-Trace context, forming a
	 * subprocess box on the X-Trace graph.
	 * 
	 * @param msgclass
	 * 			  arbitrary class value that can be used to control logging visibility
	 * @param agent
	 *            name of current agent
	 * @param process
	 *            name of process
	 * @return the process object created
	 */
	public static XTraceProcess startProcess(Class<?> msgclass, String agent, String process,
			Object... args) {
		logEvent(msgclass, agent, process + " start", args);
		return new XTraceProcess(msgclass, getThreadContext(), agent, process);
	}

	/**
	 * Log the end of a process started with
	 * {@link #startProcess(String, String)}. See
	 * {@link #startProcess(String, String)} for example usage.
	 * 
	 * The call to {@link #endProcess(XTraceProcess)} will create an edge from
	 * both the start context and the current X-Trace context, forming a
	 * subprocess box on the X-Trace graph.
	 * 
	 * @see XTraceContext#startProcess(String, String)
	 * @param process
	 *            return value from #startProcess(String, String)
	 */
	public static void endProcess(XTraceProcess process) {
		endProcess(process, process.name + " end");
	}

	/**
	 * Log the end of a process started with
	 * {@link #startProcess(String, String)}. See
	 * {@link #startProcess(String, String)} for example usage.
	 * 
	 * The call to {@link #endProcess(XTraceProcess, String)} will create an
	 * edge from both the start context and the current X-Trace context, forming
	 * a subprocess box on the X-Trace graph. This version of the function lets
	 * the user set a label for the end node.
	 * 
	 * @see #startProcess(String, String)
	 * @param process
	 *            return value from #startProcess(String, String)
	 * @param label
	 *            label for the end process X-Trace node
	 */
	public static void endProcess(XTraceProcess process, String label) {
		joinContext(process.startCtx);
		logEvent(process.msgclass, process.agent, label);
	}

	/**
	 * Log the end of a process started with
	 * {@link #startProcess(String, String)}. See
	 * {@link #startProcess(String, String)} for example usage.
	 * 
	 * The call to {@link #failProcess(XTraceProcess, Throwable)} will create an
	 * edge from both the start context and the current X-Trace context, forming
	 * a subprocess box on the X-Trace graph. This version of the function
	 * should be called when a process fails, to report an exception. It will
	 * add an Exception field to the X-Trace report's metadata.
	 * 
	 * @see #startProcess(String, String)
	 * @param process
	 *            return value from #startProcess(String, String)
	 * @param exception
	 *            reason for failure
	 */
	public static void failProcess(XTraceProcess process, Throwable t) {
		// Write stack trace to a string buffer
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		pw.flush();
		String exception = IoUtil.escapeNewlines(sw.toString());
		joinContext(process.startCtx);
		logEvent(process.msgclass, process.agent, process.name+" failed",
				"Exception", exception);
	}

	public static void failProcess(XTraceProcess process, String reason) {
		joinContext(process.startCtx);
		logEvent(process.msgclass, process.agent, process.name+" failed",
				"Reason", reason);
	}

	/*
	 * If there is no current valid context, start a new trace, otherwise log an
	 * event on the existing task.
	 */
	public static void startTrace(String agent, String title, String... tags) {
		if (!isValid()) {
			TaskID taskId = new TaskID(8);
			setThreadContext(new XTraceMetadata(taskId, 0L));
		}
		XTraceEvent event = createEvent(agent, "Start Trace: " + title);
		if (!isValid()) {
			event.put("Title", title);
		}
		for (String tag : tags) {
			event.put("Tag", tag);
		}
		event.sendReport();
	}

	public static void startTraceSeverity(String agent, String title,
			int severity, String... tags) {
		TaskID taskId = new TaskID(8);
		XTraceMetadata metadata = new XTraceMetadata(taskId, 0L);
		setThreadContext(metadata);
		metadata.setSeverity(severity);
		XTraceEvent event = createEvent(agent, "Start Trace: " + title);
		event.put("Title", title);
		for (String tag : tags) {
			event.put("Tag", tag);
		}
		event.sendReport();
	}

	public static int getDefaultOpIdLength() {
		return defaultOpIdLength;
	}

	public static void setDefaultOpIdLength(int defaultOpIdLength) {
		XTraceContext.defaultOpIdLength = defaultOpIdLength;
	}

	public static void readThreadContext(DataInput in) throws IOException {
		setThreadContext(XTraceMetadata.read(in));
	}

	/**
	 * Replace the current context with a new one, returning the value of the
	 * old context.
	 * 
	 * @param newContext
	 *            The context to replace the current one with.
	 * @return
	 */
	public static Collection<XTraceMetadata> switchThreadContext(XTraceMetadata newContext) {
		Collection<XTraceMetadata> oldContext = getThreadContext();
		setThreadContext(newContext);
		return oldContext;
	}

	/**
	 * Replace the current context with a new one, returning the value of the
	 * old context.
	 * 
	 * @param newContext
	 *            The context to replace the current one with.
	 * @return
	 */
	public synchronized static Collection<XTraceMetadata> switchThreadContext(
			Collection<XTraceMetadata> newContext) {
		Collection<XTraceMetadata> oldContext = getThreadContext();
		setThreadContext(newContext);
		return oldContext;
	}
	
	/**
	 * Returns true if the current thread context is equal to the provided context
	 * @param ctx the context we want to test
	 * @return true if the current thread context is equal to ctx
	 */
	public static boolean is(XTraceMetadata ctx) {
		Collection<XTraceMetadata> current = contexts.get();
		return ctx!=null && current!=null && current.size()==1 && current.contains(ctx);
	}
	
	/**
	 * Returns true if the set of current thread contexts are equal to the provided set of contexts
	 * @param ctx the contexts we want to test
	 * @return true if the current thread contexts are equal to ctx
	 */
	public static boolean is(Collection<XTraceMetadata> ctxs) {
		Collection<XTraceMetadata> current = contexts.get();
		if (ctxs==null || current==null || ctxs.size()==0 || current.size()==0) {
			return false;
		}
		for (XTraceMetadata m : ctxs) {
			if (!current.contains(m)) {
				return false;
			}
		}
		for (XTraceMetadata m : current) {
			if (!ctxs.contains(m)) {
				return false;
			}
		}
		return true;
	}
	
	private static IdentityHashMap<Object, Collection<XTraceMetadata>> attachedContexts = new IdentityHashMap<Object, Collection<XTraceMetadata>>();
	
	/**
	 * Some asynchronous design patterns would require refactoring in order to pass XTraceContexts along
	 * with return values, for example, Callables and Futures.  Rather than require refactoring in these
	 * cases, we provide the rememberObject and joinObject methods.  
	 * rememberObject will store the current XTraceContext for the object argument provided.  A subsequent
	 * call to joinObject will rejoin the context saved using rememberObject.
	 * If rememberObject is called multiple times, the current XTraceContext is merged with any previously
	 * remembered contexts
	 * @param attachTo
	 */
	public static void rememberObject(Object o) {
		Collection<XTraceMetadata> context = getThreadContext();
		if (context==null) return;
		synchronized(attachedContexts) {
			Collection<XTraceMetadata> existing = attachedContexts.get(o);
			if (existing!=null) {
				existing.addAll(context);
			} else {
				attachedContexts.put(o, context);
			}
		}
	}

	/**
	 * Some asynchronous design patterns would require refactoring in order to pass XTraceContexts along
	 * with return values, for example, Callables and Futures.  Rather than require refactoring in these
	 * cases, we provide the rememberObject and joinObject methods.  
	 * joinObject will restore any previously remembered contexts.  It will only do so once; subsequent
	 * calls to joinObject will not join any XTraceContexts unless there was an interleaved call to 
	 * rememberObject.
	 * joinObject does not clear the current thread context, instead it adds the remembered contexts to the
	 * current context.
	 * @param attachTo
	 */
	public static void joinObject(Object o) {
		Collection<XTraceMetadata> context;
		synchronized(attachedContexts) {
			context = attachedContexts.remove(o);
		}
		XTraceContext.joinContext(context);
	}
	
	/**
	 * The standard way to pass XTrace metadata between processes is to set the 
	 * XTrace environment variable.  This static initialization block will detect
	 * and resume any trace according to the environment variable that was set.
	 */
	static {
	    String xtrace_start_context = System.getenv(XTRACE_CONTEXT_ENV_VARIABLE);
	    if (xtrace_start_context!=null && !xtrace_start_context.equals("")) {
	  	  XTraceContext.setThreadContext(XTraceMetadata.createFromString(xtrace_start_context));
	    }
	}
}
