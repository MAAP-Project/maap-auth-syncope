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
import com.fasterxml.jackson.databind.node.ArrayNode
import org.apache.cxf.jaxrs.client.WebClient
import org.identityconnectors.framework.common.objects.Uid
import javax.ws.rs.core.Form

// Parameters:
// The connector sends us the following:
// client : CXF WebClient
// action: String correponding to the action ("CREATE" here)
// log: a handler to the Log facility
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
// id: The entry identifier (ConnId's "Name" atribute. (most often matches the uid))
// attributes: an Attribute Map, containg the <String> attribute name as a key
// and the <List> attribute value(s) as value.
// password: password string, clear text
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
  ObjectNode authNode = mapper.readTree(response.getEntity());
  authToken = authNode.get("access_token").textValue();


  WebClient client2 =  WebClient.create('https://gluu.biomass-maap.com');
  client2.header("Authorization", "Bearer " + authToken);
  client2.header("Content-Type", "application/json");

  ObjectNode node = mapper.createObjectNode();
  ArrayNode schemas = mapper.createArrayNode();
  schemas.add("urn:ietf:params:scim:schemas:core:2.0:User");
  node.set("schemas", schemas);
  node.set("userName", node.textNode(attributes.get("username").get(0)));
  node.set("active", node.booleanNode(true));//attributes.get("__ENABLE__").get(0)));

  firstName = attributes.get("given_name").get(0);
  lastName = attributes.get("family_name").get(0);

  ObjectNode name = mapper.createObjectNode();
  name.set("familyName", node.textNode(lastName));
  name.set("givenName", node.textNode(firstName));
  node.set("name", name);

  node.set("displayName", node.textNode(firstName + " " + lastName));

  ArrayNode emails = mapper.createArrayNode();
  ObjectNode email = mapper.createObjectNode();
  email.set("value", node.textNode(attributes.get("username")));
  email.set("primary", node.booleanNode(true));
  emails.add(email);
  node.set("emails", emails);

  String payload = mapper.writeValueAsString(node);

  client2.path("/identity/restv1/scim/v2/Users");
  def response = client2.post(payload);

  if (response.getStatus() == 201) {
    return "success!!!";
  } else {
    def details = response.getStatus().toString() + ": " + response.getStatusInfo().getReasonPhrase() + "; object: " + mapper.writeValueAsString(node);
    throw new RuntimeException("Could not post to ESA -- " + details);
  }
 
  key = node.get("key").textValue();
  break

default:
  key = id;
}

return key;
