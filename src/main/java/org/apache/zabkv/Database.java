/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zabkv;

//import java.io.ByteArrayOutputStream;
//import java.io.DataOutputStream;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingQueue;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;
import org.apache.zab.QuorumZab;
import org.apache.zab.StateMachine;
import org.apache.zab.Zxid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * state machine.
 */
public final class Database implements StateMachine {
  private static final Logger LOG = LoggerFactory.getLogger(Database.class);

  private QuorumZab zab;

  private String serverId;

  private ConcurrentSkipListMap<String, byte[]> kvstore =
    new ConcurrentSkipListMap<>();

  private LinkedBlockingQueue<AsyncContext> pending =
    new LinkedBlockingQueue<>();

  public Database() {
    try {
      String selfId = System.getProperty("serverId");
      String logDir = System.getProperty("logdir");
      String joinPeer = System.getProperty("join");
      if (selfId != null && joinPeer == null) {
        joinPeer = selfId;
      }
      Properties prop = new Properties();
      if (selfId != null) {
        prop.setProperty("serverId", selfId);
        prop.setProperty("logdir", selfId);
      }
      if (joinPeer != null) {
        prop.setProperty("joinPeer", joinPeer);
      }
      if (logDir != null) {
        prop.setProperty("logdir", logDir);
      }
      prop.setProperty("snapshot_threshold_bytes",
                       System.getProperty("snapshot", "-1"));
      if (joinPeer != null) {
        zab = new QuorumZab(this, prop, joinPeer);
      } else {
        zab = new QuorumZab(this, prop);
      }
      this.serverId = zab.getServerId();
    } catch (Exception ex) {
      LOG.error("Caught exception : ", ex);
      throw new RuntimeException();
    }
  }

  public String get(String key) {
    GsonBuilder builder = new GsonBuilder();
    Gson gson = builder.create();
    Map<String, Object> map = new HashMap<>();
    map.put(key, (Object)kvstore.get(key));
    return gson.toJson(map);
  }

  public void put(Map<String, byte[]> updates) {
    kvstore.putAll(updates);
  }

  public void remove(String peerId) {
    this.zab.remove(peerId);
  }

  public String getAll() throws IOException {
    GsonBuilder builder = new GsonBuilder();
    Gson gson = builder.create();
    return gson.toJson(kvstore);
  }

  /**
   * Add a request to this database.
   *
   * This method must be synchronized to ensure that the requests are sent to
   * Zab in the same order they get enqueued to the pending queue.
   */
  public synchronized boolean add(JsonPutCommand command,
                                  AsyncContext context) {
    if (!pending.add(context)) {
      return false;
    }
    try {
      ByteBuffer bb = command.toByteBuffer();
      LOG.debug("Sending a message: {}", bb);
      zab.send(command.toByteBuffer());
    } catch (IOException ex) {
      throw new RuntimeException();
    }
    return true;
  }

  @Override
  public ByteBuffer preprocess(Zxid zxid, ByteBuffer message) {
    LOG.debug("Preprocessing a message: {}", message);
    return message;
  }

  @Override
  public void deliver(Zxid zxid, ByteBuffer stateUpdate, String clientId) {
    LOG.debug("Received a message: {}", stateUpdate);
    JsonPutCommand command = JsonPutCommand.fromByteBuffer(stateUpdate);
    LOG.debug("Delivering a command: {} {}", zxid, command);
    command.execute(this);

    if (clientId == null || !clientId.equals(this.serverId)) {
      return;
    }

    AsyncContext context = pending.poll();
    if (context == null) {
      // There is no pending HTTP request to respond to.
      return;
    }
    HttpServletResponse response =
      (HttpServletResponse)(context.getResponse());
    response.setContentType("text/html");
    response.setStatus(HttpServletResponse.SC_OK);
    context.complete();
  }

  @Override
  public void save(OutputStream os) {
    LOG.debug("SAVE is called.");
    try {
      ObjectOutputStream out = new ObjectOutputStream(os);
      out.writeObject(kvstore);
    } catch (IOException e) {
      LOG.error("Caught exception", e);
    }
  }

  @Override
  public void restore(InputStream is) {
    LOG.debug("RESTORE is called.");
    try {
      ObjectInputStream oin = new ObjectInputStream(is);
      kvstore = (ConcurrentSkipListMap<String, byte[]>)oin.readObject();
      LOG.debug("The size of map after recovery from snapshot file is {}",
                kvstore.size());
    } catch (Exception e) {
      LOG.error("Caught exception", e);
    }
  }

  @Override
  public void recovering() {
    // If it's LOOKING state. Reply all pending request with 503 clear
    // pending queue.
    LOG.info("RECOVERING");
    Iterator<AsyncContext> iter = pending.iterator();
    while (iter.hasNext()) {
      AsyncContext context = iter.next();
      HttpServletResponse response =
        (HttpServletResponse)(context.getResponse());
      response.setContentType("text/html");
      response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
      context.complete();
    }
    pending.clear();
  }

  @Override
  public void leading(Set<String> activeFollowers, Set<String> clusterMembers) {
    LOG.info("LEADING with active followers : ");
    for (String peer : activeFollowers) {
      LOG.info(" -- {}", peer);
    }
    LOG.info("Cluster configuration change : ", clusterMembers.size());
    for (String peer : clusterMembers) {
      LOG.info(" -- {}", peer);
    }
  }

  @Override
  public void following(String leader, Set<String> clusterMembers) {
    LOG.info("FOLLOWING {}", leader);
    LOG.info("Cluster configuration change : ", clusterMembers.size());
    for (String peer : clusterMembers) {
      LOG.info(" -- {}", peer);
    }
  }
}
