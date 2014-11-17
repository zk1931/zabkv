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

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * zabkv starts here.
 */
public final class Main {
  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  private Main() {
  }

  public static void main(String[] args) throws Exception {
    // Options for command arguments.
    Options options = new Options();

    Option port = OptionBuilder.withArgName("port")
                               .hasArg(true)
                               .isRequired(true)
                               .withDescription("port number")
                               .create("port");

    Option addr = OptionBuilder.withArgName("addr")
                               .hasArg(true)
                               .withDescription("addr (ip:port) for Zab.")
                               .create("addr");

    Option join = OptionBuilder.withArgName("join")
                               .hasArg(true)
                               .withDescription("the addr of server to join.")
                               .create("join");

    Option dir = OptionBuilder.withArgName("dir")
                              .hasArg(true)
                              .withDescription("the directory for logs.")
                              .create("dir");

    Option help = OptionBuilder.withArgName("h")
                               .hasArg(false)
                               .withLongOpt("help")
                               .withDescription("print out usages.")
                               .create("h");

    options.addOption(port)
           .addOption(addr)
           .addOption(join)
           .addOption(dir)
           .addOption(help);

    CommandLineParser parser = new BasicParser();
    CommandLine cmd;

    try {
      cmd = parser.parse(options, args);
      if (cmd.hasOption("h")) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("zabkv", options);
        return;
      }
    } catch (ParseException exp) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("zabkv", options);
      return;
    }

    Database db = new Database(cmd.getOptionValue("addr"),
                               cmd.getOptionValue("join"),
                               cmd.getOptionValue("dir"));

    Server server = new Server(Integer.parseInt(cmd.getOptionValue("port")));
    ServletHandler handler = new ServletHandler();
    server.setHandler(handler);
    // Handlers with the initialization order >= 0 get initialized on startup.
    // If you don't specify this, Zab doesn't get initialized until the first
    // request is received.
    ServletHolder holder = new ServletHolder(new RequestHandler(db));
    handler.addServletWithMapping(holder, "/*");
    server.start();
    server.join();
  }
}
