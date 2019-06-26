/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.apache.cxf.jaxrs.client.WebClient
import org.identityconnectors.framework.common.objects.Uid

// Parameters:
// The connector sends us the following:
// client : CXF WebClient
// action: String correponding to the action ("AUTHENTICATE" here)
// log: a handler to the Log facility
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
// username: username
// password: password string, clear text or GuardedString depending on configuration
// options: a handler to the OperationOptions Map

log.info("Entering " + action + " Script");

WebClient webClient = client;
ObjectMapper mapper = new ObjectMapper();

String key;

switch (objectClass) {  
case "__ACCOUNT__":

    WebClient clientAuth = WebClient.create('https://gluu.biomass-maap.com');
    String authorizationHeader = "Basic ${ESA_GLUU_TOKEN}"

    clientAuth.header("Authorization", authorizationHeader);
    clientAuth.header("Content-Type", "application/x-www-form-urlencoded");

    clientAuth.path("/oxauth/restv1/token").accept("application/json");
    response = clientAuth.post(new Form().param("grant_type", "client_credentials"));

  if (response.getStatus() == 200) {
    ObjectNode node = mapper.readTree(response.getEntity());
    return node.get("access_token").textValue()
  } else {
    throw new RuntimeException("Could not authenticate " + username);
  }
  break

default:
  throw new RuntimeException();
}
