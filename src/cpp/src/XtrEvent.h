#include "XtrMetadata.h"
#include "XtrReporter.h"
#include <string>
#include <sstream>
#include <vector>
#include <map>

using namespace std;


#ifndef _XTR_EVENT_CTX_H
#define _XTR_EVENT_CTX_H

namespace xtr {
	
class EventEdge 
{
public:
    enum EdgeDir {
        NEXT = 0,
        UP,
        DOWN,
    };

    EventEdge() : opId(), dir(NEXT), chainId(0) {};
    EventEdge(OpId _opId, EdgeDir _dir, u_int16_t _chainId = 0)
                 : opId(_opId), dir(_dir), chainId(_chainId) {};

    static const char* DirName(EdgeDir dir) {
        switch(dir) {
        case NEXT: return "next";
        case UP  : return "up";
        case DOWN: return "down";
        default: return "";
        }
    }

    OpId opId;
    EdgeDir dir;
    u_int16_t chainId;
};

class Event   
{
public:
    /** Creates a new Event. It has a new, random opId, but
     * invalid taskId.
     */
    Event();

    /** Creates a new Event from a given Metadata.  This is used to
     * start a new context with no incoming edges.  The context's taskId and opId
     * will be taken from the metadata. Also, the chainId of the context will be
     * set to the first chain id option present in the metadata, if present.
     */ 
    Event(const Metadata& xtr);
  
    /** Creates a new Event with 
     *  a default context as a model. 
     *  The context is initialized as in the default constructor,
     *  except that the extra information (info) is copied. 
     *  This constructor is useful to set things like Agent, Machine, or
     *  some other key/value pairs that would be repeated.
     */
    Event(const Event& model);

    /** Sets the taskId of the context. If the taskId is already set and
     *  is different from the one being set, an error is returned.
     *  @param taskId taskId to set
     *  @return XTR_SUCCESS if taskId was unset (invalid) or if it was set and
     *          was the same as the parameter. XTR_FAIL otherwise.
     */
    xtr_result setTaskId(const TaskId& taskId);

    /** Sets the opId of the current context to a random one.
     *  @return XTR_SUCCESS
     */
    xtr_result setRandomOpId(size_t opIdLen = 4);

    /** Sets the severity of the event context. This has the effect of
     *  changing both the severity that is propagated by getMetadata and
     *  of the reports generated by this event.
     */
    xtr_result setSeverity(u_int8_t s);

    /** Adds an edge to the context. This has several effects on the context.
     *  The default type of edge added is 'NEXT'.
     *  The first edge added does:
     *    1. sets the taskId of the event if not already set
     *    2. logically adds an edge to the report
     *    3. sets the size of the event opId
     *  A subsequent edge added with a different TaskId has no effect. It is
     *    an error. 
     *    This is added to the report as an error report.
     *  A subsequent edge added with the same ChainId:
     *    1. logically adds an edge to the report
     *  A subsequent edge added with a different ChainId:
     *    1. logically adds an edge to the report
     *    2. signifies that the event is a barrier. This will cause the
     *       termination of the incoming ChainId
     *  @param xtr
     *  @param dir
     *  @return XTR_SUCCESS if the edge is valid, <br> 
     *          XTR_FAIL if the metadata is invalid, if the taskId of the
     *          edge is different from the first edge added.
     *  @see setTimeNow()
     */
    xtr_result addEdge(const Metadata& xtr, EventEdge::EdgeDir dir = EventEdge::NEXT);

    /** Adds information to this event. This is a key-value pair of strings.
     * This information is added to the report for this event.
     * Multiple additions to the same key are allowed, and are listed in the
     * report in an arbitrary order.
     * @param key key for the info
     * @param value value for the info
     * @return XTR_SUCCESS
     */
    xtr_result addInfo(const char* key, const char* value); 

    /** Deprecated, does nothing. The timestamp is added automatically when
     *  the event is reported. */
    void addTimestamp(const char* label);


    /** Indicates that the task is forking at this event, i.e., two or more
     *  concurrent events will follow this task. fork() creates a new chainId,
     *  and returns its index. The index starts with 0, and by default the
     *  chainId of index 0 is the same as the first edge added to the task.
     *  
     *  fork() will set up state such
     *  that the next metadata retrieved from this EventCtx will be in a different
     *  chainId than the original one. 
     *  @return the index of the current ChainId after fork is called.<br>
     *  @see getMetadata()
     */
    size_t fork();

    /** Returns an Metadata object to be propagated to the subsequent
     *  events in this task. The metadata returned will have the current chainId,
     *  as set by calls to fork(). The default chainId is the chainId of the first
     *  edge added to the context, or the canonical chainId 0 otherwise.
     *  @return a reference to the metadata.
     */
    const Metadata& getMetadata();

    /** Returns an Metadata object to be propagated to the subsequent
     *  events in this task, indexed by the paramter index. This is the integer
     *  returned by fork().
     *  @return a reference to the metadata, or a reference to an invalid metadata
     *          if index is invalid.
     */
    const Metadata& getMetadata(size_t index);
    
    /** Returns a string representation of the report for this event.
     *  This is a fully formated report that can be sent to the reporting daemon,
     *  and conforms to the X-Trace report specification version 1.0.
     */
    string getReport();

    /** Directly call the Reporter report method.
     *  @return XTR_FAIL if the Reporter is not initialized,
     *                   or if the event's priority is not sufficient
     *                   to be reported by the Reporter.
     *          XTR_SUCCESS otherwise.
     */
    xtr_result sendReport();
    
    /** TODO: this returns a XtrReport object, which can be passed to a
     *  Reporter object for reporting. Not implemented yet.
     */
    //XtrReport getReport()        
private:
    /** Sets the timestamp of the event. This is called the first
     *  time that getReport is called.
     */
    void setTimestamp();
    Metadata my_xtr;                 //this context's taskId, optionId, options
    
    vector<u_int16_t> out_chain_ids;    //this context's outgoing chainIds
    size_t chain_id_index;
    
    vector<EventEdge> in_edges;      //incoming edges
    string info;                        //append-only string for holding key-value pairs

    struct timeval timestamp;
    bool timeset;
};

}; //namespace xtr

#if 0
edge opId, chain, type
end-chain
fork (?)
timestamp: time, value

This is a multiset...
class XtrReport()
{
    put(key, value)
    get(key) 
    del(key)
    returns a fully formatted X-Trace report
    toString
    X-Trace Report 1.0
    Key: Value
    Key: Value
    Key: Value
}

/* Talks to the local daemon */
class Reporter()
{
    Reporter();
    virtual send(XtrReport& r);
}
#endif

#endif
