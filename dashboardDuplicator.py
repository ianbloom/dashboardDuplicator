import requests
import json
import hashlib
import base64
import time
import hmac
import json

# Credentials
AccessId = "dSpe6j9eTQXs3Iph7jCU"
AccessKey = "dcm!p2d2w79V=5f}+[354xL=g{k442Y6h5qV}C_6"
account = "ianbloom"

# ID of goldenDash
goldenDash = 98
# Dashboard group ID of deviceGroups
deviceGroups = 10

# Request Info
httpVerb ='GET'
resourcePath = '/dashboard/dashboards/' + str(goldenDash)
queryParams = '?template=true'
# Data payload is EMPTY for GET requests
data = ''

# Construct URL 
url = 'https://'+ account +'.logicmonitor.com/santaba/rest' + resourcePath + queryParams

# Get current time in milliseconds
epoch = str(int(time.time() * 1000))

# Concatenate Request details
requestVars = httpVerb + epoch + data + resourcePath

# Construct signature
authCode = hmac.new(AccessKey.encode(),msg=requestVars.encode(),digestmod=hashlib.sha256).hexdigest()
signature = base64.b64encode(authCode.encode())

# Construct headers
auth = 'LMv1 ' + AccessId + ':' + signature.decode() + ':' + epoch
headers = {'Content-Type':'application/json','Authorization':auth,'X-Version':'2'}

# Make request
response = requests.get(url, data=data, headers=headers)
payload = json.loads(response.content)
print("OUTPUT FROM GET: \n")
print(payload)

#Print status and body of response
#print('Response Status:',response.status_code)
#print('Response Body:',response.content)

widgetTokens = []
nameValue = {}
nameValue['name'] = "defaultDeviceGroup"
nameValue['value'] = "USA"
widgetTokens.append(nameValue)


# Begin constructing data payload for POSTs to dashboard resource
postPayload = {}
# Name is a mandatory field
postPayload['name'] = "black swan"
postPayload['sharable'] = True
postPayload['groupId'] = deviceGroups
postPayload['template'] = payload
postPayload['widgetTokens'] = widgetTokens
finalJSON = json.dumps(postPayload)

# Request Info
httpVerb ='POST'
resourcePath = '/dashboard/dashboards'
queryParams = ''
# Data payload is EMPTY for GET requests
data = finalJSON
print("MODIFIED: \n")
print(data + '\n')

# Construct URL 
url = 'https://'+ account +'.logicmonitor.com/santaba/rest' + resourcePath + queryParams

# Get current time in milliseconds
epoch = str(int(time.time() * 1000))

# Concatenate Request details
requestVars = httpVerb + epoch + data + resourcePath

# Construct signature
authCode = hmac.new(AccessKey.encode(),msg=requestVars.encode(),digestmod=hashlib.sha256).hexdigest()
signature = base64.b64encode(authCode.encode())

# Construct headers
auth = 'LMv1 ' + AccessId + ':' + signature.decode() + ':' + epoch
headers = {'Content-Type':'application/json','Authorization':auth,'X-Version':'2'}

# Make request
response = requests.post(url, data=data, headers=headers)
payload = json.loads(response.content)
print("FROM THE POST: \n")
print(payload)
