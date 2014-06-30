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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command to put an entry to the key-value store.
 */
public final class PutCommand implements Serializable {
  private static final Logger LOG = LoggerFactory.getLogger(PutCommand.class);

  private final String key;

  private final byte[] value;

  public PutCommand(String key, byte[] value) {
    this.key = key;
    this.value = value;
  }

  public void execute(Database db) {
    db.put(key, value);
  }

  public ByteBuffer toByteBuffer() throws IOException {
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
         ObjectOutputStream oos = new ObjectOutputStream(bos)) {
      oos.writeObject(this);
      oos.close();
      return ByteBuffer.wrap(bos.toByteArray());
    }
  }

  public static PutCommand fromByteBuffer(ByteBuffer bb) {
    byte[] bytes = new byte[bb.remaining()];
    bb.get(bytes);
    try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
         ObjectInputStream ois = new ObjectInputStream(bis)) {
      return (PutCommand)ois.readObject();
    } catch (ClassNotFoundException|IOException ex) {
      LOG.error("Failed to deserialize: {}", bb, ex);
      throw new RuntimeException("Failed to deserialize ByteBuffer");
    }
  }

  public String toString() {
    return String.format("PUT key='%s', value='%s'", key, new String(value));
  }
}
