package pl.skidam.yetanotherauthmod;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(input.getBytes());
            byte[] bytes = md.digest();

            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String formatIP(String ip) {

        // calculate how many in ip there are :
        int count = 0;
        for (int i = 0; i < ip.length(); i++) {
            if (ip.charAt(i) == ':') {
                count++;
            } else if (count > 2) {
                break;
            }
        }

        if (count > 2) {
            String regex = "\\[(.*?)\\]";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(ip);

            if (matcher.find()) {
                return matcher.group(1).replace("/", "");
            }
        } else {
            String[] split = ip.split(":");
            return split[0].replace("/", "");
        }

        return "";
    }

    public static String hasPurchasedMinecraft(String playerName) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        int response = connection.getResponseCode();

        // If server returns 200, player under this username has a Mojang account
        if (response == 200) {
            // return UUID of player
            try (InputStream inputStream = connection.getInputStream()) {
                JsonElement jsonElement = JsonParser.parseReader(new InputStreamReader(inputStream));
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                return jsonObject.get("id").getAsString();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}
