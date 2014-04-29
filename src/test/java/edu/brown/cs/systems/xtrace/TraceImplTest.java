package edu.brown.cs.systems.xtrace;

import java.util.Arrays;
import java.util.Random;

import junit.framework.TestCase;

import org.junit.Test;

import com.google.protobuf.InvalidProtocolBufferException;

import edu.brown.cs.systems.xtrace.Metadata.XTraceMetadata;
import edu.brown.cs.systems.xtrace.Metadata.XTraceMetadataOrBuilder;
import edu.brown.cs.systems.xtrace.Metadata.XTraceMetadata.Builder;

public class TraceImplTest extends TestCase {
  
  public static final Random random = new Random();
  public static final byte[] nullbytes = null;
  public static final Context nullcontext = null;
  
  public static XTraceMetadata build(byte[] bytes) throws InvalidProtocolBufferException {
    return Metadata.XTraceMetadata.parseFrom(bytes);
  }
  
  public static Builder newBuilder(Long taskid, Integer tenantClass, long... parentIds) {
    Builder builder = Metadata.XTraceMetadata.newBuilder();
    if (taskid!=null)
      builder.setTaskID(taskid);
    if (tenantClass!=null)
      builder.setTenantClass(tenantClass);
    for (int i = 0; i < parentIds.length; i++) {
      builder.addParentEventID(parentIds[i]);
    }
    return builder;
  }
  
  public static byte[] newBytes(Long taskid, Integer tenantClass, long... parentIds) {
    return newBuilder(taskid, tenantClass, parentIds).build().toByteArray();
  }
  
  public static byte[] randomTenantID() {
    return newBytes(null, random.nextInt());
  }
  
  public static byte[] empty() {
    return newBuilder(null, null).build().toByteArray();
  }
  
  public static byte[] randomTaskID() {
    return newBytes(random.nextLong(), null);
  }
  
  public static byte[] randomTaskIDAndTenant() {
    return newBytes(random.nextLong(), random.nextInt());
  }
  
  public static byte[] randomTaskIDAndTenantAndParents(int numparents) {
    Builder builder = newBuilder(random.nextLong(), random.nextInt());
    for (int i = 0; i < numparents; i++) {
      builder.addParentEventID(random.nextLong());
    }
    return builder.build().toByteArray();
  }
  
  public byte[] taskIDwithRandomTenant(long taskid) {
    return newBytes(taskid, random.nextInt());
  }
  
  public byte[] addParents(byte[] proto, int numparents) throws InvalidProtocolBufferException {
    Builder builder = Metadata.XTraceMetadata.parseFrom(proto).toBuilder();
    for (int i = 0; i < numparents; i++) {
      builder.addParentEventID(random.nextLong());
    }
    return builder.build().toByteArray();
  }
  
  public byte[] duplicate(byte[] proto) {
    return Arrays.copyOf(proto, proto.length);
  }
  
  @Test
  public void testSetSimple() {
    Trace xtrace = new Trace();
    byte[] xmd1 = randomTaskID();
    byte[] xmd2 = randomTaskID();
    
    xtrace.set(xmd1);
    assertTrue(Arrays.equals(xmd1, xtrace.get().bytes()));
    assertFalse(Arrays.equals(xmd1, xmd2));
    assertFalse(Arrays.equals(xmd2, xtrace.get().bytes()));
    
    xtrace.set(xmd2);
    assertTrue(Arrays.equals(xmd2, xtrace.get().bytes()));
    assertFalse(Arrays.equals(xmd1, xmd2));
    assertFalse(Arrays.equals(xmd1, xtrace.get().bytes()));
  }
  
  @Test
  public void testNulls() {
    Trace xtrace = new Trace();
    xtrace.set(nullbytes);
    assertNull(xtrace.get());
    
    xtrace.join(nullbytes);
    assertNull(xtrace.get());
    
    xtrace.join(nullcontext);
    assertNull(xtrace.get());
    
    byte[] xmd = randomTaskID();
    xtrace.set(xmd);
    assertTrue(Arrays.equals(xmd, xtrace.get().bytes()));
    
    xtrace.join(nullbytes);
    assertTrue(Arrays.equals(xmd, xtrace.get().bytes()));
    
    xtrace.join(nullcontext);
    assertTrue(Arrays.equals(xmd, xtrace.get().bytes()));
    
    xtrace.set(nullbytes);
    assertNull(xtrace.get());
    
  }

