
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
			sj.add("\"" + arg.getKey() + "\":\"" + arg.getValue() + "\"");
		    
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
	
	
	/**
	 * Avatars - Remove avatar 
	 * @param instanceId uuid/id of the instance 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject avatarDelete(String instanceId) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
		}};
		
		return apicallObject("/bot/i/:instanceId/avatar", arguments, "DELETE");
	}

	/**
	 * Avatars - Upload avatar 
	 * @param instanceId uuid/id of the instance 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject avatarUpload(String instanceId) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
		}};
		
		return apicallObject("/bot/i/:instanceId/avatar", arguments, "POST");
	}

	/**
	 * Filelist - Add an URL 
	 * @param url the actual url you want to add 
	 * @param title a user defined title that should be displayed 
	 * @param parent the UUID of the folder the url should be placed into 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject addUrl(String url, String title, String parent) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("url", url.toString());
			put("title", title.toString());
			put("parent", parent.toString());
		}};
		
		return apicallObject("/bot/url", arguments, "POST");
	}

	/**
	 * Filelist - Create folder 
	 * @param name name of the folder to be created 
	 * @param parent uuid of the parent folder 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject createFolder(String name, String parent) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("name", name.toString());
			put("parent", parent.toString());
		}};
		
		return apicallObject("/bot/folders", arguments, "POST");
	}

	/**
	 * Filelist - Delete file 
	 * @param id uuid of the file that should be deleted 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject deleteFile(String id) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("id", id.toString());
		}};
		
		return apicallObject("/bot/files/:id", arguments, "DELETE");
	}

	/**
	 * Filelist - List 
	 * @return JSONObject with fields <br>
	 * <b>mediainfo</b> <br>
	 * <b>mediainfo.uuid</b> the tracks' unique identifier <br>
	 * <b>mediainfo.parent</b> the tracks' parents' uuid <br>
	 * <b>mediainfo.type</b> type of the track <br>
	 * <b>mediainfo.mimeType</b> the recognized mime-type (currently unsupported) <br>
	 * <b>mediainfo.title</b> <br>
	 * <b>mediainfo.artist</b> <br>
	 * <b>mediainfo.tempTitle</b> this will contain the title of the current track in case of radio streams - if supported by the server <br>
	 * <b>mediainfo.tempArtist</b> this will contain the artist of the current track in case of radio streams - if supported by the server <br>
	 * <b>mediainfo.album</b> <br>
	 * <b>mediainfo.albumArtist</b> <br>
	 * <b>mediainfo.track</b> <br>
	 * <b>mediainfo.totalTracks</b> <br>
	 * <b>mediainfo.copyright</b> <br>
	 * <b>mediainfo.genre</b> <br>
	 * <b>mediainfo.thumbnail</b> this file will actually be available at /cache/%thumbnail <br>
	 * <b>mediainfo.codec</b> audio-codec used in the file (currently unsupported) <br>
	 * <b>mediainfo.duration</b> duration of the track in milliseconds <br>
	 * <b>mediainfo.bitrate</b> <br>
	 * <b>mediainfo.channels</b> <br>
	 * <b>mediainfo.samplerate</b> <br>
	 * <b>mediainfo.filesize</b> 
	 */
	public JSONArray getFiles() {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
		}};
		
		return apicallArray("/bot/files", arguments, "GET");
	}

	/**
	 * Filelist - Update file (tags) 
	 * @param id uuid of the file that should be deleted 
	 * @param title 
	 * @param artist 
	 * @param album 
	 * @param parent new parent-(folder) of the file 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject updateFile(String id, String title, String artist, String album, String parent) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("id", id.toString());
			put("title", title.toString());
			put("artist", artist.toString());
			put("album", album.toString());
			put("parent", parent.toString());
		}};
		
		return apicallObject("/bot/files/:id", arguments, "PATCH");
	}

	/**
	 * Filelist - Upload a file 
	 * @param filename the original name of the file 
	 * @param playlist a playlist the file should be added to after upload 
	 * @param folder the folder UUID the file should be added to after upload 
	 * @return JSONObject with fields <br>
	 * <b>uuid</b> the tracks' unique identifier <br>
	 * <b>parent</b> the tracks' parents' uuid <br>
	 * <b>type</b> type of the track <br>
	 * <b>mimeType</b> the recognized mime-type (currently unsupported) <br>
	 * <b>title</b> <br>
	 * <b>artist</b> <br>
	 * <b>tempTitle</b> this will contain the title of the current track in case of radio streams - if supported by the server <br>
	 * <b>tempArtist</b> this will contain the artist of the current track in case of radio streams - if supported by the server <br>
	 * <b>album</b> <br>
	 * <b>albumArtist</b> <br>
	 * <b>track</b> <br>
	 * <b>totalTracks</b> <br>
	 * <b>copyright</b> <br>
	 * <b>genre</b> <br>
	 * <b>thumbnail</b> this file will actually be available at /cache/%thumbnail <br>
	 * <b>codec</b> audio-codec used in the file (currently unsupported) <br>
	 * <b>duration</b> duration of the track in milliseconds <br>
	 * <b>bitrate</b> <br>
	 * <b>channels</b> <br>
	 * <b>samplerate</b> <br>
	 * <b>filesize</b> 
	 */
	public JSONObject uploadFile(String filename, String playlist, String folder) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("filename", filename.toString());
			put("playlist", playlist.toString());
			put("folder", folder.toString());
		}};
		
		return apicallObject("/bot/upload", arguments, "POST");
	}

	/**
	 * Filelist - Upload restrictions 
	 * @return JSONObject with fields <br>
	 * <b>maxSize</b> the maximum size an uploaded file can have 
	 */
	public JSONObject uploadInfo() {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
		}};
		
		return apicallObject("/bot/uploadInfo", arguments, "GET");
	}

	/**
	 * General - General Information 
	 * @return JSONObject with fields <br>
	 * <b>bot</b> <br>
	 * <b>system</b> <br>
	 * <b>system.codecs</b> supported codecs <br>
	 * <b>system.formats</b> supported formats <br>
	 * <b>usageMemory</b> used memory from the bot (all instances - excluding client resources) 
	 */
	public JSONObject botInfo() {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
		}};
		
		return apicallObject("/bot/info", arguments, "GET");
	}

	/**
	 * General - Login 
	 * @param username the users' account name 
	 * @param password the users' password 
	 * @param botId the bot id you want to log in to 
	 * @return JSONObject with fields <br>
	 * <b>token</b> authorization token <br>
	 * <b>botId</b> id of the bot logged in to 
	 */
	public JSONObject login(String username, String password, String botId) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("username", username.toString());
			put("password", password.toString());
			put("botId", botId.toString());
		}};
		
		return apicallObject("/bot/login", arguments, "POST");
	}

	/**
	 * Instances - Create 
	 * @return JSONObject with fields <br>
	 * <b>success</b> <br>
	 * <b>uuid</b> uuid of the newly created instance 
	 */
	public JSONObject createInstance() {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
		}};
		
		return apicallObject("/bot/instances", arguments, "POST");
	}

	/**
	 * Instances - Delete 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject deleteInstance() {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
		}};
		
		return apicallObject("/bot/instances", arguments, "DELETE");
	}

	/**
	 * Instances - List 
	 * @return JSONObject with fields <br>
	 * <b>instances</b> <br>
	 * <b>instances.uuid</b> <br>
	 * <b>instances.nick</b> <br>
	 * <b>instances.name</b> <br>
	 * <b>instances.running</b> <br>
	 * <b>instances.mainInstance</b> 
	 */
	public JSONArray getInstances() {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
		}};
		
		return apicallArray("/bot/instances", arguments, "GET");
	}

	/**
	 * Instances - Settings 
	 * @param instanceId uuid/id of the instance 
	 * @param nick the nickname the bot should use 
	 * @param serverHost IP/hostname of the teamspeak server to use 
	 * @param serverPort the port to use 
	 * @param serverPassword the password to use 
	 * @param channelName 
	 * @param channelPassword 
	 * @param updateDescription update the client description to contain track information 
	 * @param announce announce new tracks in the channel 
	 * @param annonuceString which string to use when announcing tracks 
	 * @param identity a TeamSpeak identity that the bot should use 
	 * @param enableDucking ducking reduces the volume of music when somebody is talking in the channel 
	 * @param duckingVolume the volume level that should be used when ducking is active 
	 * @param channelCommander if true, the bot tries to become channel commander (required the permission on the ts server) 
	 * @param stickToChannel if true, the bot always tries to go back to its original channel when moved 
	 * @param ttsExternalURL the URL to use for Text-To-Speech (should contain the variables __TEXT and __LOCALE) 
	 * @param ttsDefaultLocale the default locale that should be used for the __LOCALE variable 
	 * @param ignoreChatServer ignores the server chat for commands 
	 * @param ignoreChatPrivate ignores private messages for commands 
	 * @param ignoreChatChannel ignores the channel chat for commands 
	 * @param idleTrack a mediaurl to be played when the bot becomes idle 
	 * @param startupTrack a mediaurl to be played when the bot starts up 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject getSettings(String instanceId, String nick, String serverHost, Integer serverPort, String serverPassword, String channelName, String channelPassword, Boolean updateDescription, Boolean announce, String annonuceString, String identity, Boolean enableDucking, Integer duckingVolume, Boolean channelCommander, Boolean stickToChannel, String ttsExternalURL, String ttsDefaultLocale, Boolean ignoreChatServer, Boolean ignoreChatPrivate, Boolean ignoreChatChannel, String idleTrack, String startupTrack) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
			put("nick", nick.toString());
			put("serverHost", serverHost.toString());
			put("serverPort", serverPort.toString());
			put("serverPassword", serverPassword.toString());
			put("channelName", channelName.toString());
			put("channelPassword", channelPassword.toString());
			put("updateDescription", updateDescription.toString());
			put("announce", announce.toString());
			put("annonuceString", annonuceString.toString());
			put("identity", identity.toString());
			put("enableDucking", enableDucking.toString());
			put("duckingVolume", duckingVolume.toString());
			put("channelCommander", channelCommander.toString());
			put("stickToChannel", stickToChannel.toString());
			put("ttsExternalURL", ttsExternalURL.toString());
			put("ttsDefaultLocale", ttsDefaultLocale.toString());
			put("ignoreChatServer", ignoreChatServer.toString());
			put("ignoreChatPrivate", ignoreChatPrivate.toString());
			put("ignoreChatChannel", ignoreChatChannel.toString());
			put("idleTrack", idleTrack.toString());
			put("startupTrack", startupTrack.toString());
		}};
		
		return apicallObject("/bot/i/:instanceId/settings", arguments, "POST");
	}

	/**
	 * Instances - Status 
	 * @param instanceId uuid/id of the instance 
	 * @return JSONObject with fields <br>
	 * <b>v</b> current version of the bot <br>
	 * <b>currentTrack</b> currently played track <br>
	 * <b>position</b> <br>
	 * <b>running</b> <br>
	 * <b>playing</b> <br>
	 * <b>shuffle</b> <br>
	 * <b>repeat</b> <br>
	 * <b>volume</b> <br>
	 * <b>needsRestart</b> this gets set after an update has been applied <br>
	 * <b>playlist</b> <br>
	 * <b>playlistTrack</b> <br>
	 * <b>queueLen</b> <br>
	 * <b>queueVersion</b> <br>
	 * <b>modes</b> <br>
	 * <b>downloaded</b> <br>
	 * <b>serverUID</b> <br>
	 * <b>flags</b> <br>
	 * <b>muted</b> 
	 */
	public JSONObject getStatus(String instanceId) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
		}};
		
		return apicallObject("/bot/i/:instanceId/status", arguments, "POST");
	}

	/**
	 * Instances - Shutdown 
	 * @param instanceId uuid/id of the instance 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject kill(String instanceId) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
		}};
		
		return apicallObject("/bot/i/:instanceId/kill", arguments, "POST");
	}

	/**
	 * Instances - Restart 
	 * @param instanceId uuid/id of the instance 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject respawn(String instanceId) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
		}};
		
		return apicallObject("/bot/i/:instanceId/respawn", arguments, "POST");
	}

	/**
	 * Instances - Launch 
	 * @param instanceId uuid/id of the instance 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject spawn(String instanceId) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
		}};
		
		return apicallObject("/bot/i/:instanceId/spawn", arguments, "POST");
	}

	/**
	 * Logging - Recent Bot-Log entries 
	 * @return JSONObject with fields <br>
	 * <b>entries</b> <br>
	 * <b>entries.message</b> the log string <br>
	 * <b>entries.severity</b> severity <br>
	 * <b>entries.time</b> timestamp (unix-time) 
	 */
	public JSONArray getBotLog() {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
		}};
		
		return apicallArray("/bot/log", arguments, "GET");
	}

	/**
	 * Logging - Instance-Log 
	 * @param instanceId uuid/id of the instance 
	 * @return JSONObject with fields <br>
	 * <b>entries</b> <br>
	 * <b>entries.message</b> the log string <br>
	 * <b>entries.severity</b> severity <br>
	 * <b>entries.time</b> timestamp (unix-time) 
	 */
	public JSONArray getInstanceLog(String instanceId) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
		}};
		
		return apicallArray("/bot/i/:instanceId/log", arguments, "GET");
	}

	/**
	 * Playback - Decrease volume by 5% 
	 * @param instanceId uuid/id of the instance 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject decreaseVolume(String instanceId) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
		}};
		
		return apicallObject("/bot/i/:instanceId/volume/down", arguments, "POST");
	}

	/**
	 * Playback - Increase volume by 5% 
	 * @param instanceId uuid/id of the instance 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject increaseVolume(String instanceId) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
		}};
		
		return apicallObject("/bot/i/:instanceId/volume/up", arguments, "POST");
	}

	/**
	 * Playback - Pause playback 
	 * @param instanceId uuid/id of the instance 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject pause(String instanceId) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
		}};
		
		return apicallObject("/bot/i/:instanceId/pause", arguments, "POST");
	}

	/**
	 * Playback - Playback a file 
	 * @param instanceId uuid/id of the instance 
	 * @param id uuid of the file to playback 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject playById(String instanceId, String id) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
			put("id", id.toString());
		}};
		
		return apicallObject("/bot/i/:instanceId/play/byId/:id", arguments, "POST");
	}

	/**
	 * Playback - Playback a file inside a playlist 
	 * @param instanceId uuid/id of the instance 
	 * @param playlistId uuid of the playlist 
	 * @param index number of the track inside the playlist 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject playByList(String instanceId, String playlistId, Integer index) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
			put("playlistId", playlistId.toString());
			put("index", index.toString());
		}};
		
		return apicallObject("/bot/i/:instanceId/play/byList/:playlistId/:index", arguments, "POST");
	}

	/**
	 * Playback - Playback URL 
	 * @param instanceId uuid/id of the instance 
	 * @param url the url that should be played back 
	 * @param plugin name of the plugin that returned this url 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject playUrl(String instanceId, String url, String plugin) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
			put("url", url.toString());
			put("plugin", plugin.toString());
		}};
		
		return apicallObject("/bot/i/:instanceId/playUrl?url=:url&plugin=:plugin", arguments, "POST");
	}

	/**
	 * Playback - recently played tracks 
	 * @param instanceId uuid/id of the instance 
	 * @return JSONObject with fields <br>
	 * <b>body</b> array of uuids 
	 */
	public JSONObject recentTracks(String instanceId) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
		}};
		
		return apicallObject("/bot/i/:instanceId/recent", arguments, "GET");
	}

	/**
	 * Playback - Say (TTS) 
	 * @param instanceId uuid/id of the instance 
	 * @param text the text to say 
	 * @param locale the locale to use - if none is given, the default one will be used 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject say(String instanceId, String text, String locale) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
			put("text", text.toString());
			put("locale", locale.toString());
		}};
		
		return apicallObject("/bot/i/:instanceId/say", arguments, "POST");
	}

	/**
	 * Playback - Seek 
	 * @param instanceId uuid/id of the instance 
	 * @param val position in percent 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject seek(String instanceId, Integer val) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
			put("val", val.toString());
		}};
		
		return apicallObject("/bot/i/:instanceId/seek/:val", arguments, "POST");
	}

	/**
	 * Playback - Enable/Disable mute 
	 * @param instanceId uuid/id of the instance 
	 * @param val 0 / 1 to disable/enable mute 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject setMute(String instanceId, Integer val) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
			put("val", val.toString());
		}};
		
		return apicallObject("/bot/i/:instanceId/mute/:val", arguments, "POST");
	}

	/**
	 * Playback - Set the volume 
	 * @param instanceId uuid/id of the instance 
	 * @param volume volume level in percent (0-100) 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject setVolume(String instanceId, Integer volume) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
			put("volume", volume.toString());
		}};
		
		return apicallObject("/bot/i/:instanceId/volume/set/:volume", arguments, "POST");
	}

	/**
	 * Playback - Stop playback 
	 * @param instanceId uuid/id of the instance 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject stop(String instanceId) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
		}};
		
		return apicallObject("/bot/i/:instanceId/stop", arguments, "POST");
	}

	/**
	 * Playlists - Next 
	 * @param instanceId uuid/id of the instance 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject playNext(String instanceId) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
		}};
		
		return apicallObject("/bot/i/:instanceId/playNext", arguments, "POST");
	}

	/**
	 * Playlists - Previous 
	 * @param instanceId uuid/id of the instance 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject playPrevious(String instanceId) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
		}};
		
		return apicallObject("/bot/i/:instanceId/playPrevious", arguments, "POST");
	}

	/**
	 * Playlists - Enable/Disable repeat 
	 * @param instanceId uuid/id of the instance 
	 * @param val 0 / 1 to disable/enable repeat 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject setRepeat(String instanceId, Integer val) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
			put("val", val.toString());
		}};
		
		return apicallObject("/bot/i/:instanceId/repeat/:val", arguments, "POST");
	}

	/**
	 * Playlists - Enable/Disable shuffle 
	 * @param instanceId uuid/id of the instance 
	 * @param val 0 / 1 to disable/enable shuffle 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject setShuffle(String instanceId, Integer val) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
			put("val", val.toString());
		}};
		
		return apicallObject("/bot/i/:instanceId/shuffle/:val", arguments, "POST");
	}

	/**
	 * Queue - Append a track to the queue 
	 * @param instanceId uuid/id of the instance 
	 * @param uuid track uuid 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject appendQueue(String instanceId, String uuid) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
			put("uuid", uuid.toString());
		}};
		
		return apicallObject("/bot/i/:instanceId/queue/append/:uuid", arguments, "POST");
	}

	/**
	 * Queue - Get list 
	 * @param instanceId uuid/id of the instance 
	 * @return JSONObject with fields <br>
	 * <b>mediainfo</b> list of tracks in the queue 
	 */
	public JSONArray getQueue(String instanceId) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
		}};
		
		return apicallArray("/bot/i/:instanceId/queue", arguments, "GET");
	}

	/**
	 * Queue - Prepend a track to the queue 
	 * @param instanceId uuid/id of the instance 
	 * @param uuid track uuid 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject prependQueue(String instanceId, String uuid) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
			put("uuid", uuid.toString());
		}};
		
		return apicallObject("/bot/i/:instanceId/queue/prepend/:uuid", arguments, "POST");
	}

	/**
	 * Queue - Remove from queue 
	 * @param instanceId uuid/id of the instance 
	 * @param queuePos the position of the track inside the queue that should be removed 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject removeFromQueue(String instanceId, Integer queuePos) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
			put("queuePos", queuePos.toString());
		}};
		
		return apicallObject("/bot/i/:instanceId/queue/:queuePos", arguments, "DELETE");
	}

	/**
	 * Radio - Get radio stations 
	 * @param search limit output to stations which match this string 
	 * @return JSONObject with fields <br>
	 * <b>station</b> <br>
	 * <b>s.n</b> name of the station <br>
	 * <b>s.u</b> url of the station <br>
	 * <b>s.g</b> genre <br>
	 * <b>s.b</b> bitrate 
	 */
	public JSONArray getRadioStations(String search) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("search", search.toString());
		}};
		
		return apicallArray("/bot/stations?q=:search", arguments, "PATCH");
	}

	/**
	 * Scripts - List Scripts 
	 * @return JSONObject with fields <br>
	 * <b>scriptname</b> <br>
	 * <b>scriptname.name</b> <br>
	 * <b>scriptname.version</b> <br>
	 * <b>scriptname.description</b> <br>
	 * <b>scriptname.author</b> <br>
	 * <b>scriptname.vars</b> <br>
	 * <b>scriptname.vars.varname</b> <br>
	 * <b>scriptname.vars.varname.title</b> <br>
	 * <b>scriptname.vars.varname.type</b> 
	 */
	public JSONArray getScripts() {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
		}};
		
		return apicallArray("/bot/scripts", arguments, "GET");
	}

	/**
	 * Scripts - Save Settings 
	 * @param instanceId uuid/id of the instance 
	 * @param ScriptName 
	 * @param ScriptName_enabled 
	 * @param ScriptName_config the JSON-encoded script settings 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject saveSettings(String instanceId, Object[] ScriptName, Boolean ScriptName_enabled, String ScriptName_config) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
			put("ScriptName", ScriptName.toString());
			put("ScriptName_enabled", ScriptName_enabled.toString());
			put("ScriptName_config", ScriptName_config.toString());
		}};
		
		return apicallObject("/bot/i/:instanceId/scriptSettings", arguments, "POST");
	}

	/**
	 * Streaming - Get audio stream 
	 * @param instanceId uuid/id of the instance 
	 * @param token a token acquired by streamToken 
	 * @return 
	 */
	public JSONObject getStream(String instanceId, String token) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
			put("token", token.toString());
		}};
		
		return apicallObject("/bot/i/:instanceId/stream/:token", arguments, "GET");
	}

	/**
	 * Streaming - Get a token for the WebStream 
	 * @param instanceId uuid/id of the instance 
	 * @return JSONObject with fields <br>
	 * <b>success</b> <br>
	 * <b>token</b> the token that can be used to initiate the WebStream 
	 */
	public JSONObject getStreamToken(String instanceId) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
		}};
		
		return apicallObject("/bot/i/:instanceId/streamToken", arguments, "POST");
	}

	/**
	 * TeamSpeak - Channel list 
	 * @param instanceId uuid/id of the instance 
	 * @return JSONObject with fields <br>
	 * <b>channel</b> <br>
	 * <b>channel.id</b> <br>
	 * <b>channel.parent</b> <br>
	 * <b>channel.name</b> <br>
	 * <b>channel.topic</b> <br>
	 * <b>channel.codec</b> <br>
	 * <b>channel.quality</b> <br>
	 * <b>channel.maxClients</b> <br>
	 * <b>channel.order</b> <br>
	 * <b>channel.perm</b> 1 = is permanent <br>
	 * <b>channel.sperm</b> 1 = is semi-permanent <br>
	 * <b>channel.default</b> 1 = is default <br>
	 * <b>channel.pw</b> 1 = is passworded <br>
	 * <b>channel.enc</b> 1 = is encrypted <br>
	 * <b>channel.clients</b> <br>
	 * <b>channel.clients.id</b> <br>
	 * <b>channel.clients.uid</b> <br>
	 * <b>channel.clients.nick</b> <br>
	 * <b>channel.clients.idle</b> <br>
	 * <b>channel.clients.recording</b> <br>
	 * <b>channel.clients.outputMuted</b> <br>
	 * <b>channel.clients.outputOnlyMuted</b> <br>
	 * <b>channel.clients.inputMuted</b> <br>
	 * <b>channel.clients.away</b> 
	 */
	public JSONArray getChannels(String instanceId) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
		}};
		
		return apicallArray("/bot/i/:instanceId/channels", arguments, "GET");
	}

	/**
	 * Upload_and_Download - Add job 
	 * @param url the url from that the bot should download 
	 * @return JSONObject with fields <br>
	 * <b>success</b> <br>
	 * <b>uuid</b> uuid of the job just added 
	 */
	public JSONObject addJob(String url) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("url", url.toString());
		}};
		
		return apicallObject("/bot/jobs", arguments, "POST");
	}

	/**
	 * Upload_and_Download - Cancel job 
	 * @param jobId uuid of the job (not the track) 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject cancelJob(String jobId) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("jobId", jobId.toString());
		}};
		
		return apicallObject("/bot/jobs/:jobId", arguments, "DELETE");
	}

	/**
	 * Upload_and_Download - Remove finished entries 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject clearJobs() {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
		}};
		
		return apicallObject("/bot/jobs", arguments, "DELETE");
	}

	/**
	 * Upload_and_Download - List Jobs 
	 * @return JSONObject with fields <br>
	 * <b>jobs</b> <br>
	 * <b>jobs.uuid</b> uuid <br>
	 * <b>jobs.url</b> url <br>
	 * <b>jobs.size</b> estimated size of the target <br>
	 * <b>jobs.perc</b> estimated progression in percent <br>
	 * <b>jobs.status</b> <br>
	 * <b>jobs.trackuuid</b> uuid of the track that was generated out of the job <br>
	 * <b>jobs.message</b> extended message of the external script <br>
	 * <b>jobs.eta</b> estimated time of download to go <br>
	 * <b>jobs.bw</b> current badnwidth <br>
	 * <b>jobs.play</b> autoplay when done <br>
	 * <b>jobs.temp</b> delete after playback <br>
	 * <b>jobs.done</b> true, if the job is finished 
	 */
	public JSONArray getJobs() {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
		}};
		
		return apicallArray("/bot/jobs", arguments, "GET");
	}

	/**
	 * Users - Create 
	 * @param name username 
	 * @param password new password for the user 
	 * @param tsuid TeamSpeak unique identifier (identity) 
	 * @param tsgid TeamSpeak group-id 
	 * @param privileges bitmask of the users' privileges 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject createUser(String name, String password, String tsuid, String tsgid, Integer privileges) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("name", name.toString());
			put("password", password.toString());
			put("tsuid", tsuid.toString());
			put("tsgid", tsgid.toString());
			put("privileges", privileges.toString());
		}};
		
		return apicallObject("/bot/users", arguments, "POST");
	}

	/**
	 * Users - Delete 
	 * @param userId uuid of the user that should be deleted 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject deleteUser(String userId) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("userId", userId.toString());
		}};
		
		return apicallObject("/bot/users/:userId", arguments, "DELETE");
	}

	/**
	 * Users - List 
	 * @return JSONObject with fields <br>
	 * <b>users</b> <br>
	 * <b>users.id</b> uuid of the user <br>
	 * <b>users.name</b> name of the user (login-name) <br>
	 * <b>users.tsuid</b> TeamSpeak unique identifier that has been bound to the user <br>
	 * <b>users.tsgid</b> TeamSpeak group-id that has been bound to the user <br>
	 * <b>users.locked</b> if true, the user cannot login <br>
	 * <b>users.isAdmin</b> if true, this is the admin-user that cannot be changed <br>
	 * <b>users.privileges</b> bitmask of all privileges the user has, see privileges 
	 */
	public JSONArray getUsers() {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
		}};
		
		return apicallArray("/bot/users", arguments, "GET");
	}

	/**
	 * Users - Update Instance Privileges 
	 * @param instanceId uuid/id of the instance 
	 * @param userId uuid of the user that should be deleted 
	 * @param privileges bitmask of the users' privileges for this instance 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject updateInstanceUserPrivileges(String instanceId, String userId, Integer privileges) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("instanceId", instanceId.toString());
			put("userId", userId.toString());
			put("privileges", privileges.toString());
		}};
		
		return apicallObject("/bot/i/:instanceId/users/:userId", arguments, "PATCH");
	}

	/**
	 * Users - Update 
	 * @param userId uuid of the user that should be deleted 
	 * @param password new password for the user 
	 * @param tsuid TeamSpeak unique identifier (identity) 
	 * @param tsgid TeamSpeak group-id 
	 * @param privileges bitmask of the users' privileges 
	 * @return JSONObject with fields <br>
	 * <b>success</b> 
	 */
	public JSONObject updateUser(String userId, String password, String tsuid, String tsgid, Integer privileges) {
		@SuppressWarnings("serial")
		HashMap<String,String> arguments = new HashMap<String,String>() {{
			put("userId", userId.toString());
			put("password", password.toString());
			put("tsuid", tsuid.toString());
			put("tsgid", tsgid.toString());
			put("privileges", privileges.toString());
		}};
		
		return apicallObject("/bot/users/:userId", arguments, "PATCH");
	}

}
