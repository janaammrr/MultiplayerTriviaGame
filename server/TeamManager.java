package server;

import model.GameConfig;
import model.Question;
import model.Team;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeamManager {
    private static final Map<String, Team> teams = new HashMap<>();

    public static synchronized String createTeam(String teamName, ClientHandler creator, String category,
                                                 String difficulty, int questionCount, int maxPlayers,
                                                 GameConfig config) {
        if (teams.containsKey(teamName)) {
            return "Team already exists";
        }
        if (maxPlayers < config.getMinTeamPlayers() || maxPlayers > config.getMaxTeamPlayers()) {
            return "Invalid team size. Allowed range is " + config.getMinTeamPlayers() + " to " + config.getMaxTeamPlayers();
        }

        Team team = new Team(teamName, creator.getUser().getUsername(), category, difficulty, questionCount, maxPlayers);
        team.addMember(creator);
        teams.put(teamName, team);
        return "Team created successfully";
    }

    public static synchronized String joinTeam(String teamName, ClientHandler player) {
        Team team = teams.get(teamName);
        if (team == null) {
            return "Team Not Found";
        }
        if (team.isInGame()) {
            return "Team already in game";
        }
        if (team.size() >= team.getMaxPlayers()) {
            return "Team is already full";
        }
        if (!team.addMember(player)) {
            return "Could not join team";
        }
        return "Joined Team Successfully!";
    }

    public static synchronized List<Team> listOpenTeams() {
        List<Team> result = new ArrayList<>();
        for (Team team : teams.values()) {
            if (!team.isInGame()) {
                result.add(team);
            }
        }
        result.sort(Comparator.comparing(Team::getTeamName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    public static synchronized GameRoom tryCreateMatch(GameService gameService, GameConfig config) {
        List<Team> readyTeams = new ArrayList<>();
        for (Team team : teams.values()) {
            if (team.isReady() && !team.isInGame()) {
                readyTeams.add(team);
            }
        }

        for (int i = 0; i < readyTeams.size(); i++) {
            for (int j = i + 1; j < readyTeams.size(); j++) {
                Team first = readyTeams.get(i);
                Team second = readyTeams.get(j);
                if (!isCompatible(first, second)) {
                    continue;
                }

                List<Question> questions = gameService.getQuestionsForGame(
                        first.getCategory(), first.getDifficulty(), first.getQuestionCount());
                if (questions.size() < first.getQuestionCount()) {
                    return null;
                }

                first.setInGame(true);
                second.setInGame(true);
                return new GameRoom(first, second, questions, gameService, config);
            }
        }
        return null;
    }

    public static synchronized void handleDisconnect(String teamName, ClientHandler player) {
        Team team = teams.get(teamName);
        if (team == null) {
            return;
        }
        team.removeMember(player);
        if (team.size() == 0) {
            teams.remove(teamName);
        }
    }

    public static synchronized void finishMatch(Team first, Team second) {
        if (first != null) {
            teams.remove(first.getTeamName());
        }
        if (second != null) {
            teams.remove(second.getTeamName());
        }
    }

    private static boolean isCompatible(Team first, Team second) {
        return first.size() == second.size()
                && first.getMaxPlayers() == second.getMaxPlayers()
                && first.getQuestionCount() == second.getQuestionCount()
                && first.getCategory().equalsIgnoreCase(second.getCategory())
                && first.getDifficulty().equalsIgnoreCase(second.getDifficulty());
    }
}
