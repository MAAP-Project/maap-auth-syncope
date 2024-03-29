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
import com.fasterxml.jackson.databind.node.ObjectNode
import javax.ws.rs.core.Response
import org.apache.cxf.jaxrs.client.WebClient
import org.identityconnectors.common.security.GuardedString
import org.identityconnectors.framework.common.objects.OperationOptions;

// Parameters:
// The connector sends the following:
// client : CXF WebClient
// objectClass: a String describing the Object class (__ACCOUNT__ / __GROUP__ / other)
// action: a string describing the action ("SEARCH" here)
// log: a handler to the Log facility
// options: a handler to the OperationOptions Map
// query: a handler to the Query Map
//
// The Query map describes the filter used (via FIQL's ConditionType):
//
// query = [ operation: "EQUALS", left: attribute, right: "value" ]
// query = [ operation: "GREATER_THAN", left: attribute, right: "value" ]
// query = [ operation: "GREATER_OR_EQUALS", left: attribute, right: "value" ]
// query = [ operation: "LESS_THAN", left: attribute, right: "value" ]
// query = [ operation: "LESS_OR_EQUALS", left: attribute, right: "value" ]
// query = null : then we assume we fetch everything
//
// AND and OR filter just embed a left/right couple of queries.
// query = [ operation: "AND", left: query1, right: query2 ]
// query = [ operation: "OR", left: query1, right: query2 ]
//
// Returns: A list of Maps. Each map describing one row.
// !!!! Each Map must contain a '__UID__' and '__NAME__' attribute.
// This is required to build a ConnectorObject.

def getAttributeValue(attributes, attributeName) {

  for (int j = 0; j < attributes.size(); j++) {  
    if(attributes[j].schema.textValue() == attributeName) {
      return attributes[j].values[0].textValue();
    }
  }

  return "";
}

def buildConnectorObject(node) {
  ArrayNode attributes = node.get("plainAttrs");

  return [
    __UID__:node.get("key").textValue(),
    __NAME__:node.get("key").textValue(),
    key:node.get("key").textValue(),
    username:node.get("username").textValue(),
    given_name:getAttributeValue(attributes, "given_name"),
    family_name:getAttributeValue(attributes, "family_name"),
    email:getAttributeValue(attributes, "email")
  ];
}

log.info("Entering " + action + " Script");

// ----------------
// Manage pagination
// ----------------
def offset = options[OperationOptions.OP_PAGED_RESULTS_COOKIE] == null
? 0
: options[OperationOptions.OP_PAGED_RESULTS_COOKIE].toInteger();
 
def pageSize = options[OperationOptions.OP_PAGE_SIZE] == null 
? 100
: options[OperationOptions.OP_PAGE_SIZE].toInteger();

def limit = offset + pageSize;

log.ok("pagedResultsCookie: " + offset);
log.ok("pageSize: " + pageSize);
log.ok("limit: " + limit);
// ----------------

WebClient webClient = client;
ObjectMapper mapper = new ObjectMapper();

def result = []

switch (objectClass) {
case "__ACCOUNT__":
  if (query == null || (!query.get("left").equals("__UID__") && !query.get("conditionType").equals("EQUALS"))) {
    webClient.path("/users");
    Response response = webClient.get();    
    ArrayNode nodes = mapper.readTree(response.getEntity()).get("result");
    
    // beware: this is not enforcing any server-side pagination feature
    for (i = offset; i < (limit < nodes.size() ? limit: nodes.size()); i++) {
      result.add(buildConnectorObject(nodes.get(i)));
    }
  } else {
    webClient.path("/users/" + query.get("right"));
    Response response = webClient.get();
    if (response.getStatus() == 200) {
      ObjectNode node = mapper.readTree(response.getEntity());
      result.add(buildConnectorObject(node.get("result")));
    } else {
      log.warn("Could not read object {0}", query.get("right"));
    }
  }

  break

default:
  result;
}

// ----------------
// Return paged result cookie
// ----------------
def pagedResultCookieLine = [:]
if (pageSize > result.size()) {
  // no more results
  pagedResultCookieLine.put(OperationOptions.OP_PAGED_RESULTS_COOKIE, null);
} else {
  pagedResultCookieLine.put(OperationOptions.OP_PAGED_RESULTS_COOKIE, "" + limit);
}
 
result.add(pagedResultCookieLine);
// ----------------

return result;
