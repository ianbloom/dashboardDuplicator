import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Hex;
import groovy.json.JsonSlurper;
import groovy.json.JsonOutput;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.ContentType;


def accessId = "dSpe6j9eTQXs3Iph7jCU";
def accessKey = "dcm!p2d2w79V=5f}+[354xL=g{k442Y6h5qV}C_6";
def account = "ianbloom";
// goldenDash is the ID of the goldenDash
// might have to get this by name in prod script
def goldenDash = '98';
// ID of the dashboardGroup you would like to dump all dashboards into
def dashboardGroupName = 'Need_A_Place';
// ID of the device group you would like treat as root group
def deviceGroup = '39';
def fileName = 'goldenDash.txt';
def groupFileName = 'groups.txt';

// fileExists is a boolean value that tests for the existence of a template
// if !fileExists then this is first run, and we create the file
def fileExists = new File(fileName).exists();
def groupFileExists = new File(groupFileName).exists();

if(fileExists && groupFileExists) {
	println('the file exists');
	// Open the file as text for comparison to current goldenDash template
	file = new File(fileName).text;
	file2 = new File(groupFileName).text;
	
	////////////////////////////
	// GET DASHBOARD TEMPLATE //
	////////////////////////////

	requestVerb = 'GET';
	resourcePath = "/dashboard/dashboards/${goldenDash}";
	// We need template=true to copy widget position
	queryParameters = '?template=true';
	data = '';

	responseDict = LMGET(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
	responseBody = responseDict.body;
	// Parse responseBody (JSON string) as JSON object
	template = new JsonSlurper().parseText(responseBody);

	////////////////////////
	// GET GROUP TEMPLATE //
	////////////////////////

	requestVerb = 'GET';
	resourcePath = "/device/groups";
	queryParameters = "?filter=parentId~${deviceGroup}&fields=name,id";
	data = '';

	responseDict2 = LMGET(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
	responseBody2 = responseDict.body;

	// Check if current goldenDash matches file
	if(file == responseBody && file2 == responseBody2) {
		println('MATCH');
		return 0;
	}
	else {
		println('NO MATCH');

		////////////////////////////
		// WRITE TEMPLATE TO FILE //
		////////////////////////////

		file = new File(fileName);
		file.write(responseBody);
		
		file2 = new File(groupFileName);
		file2.write(responseBody2);

		///////////////////////
		// GET DEVICE GROUPS //
		///////////////////////

		requestVerb = 'GET';
		resourcePath = "/device/groups";
		queryParameters = "?filter=parentId~${deviceGroup}";
		data = '';

		responseDict = LMGET(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
		responseBody = responseDict.body;
		// Parse responseBody (JSON string) as JSON object
		output = new JsonSlurper().parseText(responseBody);
		deviceGroupArray = output.items;

		/////////////////////////
		// GET DASHBOARD GROUP //
		/////////////////////////

		requestVerb = 'GET';
		resourcePath = "/dashboard/groups";
		queryParameters = "?filter=name~${dashboardGroupName}";
		data = '';

		responseDict = LMGET(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
		responseBody = responseDict.body;
		// Parse responseBody (JSON string) as JSON object
		output = new JsonSlurper().parseText(responseBody);
		// If the dashboard group already exists
		if(output.total == 1) {
			dashboardGroup = output.items[0].id;
		}
		else {
			requestVerb = 'POST';
			resourcePath = "/dashboard/groups";
			queryParameters = "";
			data = '{"name":"' + dashboardGroupName + '"}';

			responseDict = LMPOST(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
			responseBody = responseDict.body;
			// Parse responseBody (JSON string) as JSON object
			output = new JsonSlurper().parseText(responseBody);
			dashboardGroup = output.id;
		}

		responseBody = responseDict.body;
		responseCode = responseDict.code;
		println("Response Code: ${responseCode}");
		println("Response Body: ${responseBody}");

		////////////////////////////////////////////
		// CONSTRUCT PAYLOAD FOR DASHBOARD COPIES //
		////////////////////////////////////////////

		deviceGroupArray.each{ item ->

			//////////////////////
			// GET DASHBOARD ID //
			//////////////////////

			requestVerb = 'GET';
			resourcePath = "/dashboard/dashboards";
			queryParameters = "?filter=name:${item.name},groupId:${dashboardGroup}";
			//queryParameters = "?filter=groupId:${dashboardGroup}";
			// Use json constructed earlier as payload for POST
			data = '';

			responseDict = LMGET(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
			responseBody = responseDict.body;
			responseCode = responseDict.code;
			//println("responseCode: ${responseCode}");
			//println("responseBody: ${responseBody}");
			output = new JsonSlurper().parseText(responseBody);

			// If the dashboard does not exist yet, then POST
			if(output.total == 0) {

				///////////////////
				// BUILD PAYLOAD //
				///////////////////

				// widgetTokens must be an array containing a series of NVPs
				widgetTokens = [];
				widgetTokensDict = [:];
				widgetTokensDict['name'] = 'defaultDeviceGroup';
				widgetTokensDict['value'] = "${item.fullPath}";
				// Create widgetTokens based on the fullPath of device subgroup
				widgetTokens.add(widgetTokensDict);

				// Initialize postPayload dictionary to begin constructing POST JSON
				postPayload = [:];
				postPayload['widgetTokens'] = widgetTokens;
				postPayload['sharable'] = true;
				postPayload['name'] = item.name;
				postPayload['groupId'] = dashboardGroup;
				// Append template that was obtained from the Golden Dash
				postPayload['template'] = template;

				// Transform JSON into JSON string
				json = JsonOutput.toJson(postPayload);
				//println(json);

				///////////////////////////
				// POST DASHBOARD COPIES //
				///////////////////////////

				requestVerb = 'POST';
				resourcePath = "/dashboard/dashboards";
				queryParameters = '';
				// Use json constructed earlier as payload for POST
				data = json;

				responseDict = LMPOST(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
				responseBody = responseDict.body;
				responseCode = responseDict.code;
				println("Response Code: ${responseCode}");
				println("Response Body: ${responseBody}");
				// Parse responseBody (JSON string) as JSON object
				output = new JsonSlurper().parseText(responseBody);

			}
			// If the dashboard DOES exist, capture its ID then DELETE and replace with POST
			else {
				dashId = output.items[0].id;
				println("Dashboard ID: ${dashId}");

				/////////////////////
				// DELETE ORIGINAL //
				/////////////////////

				requestVerb = 'DELETE';
				resourcePath = "/dashboard/dashboards/${dashId}";
				queryParameters = '';
				// Use json constructed earlier as payload for POST
				data = '';

				responseDict = LMDELETE(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
				responseBody = responseDict.body;
				responseCode = responseDict.code;
				println("DELETE CODE: ${responseCode}");
				println("DELETE BODY: ${responseBody}");


				///////////////////
				// BUILD PAYLOAD //
				///////////////////

				// widgetTokens must be an array containing a series of NVPs
				widgetTokens = [];
				widgetTokensDict = [:];
				widgetTokensDict['name'] = 'defaultDeviceGroup';
				widgetTokensDict['value'] = "${item.fullPath}";
				// Create widgetTokens based on the fullPath of device subgroup
				widgetTokens.add(widgetTokensDict);

				// Initialize postPayload dictionary to begin constructing POST JSON
				postPayload = [:];
				postPayload['widgetTokens'] = widgetTokens;
				postPayload['sharable'] = true;
				postPayload['name'] = item.name;
				postPayload['groupId'] = dashboardGroup;
				// Append template that was obtained from the Golden Dash
				postPayload['template'] = template;

				// Transform JSON into JSON string
				json = JsonOutput.toJson(postPayload);
				//println(json);

				///////////////////////////
				// POST DASHBOARD COPIES //
				///////////////////////////

				requestVerb = 'POST';
				//resourcePath = "/dashboard/dashboards/${dashId}";
				resourcePath = "/dashboard/dashboards";

				queryParameters = '';
				// Use json constructed earlier as payload for POST
				data = json;

				responseDict = LMPOST(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
				responseBody = responseDict.body;
				responseCode = responseDict.code;
				println("Response Code: ${responseCode}");
				println("Response Body: ${responseBody}");
				// Parse responseBody (JSON string) as JSON object
				output = new JsonSlurper().parseText(responseBody);
			}
		}
	}
}
else {

	////////////////////////////
	// GET DASHBOARD TEMPLATE //
	////////////////////////////

	requestVerb = 'GET';
	resourcePath = "/dashboard/dashboards/${goldenDash}";
	// We need template=true to copy widget position
	queryParameters = '?template=true';
	data = '';

	responseDict = LMGET(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
	responseBody = responseDict.body;

	// Parse responseBody (JSON string) as JSON object
	template = new JsonSlurper().parseText(responseBody);

	////////////////////////////
	// WRITE TEMPLATE TO FILE //
	////////////////////////////

	file = new File(fileName);
	file.write(responseBody);

	//////////////////////////
	// WRITE GROUPS TO FILE //
	//////////////////////////

	requestVerb = 'GET';
	resourcePath = "/device/groups";
	queryParameters = "?filter=parentId~${deviceGroup}&fields=name,id";
	data = '';

	responseDict2 = LMGET(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
	responseBody2 = responseDict.body;

	file2 = new File(groupFileName);
	file2.write(responseBody2);
	
	///////////////////////
	// GET DEVICE GROUPS //
	///////////////////////

	requestVerb = 'GET';
	resourcePath = "/device/groups";
	queryParameters = "?filter=parentId~${deviceGroup}";
	data = '';

	responseDict = LMGET(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
	responseBody = responseDict.body;
	// Parse responseBody (JSON string) as JSON object
	output = new JsonSlurper().parseText(responseBody);
	deviceGroupArray = output.items;

	/////////////////////////
	// GET DASHBOARD GROUP //
	/////////////////////////

	requestVerb = 'GET';
	resourcePath = "/dashboard/groups";
	queryParameters = "?filter=name~${dashboardGroupName}";
	data = '';

	responseDict = LMGET(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
	responseBody = responseDict.body;
	// Parse responseBody (JSON string) as JSON object
	output = new JsonSlurper().parseText(responseBody);
	// If the dashboard group already exists
	if(output.total == 1) {
		dashboardGroup = output.items[0].id;
	}
	else {
		requestVerb = 'POST';
		resourcePath = "/dashboard/groups";
		queryParameters = "";
		data = '{"name":"' + dashboardGroupName + '"}';

		responseDict = LMPOST(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
		responseBody = responseDict.body;
		// Parse responseBody (JSON string) as JSON object
		output = new JsonSlurper().parseText(responseBody);
		dashboardGroup = output.id;
	}

	responseBody = responseDict.body;
	responseCode = responseDict.code;
	println("Response Code: ${responseCode}");
	println("Response Body: ${responseBody}");

	////////////////////////////////////////////
	// CONSTRUCT PAYLOAD FOR DASHBOARD COPIES //
	////////////////////////////////////////////

	deviceGroupArray.each{ item ->

		//////////////////////
		// GET DASHBOARD ID //
		//////////////////////

		requestVerb = 'GET';
		resourcePath = "/dashboard/dashboards";
		queryParameters = "?filter=name:${item.name},groupId:${dashboardGroup}";
		//queryParameters = "?filter=groupId:${dashboardGroup}";
		// Use json constructed earlier as payload for POST
		data = '';

		responseDict = LMGET(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
		responseBody = responseDict.body;
		responseCode = responseDict.code;
		//println("responseCode: ${responseCode}");
		//println("responseBody: ${responseBody}");
		output = new JsonSlurper().parseText(responseBody);

		// If the dashboard does not exist yet, then POST
		if(output.total == 0) {

			///////////////////
			// BUILD PAYLOAD //
			///////////////////

			// widgetTokens must be an array containing a series of NVPs
			widgetTokens = [];
			widgetTokensDict = [:];
			widgetTokensDict['name'] = 'defaultDeviceGroup';
			widgetTokensDict['value'] = "${item.fullPath}";
			// Create widgetTokens based on the fullPath of device subgroup
			widgetTokens.add(widgetTokensDict);

			// Initialize postPayload dictionary to begin constructing POST JSON
			postPayload = [:];
			postPayload['widgetTokens'] = widgetTokens;
			postPayload['sharable'] = true;
			postPayload['name'] = item.name;
			postPayload['groupId'] = dashboardGroup;
			// Append template that was obtained from the Golden Dash
			postPayload['template'] = template;

			// Transform JSON into JSON string
			json = JsonOutput.toJson(postPayload);
			//println(json);

			///////////////////////////
			// POST DASHBOARD COPIES //
			///////////////////////////

			requestVerb = 'POST';
			resourcePath = "/dashboard/dashboards";
			queryParameters = '';
			// Use json constructed earlier as payload for POST
			data = json;

			responseDict = LMPOST(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
			responseBody = responseDict.body;
			responseCode = responseDict.code;
			println("Response Code: ${responseCode}");
			println("Response Body: ${responseBody}");
			// Parse responseBody (JSON string) as JSON object
			output = new JsonSlurper().parseText(responseBody);

		}
		// If the dashboard DOES exist, capture its ID then DELETE and replace with POST
		else {
			dashId = output.items[0].id;
			println("Dashboard ID: ${dashId}");

			/////////////////////
			// DELETE ORIGINAL //
			/////////////////////

			requestVerb = 'DELETE';
			resourcePath = "/dashboard/dashboards/${dashId}";
			queryParameters = '';
			// Use json constructed earlier as payload for POST
			data = '';

			responseDict = LMDELETE(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
			responseBody = responseDict.body;
			responseCode = responseDict.code;
			println("DELETE CODE: ${responseCode}");
			println("DELETE BODY: ${responseBody}");


			///////////////////
			// BUILD PAYLOAD //
			///////////////////

			// widgetTokens must be an array containing a series of NVPs
			widgetTokens = [];
			widgetTokensDict = [:];
			widgetTokensDict['name'] = 'defaultDeviceGroup';
			widgetTokensDict['value'] = "${item.fullPath}";
			// Create widgetTokens based on the fullPath of device subgroup
			widgetTokens.add(widgetTokensDict);

			// Initialize postPayload dictionary to begin constructing POST JSON
			postPayload = [:];
			postPayload['widgetTokens'] = widgetTokens;
			postPayload['sharable'] = true;
			postPayload['name'] = item.name;
			postPayload['groupId'] = dashboardGroup;
			// Append template that was obtained from the Golden Dash
			postPayload['template'] = template;

			// Transform JSON into JSON string
			json = JsonOutput.toJson(postPayload);
			//println(json);

			///////////////////////////
			// POST DASHBOARD COPIES //
			///////////////////////////

			requestVerb = 'POST';
			//resourcePath = "/dashboard/dashboards/${dashId}";
			resourcePath = "/dashboard/dashboards";

			queryParameters = '';
			// Use json constructed earlier as payload for POST
			data = json;

			responseDict = LMPOST(accessId, accessKey, account, requestVerb, resourcePath, queryParameters, data);
			responseBody = responseDict.body;
			responseCode = responseDict.code;
			println("Response Code: ${responseCode}");
			println("Response Body: ${responseBody}");
			// Parse responseBody (JSON string) as JSON object
			output = new JsonSlurper().parseText(responseBody);
		}
	}
}


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
	http_request.setHeader("X-Version", "2");
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

def LMDELETE(_accessId, _accessKey, _account, _requestVerb, _resourcePath, _queryParameters, _data) {

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
	http_request = new HttpDelete(url);
	http_request.addHeader("Authorization" , "LMv1 " + _accessId + ":" + signature + ":" + epoch);
	http_request.setHeader("X-Version", "2");
	response = httpclient.execute(http_request);
	responseBody = EntityUtils.toString(response.getEntity());
	code = response.getStatusLine().getStatusCode();

	responseDict['code'] = code;
	responseDict['body'] = responseBody
	
	return responseDict;
}