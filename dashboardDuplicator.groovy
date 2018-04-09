import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Hex;
import groovy.json.JsonSlurper;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.ContentType;

def accessId = "dSpe6j9eTQXs3Iph7jCU";
def accessKey = "dcm!p2d2w79V=5f}+[354xL=g{k442Y6h5qV}C_6";
def account = "ianbloom";
def requestVerb = 'GET';
def resourcePath = '/dashboard/dashboards/98/widgets';
def queryParameters = '';
def data = '';
/*
{
    "id" : 98,
    "groupId" : 1,
    "name" : "goldenDash",
    "fullName" : "goldenDash",
    "description" : "",
    "sharable" : true,
    "owner" : "",
    "widgetsOrder" : "",
    "userPermission" : "write",
    "groupName" : "ianbloom",
    "groupFullPath" : "",
    "widgetTokens" : [ ],
    "widgetsConfig" : { },
    "template" : null
  } 
*/
/*
"widgetsConfig" : {
            "113" : {
              "col" : 1,
              "sizex" : 4,
              "row" : 1,
              "sizey" : 2
            },
            "114" : {
              "col" : 5,
              "sizex" : 2,
              "row" : 1,
              "sizey" : 4
            },
            "115" : {
              "col" : 1,
              "sizex" : 4,
              "row" : 3,
              "sizey" : 2
            }
*/
          

responseDict = LMGET(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
responseBody = responseDict.body;
output = new JsonSlurper().parseText(responseBody);


widgetsArray = output.items;

// Initialize widgetsConfig array to hold widgetsConfig for dashboard configuration
widgetsConfig = [:];

widgetsArray.each { widget ->
	widgetsConfig[widget.id] = [:];
	widgetsConfig[widget.id]['col'] = widget.order;
	widgetsConfig[widget.id]['sizex'] = widget.colSpan;
	widgetsConfig[widget.id]['row'] = widget.columnIdx;
	widgetsConfig[widget.id]['sizey'] = widget.rowSpan
}

println(widgetsConfig);
return 0;









/////////////////////////////////////
// Santa's Little Helper Functions //
/////////////////////////////////////

def LMPUT(_accessId, _accessKey, _account, _requestVerb, _resourcePath, _queryParameters, _data) {

	// Initialize dictionary to hold response code and response body
	responseDict = [:];

	// Construcst URL to POST to from specified input
	url = 'https://' + _account + '.logicmonitor.com' + '/santaba/rest' + _resourcePath + _queryParameters;

	StringEntity params = new StringEntity(_data,ContentType.APPLICATION_JSON);

	// Get current time
	epoch = System.currentTimeMillis();

	// Calculate signature
	requestVars = _requestVerb + epoch + _data + _resourcePath;

	hmac = Mac.getInstance('HmacSHA256');
	secret = new SecretKeySpec(_accessKey.getBytes(), 'HmacSHA256');
	hmac.init(secret);
	hmac_signed = Hex.encodeHexString(hmac.doFinal(requestVars.getBytes()));
	signature = hmac_signed.bytes.encodeBase64();

	// HTTP Get
	CloseableHttpClient httpclient = HttpClients.createDefault();
	http_request = new HttpPut(url);
	http_request.addHeader("Authorization" , "LMv1 " + _accessId + ":" + signature + ":" + epoch);
	http_request.setHeader("Accept", "application/json");
	http_request.setHeader("Content-type", "application/json");
	http_request.setEntity(params);
	response = httpclient.execute(http_request);
	responseBody = EntityUtils.toString(response.getEntity());
	code = response.getStatusLine().getStatusCode();

	responseDict['code'] = code;
	responseDict['body'] = responseBody
	
	return responseDict;
}

def LMGET(_accessId, _accessKey, _account, _requestVerb, _resourcePath, _queryParameters, _data) {
	// DATA SHOULD BE EMPTY
	// Initialize dictionary to hold response code and response body
	responseDict = [:];

	// Construcst URL to POST to from specified input
	url = 'https://' + _account + '.logicmonitor.com' + '/santaba/rest' + _resourcePath + _queryParameters;

	// Get current time
	epoch = System.currentTimeMillis();

	// Calculate signature
	requestVars = _requestVerb + epoch + _data + _resourcePath;

	hmac = Mac.getInstance('HmacSHA256');
	secret = new SecretKeySpec(_accessKey.getBytes(), 'HmacSHA256');
	hmac.init(secret);
	hmac_signed = Hex.encodeHexString(hmac.doFinal(requestVars.getBytes()));
	signature = hmac_signed.bytes.encodeBase64();

	// HTTP Get
	CloseableHttpClient httpclient = HttpClients.createDefault();
	http_request = new HttpGet(url);
	http_request.addHeader("Authorization" , "LMv1 " + _accessId + ":" + signature + ":" + epoch);
	http_request.setHeader("X-Version", "2");
	response = httpclient.execute(http_request);
	responseBody = EntityUtils.toString(response.getEntity());
	code = response.getStatusLine().getStatusCode();

	responseDict['code'] = code;
	responseDict['body'] = responseBody
	
	return responseDict;
}

def LMPOST(_accessId, _accessKey, _account, _requestVerb, _resourcePath, _queryParameters, _data) {

	// Initialize dictionary to hold response code and response body
	responseDict = [:];

	// Construcst URL to POST to from specified input
	url = 'https://' + _account + '.logicmonitor.com' + '/santaba/rest' + _resourcePath + _queryParameters;

	StringEntity params = new StringEntity(_data,ContentType.APPLICATION_JSON);

	// Get current time
	epoch = System.currentTimeMillis();

	// Calculate signature
	requestVars = _requestVerb + epoch + _data + _resourcePath;

	hmac = Mac.getInstance('HmacSHA256');
	secret = new SecretKeySpec(_accessKey.getBytes(), 'HmacSHA256');
	hmac.init(secret);
	hmac_signed = Hex.encodeHexString(hmac.doFinal(requestVars.getBytes()));
	signature = hmac_signed.bytes.encodeBase64();

	// HTTP Get
	CloseableHttpClient httpclient = HttpClients.createDefault();
	http_request = new HttpPost(url);
	http_request.addHeader("Authorization" , "LMv1 " + _accessId + ":" + signature + ":" + epoch);
	http_request.setHeader("X-Version", "2");
	http_request.setHeader("Accept", "application/json");
	http_request.setHeader("Content-type", "application/json");
	http_request.setEntity(params);
	response = httpclient.execute(http_request);
	responseBody = EntityUtils.toString(response.getEntity());
	code = response.getStatusLine().getStatusCode();

	responseDict['code'] = code;
	responseDict['body'] = responseBody
	
	return responseDict;
}