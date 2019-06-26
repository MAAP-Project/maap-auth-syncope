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
import com.fasterxml.jackson.databind.node.ArrayNode
import javax.ws.rs.core.Form

def getAttributeValue(attributes, attributeName) {

  for (int j = 0; j < attributes.size(); j++) {
    if(attributes[j].schema.textValue() == attributeName) {
      return attributes[j].values[0].textValue();
    }
  }

  return "";
}

// Parameters:
// The connector sends us the following:
// client : CXF WebClient
//
// action: String correponding to the action (UPDATE/ADD_ATTRIBUTE_VALUES/REMOVE_ATTRIBUTE_VALUES)
//   - UPDATE : For each input attribute, replace all of the current values of that attribute
//     in the target object with the values of that attribute.
//   - ADD_ATTRIBUTE_VALUES: For each attribute that the input set contains, add to the current values
//     of that attribute in the target object all of the values of that attribute in the input set.
//   - REMOVE_ATTRIBUTE_VALUES: For each attribute that the input set contains, remove from the current values
//     of that attribute in the target object any value that matches one of the values of the attribute from the input set.

// log: a handler to the Log facility
//
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
//
// uid: a String representing the entry uid
//
// attributes: an Attribute Map, containg the <String> attribute name as a key
// and the <List> attribute value(s) as value.
//
// password: password string, clear text (only for UPDATE)
//
// options: a handler to the OperationOptions Map

log.info("Entering " + action + " Script");

WebClient webClient = client;
ObjectMapper mapper = new ObjectMapper();

assert uid != null

switch (action) {
case "UPDATE":
  switch (objectClass) {  
  case "__ACCOUNT__":

//  if(attributes.get("username").get(0) == "nasatestuser4@nasa.org") {
    throw new RuntimeException("nasa test user -- " + mapper.writeValueAsString(attributes));
    break;
//  }

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

  break;

  default:
    break
  }

  return uid;
  break

case "ADD_ATTRIBUTE_VALUES":
  break


default:
  break
}
