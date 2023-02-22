package pl.skidam.yetanotherauthmod;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class LoginDatabase {
    private final Path databasePath;
    private final Map<String, String> users;
    private final Gson gson;

    public LoginDatabase(Path databasePath) {
        this.databasePath = databasePath;
        this.users = new HashMap<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        load();
    }

    public void addUser(String login, String password) {
        if (users.containsKey(login)) {
            return;
        }
        password = Utils.sha256(password);
        users.put(login, password);
        save();
    }

    public boolean checkLogin(String login, String password) {
        if (!users.containsKey(login)) {
            return false;
        }
        password = Utils.sha256(password);
        return users.get(login).equals(password);
    }

    public boolean userExists(String login) {
        return users.containsKey(login);
    }

    private void load() {
        try {
            Files.createDirectories(databasePath.getParent());
            if (Files.exists(databasePath)) {
                String json = Files.readString(databasePath);
                Type type = new TypeToken<Map<String, String>>(){}.getType();
                Map<String, String> map = gson.fromJson(json, type);
                if (map != null) {
                    users.putAll(map);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void save() {
        try {
            String json = gson.toJson(users);
            Files.writeString(databasePath, json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}