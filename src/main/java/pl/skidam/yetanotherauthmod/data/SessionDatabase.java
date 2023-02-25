package pl.skidam.yetanotherauthmod.data;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class SessionDatabase {
    private final Path databasePath;
    private final Map<String, Session> sessions;
    private final Gson gson;

    public SessionDatabase(Path databasePath) {
        this.databasePath = databasePath;
        this.sessions =  new HashMap<>();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        load();
    }

    public void createSession(String login, String IP) {
        if (sessions.containsKey(login)) {
            deleteSession(login); // Delete old session
        }
        String now = String.valueOf(System.currentTimeMillis());
        sessions.put(login, new Session(IP, now));
        save();
    }

    public void deleteSession(String login) {
        if (!sessions.containsKey(login)) {
            return;
        }
        sessions.remove(login);
        save();
    }

    public boolean activeSession(String login) {
        if (!sessions.containsKey(login)) {
            return false;
        }

        // Time of the session creation
        long CreationDate = Long.parseLong(sessions.get(login).getCreationDate());

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(CreationDate);
        calendar.add(Calendar.DAY_OF_MONTH, 30);

        // Time in milliseconds 30 days from now
        long ExpiresDate = calendar.getTimeInMillis();
        long CurrentDate = System.currentTimeMillis();

        return ExpiresDate > CurrentDate;
    }

    public boolean checkSession(String login, String IP) {
        if (!sessions.containsKey(login)) {
            return false;
        }

        if (!sessions.get(login).IP().equals(IP)) {
            return false;
        }

        return activeSession(login);
    }

    public boolean moreSessionsOnThisIP(String IP) {
        int count = 0;
        for (Session session : sessions.values()) {
            if (session.IP().equals(IP)) {
                count++;
            }
        }
        return count > 1;
    }

    private void load() {
        try {
            Files.createDirectories(databasePath.getParent());
            if (Files.exists(databasePath)) {
                String json = Files.readString(databasePath);
                Type type = new TypeToken<Map<String, Session>>(){}.getType();
                Map<String, Session> map = gson.fromJson(json, type);
                if (map != null) {
                    sessions.putAll(map);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void save() {
        try {
            String json = gson.toJson(sessions);
            Files.writeString(databasePath, json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private record Session(String IP, String CreationDate) {
        public String getCreationDate() {
                return CreationDate;
            }
        }
}
