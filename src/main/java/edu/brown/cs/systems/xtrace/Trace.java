package edu.brown.cs.systems.xtrace;

import edu.brown.cs.systems.xtrace.Metadata.XTraceMetadata.Builder;
import edu.brown.cs.systems.xtrace.Metadata.XTraceMetadataOrBuilder;

/**
 * The basic Trace class. Adds additional behaviours on top of those defined in
 * Context.
 * 
 * @author Jonathan Mace
 */
class Trace extends Context.Manager {

  public void join(byte[] other) {
    join(Context.parse(other));
  }

  public void join(Context other) {
    if (other == null)
      return;
    else if (!exists())
      set(other);
    else
      mergeOtherContextIntoThis(other);
  }

  public void clear() {
    set((Context) null);
  }

  private void mergeOtherContextIntoThis(Context other) {
    // Break out early if current is the same as metadata
    XTraceMetadataOrBuilder current = observe();
    XTraceMetadataOrBuilder provided = other.observe();
    if (current == provided) {
      return;
    }

    // Do nothing if provided metadata has no parents
    if (provided.getParentEventIDCount() == 0) {
      return;
    }

    // Do a set if the current metadata has no parents
    if (current.getParentEventIDCount() == 0) {
      set(other);
    }

    // Check to see whether the parents are different
    int numToAdd = 0;
    long[] toAdd = new long[provided.getParentEventIDCount()];
    parents: for (int i = 0; i < toAdd.length; i++) {
      long parenti = provided.getParentEventID(i);
      for (int j = 0; j < current.getParentEventIDCount(); j++) {
        if (current.getParentEventID(j) == parenti) {
          continue parents;
        }
      }
      toAdd[numToAdd++] = parenti;
    }

    // Add new parent IDs if there are any to add
    if (numToAdd > 0) {
      Builder builder = modify();
      for (int i = 0; i < numToAdd; i++)
        builder.addParentEventID(toAdd[i]);
    }
  }

}
