package org.minifootball;

import org.minifootball.dao.DataRetriever;
import org.minifootball.model.ContinentEnum;
import org.minifootball.model.Player;
import org.minifootball.model.PlayerPositionEnum;
import org.minifootball.model.Team;

import java.sql.SQLException;
import java.util.List;

public class Main {

    private static final DataRetriever dao = new DataRetriever();

    public static void main(String[] args) {
        System.out.println("=== MiniFootball DAO Tests ===\n");

        testFindTeamById_1_RealMadrid();
        testFindTeamById_5_InterMiami();
        testFindPlayers_pagination();
        testFindTeamsByPlayerName_an();
        testFindPlayersByCriteria_JudeBellingham();
        testCreatePlayers_duplicateName();
        testCreatePlayers_success();
        testSaveTeam_addViniToRealMadrid();
        testSaveTeam_removeAllPlayersFromBarca();
    }

    private static void testFindTeamById_1_RealMadrid() {
        System.out.println("a) findTeamById(1) ");
        Team team = dao.findTeamById(1);
        printTeam(team);
        System.out.println();
    }

    private static void testFindTeamById_5_InterMiami() {
        System.out.println("b) findTeamById(5) ");
        Team team = dao.findTeamById(5);
        printTeam(team);
        System.out.println();
    }

    private static void testFindPlayers_pagination() {
        System.out.println("c) findPlayers(page=1, size=2) ");
        List<Player> page1 = dao.findPlayers(1, 2);
        printPlayers(page1, "Page 1 (size 2)");

        System.out.println("\nd) findPlayers(page=3, size=5) ");
        List<Player> page3 = dao.findPlayers(3, 5);
        printPlayers(page3, "Page 3 (size 5)");
        System.out.println();
    }

    private static void testFindTeamsByPlayerName_an() {
        System.out.println("e) findTeamsByPlayerName(\"an\")");
        List<Team> teams = dao.findTeamsByPlayerName("an");
        for (Team t : teams) {
            System.out.println("  → " + t.getName());
        }
        System.out.println();
    }

    private static void testFindPlayersByCriteria_JudeBellingham() {
        System.out.println("f) findPlayersByCriteria: ");
        List<Player> result = dao.findPlayersByCriteria(
                "ud",
                PlayerPositionEnum.MIDF,
                "Madrid",
                ContinentEnum.EUROPA,
                1, 10
        );

        printPlayers(result, "Search criteria results");
        System.out.println();
    }

    private static void testCreatePlayers_duplicateName() {
        System.out.println("g) createPlayers with duplicate name");
        List<Player> playersWithDuplicate = List.of(
                new Player(6, "Jude Bellingham", 23, PlayerPositionEnum.STR, null)
        );

        try {
            dao.createPlayers(playersWithDuplicate);
            System.out.println("ERROR: should have thrown an exception");
        } catch (RuntimeException e) {
            System.out.println("→ Exception correctly caught: " + e.getMessage());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        System.out.println();
    }

    private static void testCreatePlayers_success() {
        System.out.println("h) createPlayers Vini + Pedri");
        List<Player> newPlayers = List.of(
                new Player(6, "Vini Jr.", 25, PlayerPositionEnum.STR, null),
                new Player(7, "Pedri", 24, PlayerPositionEnum.MIDF, null)
        );

        try {
            List<Player> created = dao.createPlayers(newPlayers);
            System.out.println("Players created successfully:");
            printPlayers(created, "");
        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
        }
        System.out.println();
    }

    private static void testSaveTeam_addViniToRealMadrid() {
        System.out.println("i) saveTeam: add Vini to Real Madrid ");
        Team real = dao.findTeamById(1);
        if (real == null) {
            System.out.println("Real Madrid not found!");
            return;
        }


        Player vini = new Player(6, "Vini Jr.", 25, PlayerPositionEnum.STR, null);
        real.getPlayers().add(vini);

        try {
            Team updated = dao.saveTeam(real);
            System.out.println("Team updated: " + updated.getName());
            printTeam(updated);
        } catch (SQLException e) {
            System.out.println("Error while saving: " + e.getMessage());
        }
        System.out.println();
    }

    private static void testSaveTeam_removeAllPlayersFromBarca() {
        System.out.println("j) saveTeam: remove all players from FC Barcelona (id=2)");
        Team barca = dao.findTeamById(2);
        if (barca == null) {
            System.out.println("FC Barcelona not found!");
            return;
        }


        barca.setPlayers(List.of());

        try {
            Team updated = dao.saveTeam(barca);
            System.out.println("Team updated (players removed): " + updated.getName());
            printTeam(updated);
        } catch (SQLException e) {
            System.out.println("Error while saving: " + e.getMessage());
        }
        System.out.println();
    }


    private static void printTeam(Team team) {
        if (team == null) {
            System.out.println("Team not found");
            return;
        }
        System.out.printf("Team: %s (%s)%n", team.getName(), team.getContinent());
        System.out.println("Players (" + team.getPlayers().size() + "):");
        for (Player p : team.getPlayers()) {
            System.out.printf("  - %s (%s, %d years)%n",
                    p.getName(), p.getPosition(), p.getAge());
        }
    }

    private static void printPlayers(List<Player> players, String title) {
        if (!title.isBlank()) {
            System.out.println(title);
        }
        if (players.isEmpty()) {
            System.out.println("  (empty list)");
            return;
        }
        for (Player p : players) {
            String teamName = p.getTeam() != null ? p.getTeam().getName() : "No team";
            System.out.printf("  - %s (%s, %s)%n", p.getName(), p.getPosition(), teamName);
        }
    }
}