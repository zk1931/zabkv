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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test client.
 */
public class TestClient {

  private static final Logger LOG = LoggerFactory.getLogger(TestClient.class);

  private String addr;

  private HttpClient client = new HttpClient();

  public TestClient() throws Exception {
    this.client.start();
  }

  public void put(String uri, String key, String value)
      throws InterruptedException, TimeoutException, ExecutionException {
    String json = String.format("{'%s':'%s'}", key, value);
    Request request = client.newRequest(uri);
    request.scheme("http");
    request.method(HttpMethod.PUT);
    request.content(new StringContentProvider(json));
    request.send();
  }

  public void stop() throws Exception {
    this.client.stop();
  }

  public static void main(String[] args) throws Exception {
    // Gets the list of zabkv servers.
    String strServers = System.getProperty("servers", "localhost:8080");
    int numWrites = Integer.parseInt(System.getProperty("numWrites", "100"));
    String[] servers = strServers.split(";");
    TestClient client = new TestClient();
    for (int i = 0; i < numWrites; ++i) {
      for (String server : servers) {
        try {
          client.put("http://" + server, server + "_key_" + i, "" + i);
        } catch (Exception ex) {
          LOG.info("Caught exception while sending to {}", server);
        }
      }
    }
    client.stop();
    LOG.info("Test ends.");
  }
}
