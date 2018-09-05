apiJsonUrl = 'https://www.sinusbot.com/api/api_data.js'

classFrame = """
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

public class SinusbotAPI {
	private final String APISTR = "/api/v1";
	private String token;
	private String ip;
	private int port;
	
	/**
	 * Constructor automatically logs in and grabs token
	 * @param ip Address of sinusbot
	 * @param port Port of sinusbot
	 * @param username Username to web interface
	 * @param password Password to web interface
	 * @param botId Bot id - get via /api/v1/botId
	 */
	public SinusbotAPI(String ip, int port, String username, String password, String botId) {
		this.ip = ip;
		this.port = port;
		JSONObject loginObj = login(username, password, botId);
		if (loginObj == null) {
			throw new RuntimeException("Login failed!");
		} else {
			this.token = loginObj.getString("token");
		}
	}

	private String apicall(String api, Map<String, String> args, String requestMethod) {
		StringJoiner sj = new StringJoiner(",");
		for (Map.Entry<String, String> arg : args.entrySet()) {
			sj.add("\\"" + arg.getKey() + "\\":\\"" + arg.getValue() + "\\"");
		    
			if (api.contains(":" + arg.getKey()))
				api = api.replace(":" + arg.getKey(), arg.getValue());
		}
		byte[] body = ("{" + sj.toString() + "}").getBytes(StandardCharsets.UTF_8);
		int length = body.length;
		
		String URL = "http://"+ip+":"+port+APISTR+api;
		
		try {
			URL url = new URL(URL);
			HttpURLConnection http = (HttpURLConnection)url.openConnection();
	
			http.setRequestMethod(requestMethod);
			http.setRequestProperty("Authorization", "bearer " + token);
			
			if (!args.isEmpty()) {
				http.setDoOutput(true);
				http.setFixedLengthStreamingMode(length);
				http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
				http.connect();
				try(OutputStream os = http.getOutputStream()) {    
					os.write(body);
				}
			}

			BufferedReader br = new BufferedReader(new InputStreamReader(http.getInputStream()));
			
			return br.lines().collect(Collectors.joining());
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (ProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private JSONObject apicallObject(String api, Map<String, String> args, String requestMethod) {
		String jsonStr = apicall(api, args, requestMethod);
		if (jsonStr != null)
			return new JSONObject(jsonStr);
		return null;
	}
	
	private JSONArray apicallArray(String api, Map<String, String> args, String requestMethod) {
		String jsonStr = apicall(api, args, requestMethod);
		if (jsonStr != null)
			return new JSONArray(jsonStr);
		return null;
	}
	
	[api]
}
"""

funFrame = """
	/**
	 * [title] [description]
	 * @return [ret]
	 */
	public JSON[rettype] [name]([args]) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{[puts]
		}};
		
		return apicall[rettype]("[url]", arguments, "[type]");
	}
"""

putFrame = """			put("[paramName]", [paramName].toString());"""
descFrame = """	 * @param [paramName] [description]"""
retFrame = """	 * [ret]"""

import urllib.request
import json
import re

def typeStrToJavaType(typestr):
    typestr = re.search('<p>(.*?)</p>', typestr).group(1)
    conv = {
        "string": "String",
        "number": "Integer",
        "bool": "Boolean",
        "object[]": "Object[]",
        "Object[]": "Object[]"
    }
    return conv.get(typestr, "unknown")

def parseJsonAPIFunToJavaMethod(fun):
    funStr = funFrame.replace("[title]", fun["group"] + " - " + fun["title"])
    funStr = funStr.replace("[name]", fun["name"])
    funStr = funStr.replace("[url]", fun["url"])
    funStr = funStr.replace("[type]", fun["type"].upper())
    if 'parameter' in fun:
        params = fun["parameter"]["fields"]["Parameter"]
        puts = ""
        args = ""
        for i,param in enumerate(params):
            paramType = typeStrToJavaType(param["type"])
            if paramType == "unknown":
                return ""
            paramName = param["field"].replace(".", "_")
            if i < len(params) - 1:
                funStr = funStr.replace("[args]", paramType + " " + paramName + ", [args]")
            else:
                funStr = funStr.replace("[args]", paramType + " " + paramName + "[args]")
            putStr = putFrame.replace("[paramName]", paramName)
            funStr = funStr.replace("[puts]", "\n" + putStr + "[puts]")
            descStr = descFrame.replace("[paramName]", paramName)
            descStr = descStr.replace("[description]", param["description"].replace("<p>","").replace("</p>",""))
            funStr = funStr.replace("[description]", "\n" + descStr + "[description]")
    if 'success' in fun:
        success = fun["success"]["fields"]["Success 200"]
        funStr = funStr.replace("[ret]", "JSONObject with fields <br>[ret]")
        for i,succ in enumerate(success):
            succType = typeStrToJavaType(succ["type"])
            if succType == "Object[]":
                funStr = funStr.replace("[rettype]", "Array")
            else:
                funStr = funStr.replace("[rettype]", "Object")
            succName = succ["field"]
            succDesc = succ["description"].replace("<p>","").replace("</p>","")
            retStr = retFrame.replace("[ret]", "<b>" + succName + "</b> " + succDesc)
            if i < len(success) - 1:
                funStr = funStr.replace("[ret]", "\n" + retStr + "<br>[ret]")
            else:
                funStr = funStr.replace("[ret]", "\n" + retStr + "[ret]")
    funStr = funStr.replace("[puts]", "")
    funStr = funStr.replace("[args]", "")
    funStr = funStr.replace("[description]", "")
    funStr = funStr.replace("[ret]", "")
    funStr = funStr.replace("[rettype]", "Object")
    return funStr

# Get raw API json file
req = urllib.request.Request(apiJsonUrl, data=None, headers={'User-Agent': 'Mozilla/5.0 (Hi)'})
f = urllib.request.urlopen(req)
apistr = f.read().decode('utf-8')

# Clean from JS
apistr = apistr[7:-2]

# Parse to dictionary
api = json.loads(apistr)["api"]

# Parse every API function into a Java method
funNames = []
for fun in api:
    if fun["name"] in funNames:
        continue
    funNames.append(fun["name"])
    funStr = parseJsonAPIFunToJavaMethod(fun)
    classFrame = classFrame.replace("[api]", funStr + "[api]")

classFrame = classFrame.replace("[api]", "")

with open("SinusbotAPI.java", "w") as file:
    file.write(classFrame)
    