package model;

import server.ClientHandler;
import java.util.*;

public class Team {

    private String teamName;
    private List<ClientHandler> members = new ArrayList<>();

    public Team(String teamName) {
        this.teamName = teamName;
    }

    public String getTeamName() {
        return teamName;
    }

    public List<ClientHandler> getMembers() {
        return members;
    }

    public void addMember(ClientHandler player) {
        members.add(player);
    }

    public int size() {
        return members.size();
    }
}