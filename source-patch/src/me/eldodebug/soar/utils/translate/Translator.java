package me.eldodebug.soar.utils.translate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import me.eldodebug.soar.utils.TimerUtils;

public class Translator {
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 8000;
    private static String authCache;
    private static TimerUtils timer;

    public static final String AUTO_DETECT = "";
    public static final String ENGLISH = "en";
    public static final String JAPANESE = "ja";
    public static final String CHINESE_SIMPLIFIED = "zh-Hans";
    public static final String CHINESE_TRADITIONAL = "zh-Hant";
    public static final String POLISH = "pl";

    private static HttpURLConnection open(URL url) throws Exception {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setConnectTimeout(CONNECT_TIMEOUT_MS);
        con.setReadTimeout(READ_TIMEOUT_MS);
        con.setUseCaches(false);
        return con;
    }

    private static synchronized String auth() throws Exception {
        if (timer == null) timer = new TimerUtils();
        if (!timer.delay(300 * 1000) && authCache != null) return authCache;

        HttpURLConnection con = open(new URL("https://edge.microsoft.com/translate/auth"));
        con.setRequestMethod("GET");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) content.append(line);
            authCache = content.toString();
            timer.reset();
            return authCache;
        } finally {
            con.disconnect();
        }
    }

    public static String translate(String text, String from, String to) throws Exception {
        String fromEncoded = URLEncoder.encode(from == null ? "" : from, "UTF-8");
        String toEncoded = URLEncoder.encode(to == null ? "" : to, "UTF-8");
        URL url = new URL("https://api.cognitive.microsofttranslator.com/translate?from=" + fromEncoded + "&to=" + toEncoded + "&api-version=3.0&includeSentenceLength=true");
        HttpURLConnection con = open(url);
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        con.setRequestProperty("authorization", "Bearer " + auth());
        con.setDoOutput(true);

        JsonObject request = new JsonObject();
        request.addProperty("Text", text == null ? "" : text);
        JsonArray requestArray = new JsonArray();
        requestArray.add(request);
        byte[] input = new Gson().toJson(requestArray).getBytes(StandardCharsets.UTF_8);

        try {
            try (OutputStream os = con.getOutputStream()) {
                os.write(input);
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder responseContent = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) responseContent.append(responseLine.trim());

                JsonArray jsonArray = new JsonParser().parse(responseContent.toString()).getAsJsonArray();
                StringBuilder result = new StringBuilder();
                for (JsonElement json : jsonArray) {
                    JsonArray translations = json.getAsJsonObject().getAsJsonArray("translations");
                    for (JsonElement trans : translations) {
                        if (result.length() > 0) result.append(',');
                        result.append(trans.getAsJsonObject().get("text").getAsString());
                    }
                }
                return result.toString();
            }
        } finally {
            con.disconnect();
        }
    }
}
