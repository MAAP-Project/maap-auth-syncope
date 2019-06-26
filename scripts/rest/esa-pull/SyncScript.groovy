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
import com.fasterxml.jackson.databind.node.ArrayNode
import java.util.Date;
import javax.ws.rs.core.Response
import org.apache.cxf.jaxrs.client.WebClient
import org.identityconnectors.common.security.GuardedString
import javax.ws.rs.core.Form
import com.fasterxml.jackson.databind.node.ObjectNode

// Parameters:
// The connector sends the following:
// client : CXF WebClient
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
// action: a string describing the action ("SYNC" or "GET_LATEST_SYNC_TOKEN" here)
// log: a handler to the Log facility
// options: a handler to the OperationOptions Map (null if action = "GET_LATEST_SYNC_TOKEN")
// token: a handler to an Object representing the sync token (null if action = "GET_LATEST_SYNC_TOKEN")
//
//
// Returns:
// if action = "GET_LATEST_SYNC_TOKEN", it must return an object representing the last known
// sync token for the corresponding ObjectClass
// 
// if action = "SYNC":
// A list of Maps . Each map describing one update:
// Map should look like the following:
//
// [
// "token": <Object> token object (could be Integer, Date, String) , [!! could be null]
// "operation":<String> ("CREATE_OR_UPDATE"|"DELETE")  will always default to CREATE_OR_DELETE ,
// "uid":<String> uid  (uid of the entry) ,
// "previousUid":<String> prevuid (This is for rename ops) ,
// "password":<String> password (optional... allows to pass clear text password if needed),
// "attributes":Map<String,List> of attributes name/values
// ]

def getEmail(e) {
  return e.contains("@") ? e : e.replaceAll(' ', '') + "@esa.org" 
}

def buildConnectorObject(node) {
  return [
    __UID__:node.get("id").textValue(), 
    __NAME__:node.get("id").textValue(),
    key:node.get("id").textValue(),
    username:node.get("userName").textValue(),
    password: null,
    firstName:node.get("name").get("givenName").textValue(),
    surname:node.get("name").get("familyName").textValue(),
    email:getEmail(node.get("userName").textValue())
  ];
}

log.info("Entering " + action + " Script");

WebClient webClient = client;
ObjectMapper mapper = new ObjectMapper();

if (action.equalsIgnoreCase("GET_LATEST_SYNC_TOKEN")) {
  switch (objectClass) {
  case "__ACCOUNT__":
    latestToken = new Date().getTime();
    break;
    
  default:
    latestToken = null;
  }

  return latestToken;
} else if (action.equalsIgnoreCase("SYNC")) {
  def result = [];

  switch (objectClass) {
  case "__ACCOUNT__":
    
    WebClient clientAuth = WebClient.create('https://gluu.biomass-maap.com');
    String authorizationHeader = "Basic ${ESA_GLUU_TOKEN}"

    clientAuth.header("Authorization", authorizationHeader);
    clientAuth.header("Content-Type", "application/x-www-form-urlencoded");

    clientAuth.path("/oxauth/restv1/token").accept("application/json");
    response = clientAuth.post(new Form().param("grant_type", "client_credentials"));
    ObjectNode node = mapper.readTree(response.getEntity());
    authToken = node.get("access_token").textValue();


    WebClient client2 =  WebClient.create('https://gluu.biomass-maap.com');

    client2.header("Authorization", "Bearer " + authToken);
    client2.header("Content-Type", "application/scim+json");

    client2.path("/identity/restv1/scim/v2/Users");
    response2 = client2.get();    
    ObjectNode node2 = mapper.readTree(response2.getEntity());
    ArrayNode users = node2.get("Resources")

    for (i = 1; i < users.size(); i++) {
      result.add([
          operation:"CREATE_OR_UPDATE",
          uid:users.get(i).get("id").textValue(),
          token:new Date().getTime(),
          attributes:buildConnectorObject(users.get(i))
        ]);
    }
    break;
  }
  
  log.ok("Sync script: found " + result.size() + " events to sync");
  return result;
} else {
  log.error("Sync script: action '" + action + "' is not implemented in this script");
  return null;
}
