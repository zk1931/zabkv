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

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingQueue;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;
import org.apache.zab.SingleNodeZab;
import org.apache.zab.StateMachine;
import org.apache.zab.Zxid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * state machine.
 */
public final class Database implements StateMachine {
  private static final Logger LOG = LoggerFactory.getLogger(Database.class);

  private SingleNodeZab zab;

  private ConcurrentSkipListMap<String, byte[]> kvstore =
    new ConcurrentSkipListMap<>();

  private LinkedBlockingQueue<AsyncContext> pending =
    new LinkedBlockingQueue<>();

  public Database() {
    try {
      zab = new SingleNodeZab(this, new Properties());
    } catch (IOException ex) {
      throw new RuntimeException();
    }
  }

  public byte[] get(String key) {
    return (byte[])kvstore.get((Object)key);
  }

  public byte[] put(String key, byte[] value) {
    return kvstore.put(key, value);
  }

  /**
   * Add a request to this database.
   *
   * This method must be synchronized to ensure that the requests are sent to
   * Zab in the same order they get enqueued to the pending queue.
   */
  public synchronized boolean add(PutCommand command, AsyncContext context) {
    if (!pending.add(context)) {
      return false;
    }
    try {
      zab.send(command.toByteBuffer());
    } catch (IOException ex) {
      throw new RuntimeException();
    }
    return true;
  }

  public ByteBuffer preprocess(Zxid zxid, ByteBuffer message) {
    return message;
  }

  public void deliver(Zxid zxid, ByteBuffer stateUpdate) {
    PutCommand command = PutCommand.fromByteBuffer(stateUpdate);
    LOG.debug("Delivering a command: {}", command);
    command.execute(this);
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

  public void getState(OutputStream os) {
    throw new UnsupportedOperationException();
  }

  public void setState(InputStream is) {
    throw new UnsupportedOperationException();
  }
}
