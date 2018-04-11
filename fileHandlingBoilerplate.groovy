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

def file1 = new File('groovy1.txt');
file1.write('TEST FAIL PLZ');

def openedFile = new File('groovy1.txt').text;
if (openedFile == 'TEST') {
	println('it totally works');
}
else {
	println('it totally doesnt work');
}