  /**
   * Tests to see whether objects are reused and not modified
   * when appropriate
   * @throws InvalidProtocolBufferException 
   */
  @Test
  public void testAppropriateObjectCreation() throws InvalidProtocolBufferException {
    assertEquals(null, new Trace().get());
    
    Trace xtrace = new Trace();
    byte[] xmd = randomTaskID();
    xtrace.set(xmd);
    assertEquals(xtrace.get(), xtrace.get());

    // Join should change nothing
    xtrace.join(nullbytes);
    Context ctx = xtrace.get();
    assertEquals(ctx, xtrace.get());
    
    xtrace.join(nullcontext);
    assertEquals(ctx, xtrace.get());
    
    // With no parent ids, join of an actual xmd should do nothing too
    xtrace.join(randomTaskID());
    assertEquals(ctx, xtrace.get());
    
    xtrace.join(Context.parse(randomTaskID()));
    assertEquals(ctx, xtrace.get());
    
    xtrace.join(randomTaskIDAndTenant());
    assertEquals(ctx, xtrace.get());
    
    xtrace.join(Context.parse(randomTaskIDAndTenant()));
    assertEquals(ctx, xtrace.get());
    
    // Now for the fun stuff.  If parents exist but aren't changed, nothing should change
    byte[] xmd1 = randomTaskIDAndTenantAndParents(1);
    assertEquals(1, build(xmd1).getParentEventIDCount());
    
    byte[] xmd2 = duplicate(xmd1);
    assertNotSame(xmd1, xmd2);
    assertTrue(Arrays.equals(xmd1, xmd2));
    assertEquals(1, build(xmd2).getParentEventIDCount());
    
    // If parents exist but ctx is not yet immutable, it should be updated
    xtrace = new Trace();
    xtrace.set(xmd1);
    XTraceMetadataOrBuilder builder = xtrace.observe();
    assertEquals(builder, xtrace.observe());
    assertEquals(builder, xtrace.get().observe());
    assertEquals(builder, xtrace.observe());
    assertEquals(builder, xtrace.get().observe());
    xtrace.set(xtrace.get());
    assertEquals(builder, xtrace.observe());
    assertEquals(builder, xtrace.get().observe());
    assertEquals(builder, xtrace.observe());
    assertEquals(builder, xtrace.get().observe());
    
    // They're the same, so this shouldn't have any effect
    xtrace.join(xmd2);
    assertEquals(builder, xtrace.observe());
    assertEquals(builder, xtrace.get().observe());
    assertEquals(builder, xtrace.observe());
    assertEquals(builder, xtrace.get().observe());
    
    byte[] xmd3 = xtrace.bytes();
    assertTrue(Arrays.equals(xmd1, xmd3));
    
    // Only if the ctx is immutable and parents are updated should a new one be allocated
    xtrace = new Trace();
    xmd1 = randomTaskIDAndTenantAndParents(1);
    xtrace.set(xmd1);
    builder = xtrace.observe();
    
    xmd2 = randomTaskIDAndTenantAndParents(1);
    assertFalse(Arrays.equals(xmd1, xmd2));
    
    ctx = xtrace.get();
    assertEquals(builder, xtrace.observe());
    assertEquals(builder, xtrace.get().observe());
    assertEquals(ctx.observe(), xtrace.observe());
    assertEquals(ctx.observe(), xtrace.get().observe());
    
    xtrace.join(xmd2);
    Context ctx2 = xtrace.get();
    assertFalse(ctx==ctx2);

    assertTrue(Arrays.equals(ctx.bytes(), xmd1));
    assertFalse(Arrays.equals(ctx.bytes(), xmd2));
    assertEquals(1, ctx.observe().getParentEventIDCount());
    assertFalse(Arrays.equals(ctx2.bytes(), xmd1));
    assertFalse(Arrays.equals(ctx2.bytes(), xmd2));
    assertEquals(2, ctx2.observe().getParentEventIDCount());

    assertFalse(builder==xtrace.observe());
    assertFalse(builder==xtrace.get().observe());
    assertFalse(builder==xtrace.observe());
    assertFalse(builder==xtrace.get().observe());
  }
  
  @Test
  public void testMetadataUpdateNonimmutable() {
    Trace xtrace = new Trace();
    byte[] xmd = randomTaskIDAndTenantAndParents(1);
    xtrace.set(xmd);
    XTraceMetadataOrBuilder builder = xtrace.observe();
    
    long prevParentEventID = builder.getParentEventID(0);
    long nextParentEventID = random.nextLong();
    
    xtrace.modify().clearParentEventID().addParentEventID(nextParentEventID);
    assertEquals(builder, xtrace.observe());
    assertEquals(1, xtrace.observe().getParentEventIDCount());
    assertEquals(nextParentEventID, xtrace.observe().getParentEventID(0));
    assertFalse(xtrace.observe().getParentEventID(0)==prevParentEventID);
  }
  
  @Test
  public void testMetadataUpdateImmutable() {
    Trace xtrace = new Trace();
    byte[] xmd = randomTaskIDAndTenantAndParents(1);
    xtrace.set(xmd);
    xtrace.get();
    XTraceMetadataOrBuilder builder = xtrace.observe();
    
    long prevParentEventID = builder.getParentEventID(0);
    long nextParentEventID = random.nextLong();
    
    xtrace.modify().clearParentEventID().addParentEventID(nextParentEventID);
    assertFalse(xtrace.observe()==builder);
    assertEquals(1, xtrace.observe().getParentEventIDCount());
    assertEquals(nextParentEventID, xtrace.observe().getParentEventID(0));
    assertFalse(xtrace.observe().getParentEventID(0)==prevParentEventID);
  }
  
  @Test
  public void testStringRepr() {
    byte[] xmd = randomTaskIDAndTenantAndParents(3);
    Context ctx = Context.parse(xmd);
    assertTrue(Arrays.equals(xmd, ctx.bytes()));
    String strrep = ctx.base64();
    assertNotNull(strrep);
    Context ctx2 = Context.parse(strrep);
    assertNotNull(ctx2);
    assertTrue(ctx!=ctx2);
    assertTrue(Arrays.equals(xmd, ctx2.bytes()));
    assertTrue(Arrays.equals(ctx.bytes(), ctx2.bytes()));
    assertTrue(ctx.base64().equals(ctx2.base64()));
    
    Trace xtrace = new Trace();
    assertNull(xtrace.base64());
    ctx = Context.parse((String)null);
    assertNull(ctx);
    ctx = Context.parse("");
    assertFalse(ctx.observe().hasTaskID());
    assertFalse(ctx.observe().hasTenantClass());
    assertEquals(0, ctx.observe().getParentEventIDCount());
  }
  

}
