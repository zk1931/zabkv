/**
 * Licensed to the zk9131 under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
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

package com.github.zk1931.zabkv;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;
import com.github.zk1931.jzab.PendingRequests;
import com.github.zk1931.jzab.PendingRequests.Tuple;
import com.github.zk1931.jzab.StateMachine;
import com.github.zk1931.jzab.Zab;
import com.github.zk1931.jzab.ZabConfig;
import com.github.zk1931.jzab.ZabException;
import com.github.zk1931.jzab.Zxid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * StateMachine of the ZabKV.
 */
public final class Database implements StateMachine {
  private static final Logger LOG = LoggerFactory.getLogger(Database.class);

  private Zab zab;

  private String serverId;

  private final ZabConfig config = new ZabConfig();

  private ConcurrentSkipListMap<String, byte[]> kvstore =
    new ConcurrentSkipListMap<>();

  public Database(String serverId, String joinPeer, String logDir) {
    try {
      this.serverId = serverId;
      if (this.serverId != null && joinPeer == null) {
        // It's the first server in cluster, joins itself.
        joinPeer = this.serverId;
      }
      if (this.serverId != null && logDir == null) {
        // If user doesn't specify log directory, default one is
        // serverId in current directory.
        logDir = this.serverId;
      }
      config.setLogDir(logDir);
      if (joinPeer != null) {
        zab = new Zab(this, config, this.serverId, joinPeer);
      } else {
        // Recovers from log directory.
        zab = new Zab(this, config);
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

  public String getAll() throws IOException {
    GsonBuilder builder = new GsonBuilder();
    Gson gson = builder.create();
    return gson.toJson(kvstore);
  }

  public boolean add(JsonPutCommand command, AsyncContext context) {
    try {
      ByteBuffer bb = command.toByteBuffer();
      LOG.debug("Sending a message: {}", bb);
      try {
        zab.send(command.toByteBuffer(), context);
      } catch (ZabException ex) {
        return false;
      }
    } catch (IOException ex) {
      return false;
    }
    return true;
  }

  @Override
  public ByteBuffer preprocess(Zxid zxid, ByteBuffer message) {
    LOG.debug("Preprocessing a message: {}", message);
    return message;
  }

  @Override
  public void deliver(Zxid zxid, ByteBuffer stateUpdate, String clientId,
                      Object ctx) {
    LOG.debug("Received a message: {}", stateUpdate);
    JsonPutCommand command = JsonPutCommand.fromByteBuffer(stateUpdate);
    command.execute(this);
    if (clientId == null || !clientId.equals(this.serverId)) {
      return;
    }
    AsyncContext context = (AsyncContext)ctx;
    if (context == null) {
      // This request is sent from other instance.
      return;
    }
    HttpServletResponse response =
      (HttpServletResponse)(context.getResponse());
    response.setContentType("text/html");
    response.setStatus(HttpServletResponse.SC_OK);
    context.complete();
  }

  @Override
  public void flushed(ByteBuffer request, Object ctx) {
  }

  @Override
  public void save(OutputStream os) {
    // No support for snapshot yet.
  }

  @Override
  public void restore(InputStream is) {
    // No support for snapshot yet.
  }

  @Override
  public void snapshotDone(String filePath, Object ctx) {
  }

  @Override
  public void removed(String peerId, Object ctx) {
  }

  @Override
  public void recovering(PendingRequests pendingRequests) {
    LOG.info("Recovering...");
    // Returns error for all pending requests.
    for (Tuple tp : pendingRequests.pendingSends) {
      AsyncContext context = (AsyncContext)tp.param;
      HttpServletResponse response =
        (HttpServletResponse)(context.getResponse());
      response.setContentType("text/html");
      response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
      context.complete();
    }
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
