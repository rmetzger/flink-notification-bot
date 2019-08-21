/*
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

package com.github.wuchong.flink.notification.bot.travis;

import com.github.wuchong.flink.notification.bot.email.Email;
import com.github.wuchong.flink.notification.bot.email.EmailGenerator;
import com.github.wuchong.flink.notification.bot.email.EmailSender;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class TravisPostHandler implements HttpHandler {

    private static final Logger LOG = LoggerFactory.getLogger(TravisPostHandler.class);

    @SuppressWarnings("unchecked")
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        Headers headers = exchange.getRequestHeaders();
        LOG.info("SIGNATURE headers: " + headers.get("SIGNATURE"));
        LOG.info("HTTP_SIGNATURE headers: " + headers.get("HTTP_SIGNATURE"));
//        String signature = headers.getFirst("SIGNATURE");
//        byte[] byteSignature = Base64.getDecoder().decode(signature);
        // parse request
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        String paylaod = br.readLine();
        try {
            Map<String, String> builds = BuildResult.parse(paylaod);
            String account = builds.get("account");
            String repository = builds.get("repository");
            if ("apache".equals(account) && "flink".equals(repository)) {
                Email email = EmailGenerator.createEmail(builds);
                EmailSender.send(email);
            } else {
                LOG.info("Ignore builds notification from " + account + "/" + repository);
                LOG.info("Builds data: " + builds.toString());
            }
        } catch (Exception e) {
            LOG.warn("Corrupt payload: " + paylaod, e);
        }
    }
}
