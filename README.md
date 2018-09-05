# Sinusbot-API-Java-Class-Generator
This python script generates a Java class for convenient Sinusbot API usage.

The generated Java class depends on [org.json](https://mvnrepository.com/artifact/org.json/json).  
Either a `JSONObject` or a `JSONArray` are returned by each API function, depending on the HTTP json response.

Obtain botId field via `http://127.0.0.1:8087/api/v1/botId` Sinusbot API.

## Example Usage
```Java
// Create API instance and login
SinusbotAPI api = new SinusbotAPI("127.0.0.1", 8087, "admin", "password", "botId"); 

// Use the TTS API
api.say("instanceId", "Test!", "en"); 

// Print titles of all files stored on Sinusbot
JSONArray response = api.getFiles();
for (int i = 0; i < response.length(); i++)
  System.out.println(response.getJSONObject(i).get("title")); 
```
