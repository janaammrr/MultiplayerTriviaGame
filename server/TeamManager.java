package server;

import model.Team;
import java.util.*;

public class TeamManager {

    private static Map<String, Team> teams = new HashMap<>();

    public static synchronized String createTeam(String name) {
        if (teams.containsKey(name))
            return "Team already exists";

        teams.put(name, new Team(name));
        return "Team created successfully";
    }

    public static synchronized Team getTeam(String name) {
        return teams.get(name);
    }

    public static synchronized boolean teamExists(String name) {
        return teams.containsKey(name);
    }
}