import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import javax.ws.rs.core.Response
import org.apache.cxf.jaxrs.client.WebClient
import org.identityconnectors.common.security.GuardedString
import org.identityconnectors.framework.common.objects.OperationOptions;
import javax.ws.rs.core.Form

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

def buildConnectorObject(node) {
  return [
    __UID__:node.get("id").textValue(), 
    __NAME__:node.get("id").textValue(),
    key:node.get("id").textValue(),
    username:node.get("userName").textValue(),
    password: null,
    firstName:node.get("name").get("givenName").textValue(),
    surname:node.get("name").get("familyName").textValue(),
    email:node.get("displayName").textValue() + "@esa.org"
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

  //Get token
  WebClient clientAuth = WebClient.create('https://gluu.biomass-maap.com');
    
  String authorizationHeader = "Basic ${ESA_GLUU_TOKEN}"

  clientAuth.header("Authorization", authorizationHeader);
  clientAuth.header("Content-Type", "application/x-www-form-urlencoded");

  clientAuth.path("/oxauth/restv1/token").accept("application/json");
  response = clientAuth.post(new Form().param("grant_type", "client_credentials"));
  ObjectNode node = mapper.readTree(response.getEntity());
  authToken = node.get("access_token").textValue();


  //Fetch data
  WebClient client2 =  WebClient.create('https://gluu.biomass-maap.com');

  client2.header("Authorization", "Bearer " + authToken);
  client2.header("Content-Type", "application/scim+json");

  client2.path("/identity/restv1/scim/v2/Users");
  response2 = client2.get();    
  ObjectNode node2 = mapper.readTree(response2.getEntity());
  ArrayNode users = node2.get("Resources")

  for (i = offset; i < (limit < users.size() ? limit: nodes.size()); i++) {
    result.add(buildConnectorObject(users.get(i)));
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
