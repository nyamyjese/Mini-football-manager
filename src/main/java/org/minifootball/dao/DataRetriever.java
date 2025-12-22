package org.minifootball.dao;

import org.minifootball.db.DBConnection;
import org.minifootball.model.ContinentEnum;
import org.minifootball.model.Player;
import org.minifootball.model.PlayerPositionEnum;
import org.minifootball.model.Team;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DataRetriever {

    public Team findTeamById(Integer id) {
        if (id == null) {
            return null;
        }

        final String teamSql = """
            SELECT id, name, continent 
            FROM team 
            WHERE id = ?
            """;

        final String playersSql = """
            SELECT id, name, age, position
            FROM player
            WHERE id_team = ?
            """;

        Team team = null;

        try (Connection conn = DBConnection.getDBConnection()) {

            try (PreparedStatement teamPs = conn.prepareStatement(teamSql)) {
                teamPs.setInt(1, id);

                try (ResultSet rs = teamPs.executeQuery()) {
                    if (rs.next()) {
                        team = new Team(
                                rs.getInt("id"),
                                rs.getString("name"),
                                ContinentEnum.valueOf(rs.getString("continent")),
                                new ArrayList<>()
                        );
                    } else {
                        System.out.println("Team id : " + id + "not found ! ");
                    }
                }
            }

            List<Player> players = new ArrayList<>();

            try (PreparedStatement playersPs = conn.prepareStatement(playersSql)) {
                playersPs.setInt(1, id);

                try (ResultSet rs = playersPs.executeQuery()) {
                    while (rs.next()) {
                        Player player = new Player(
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getInt("age"),
                                PlayerPositionEnum.valueOf(rs.getString("position")),
                                team
                        );
                        players.add(player);
                    }
                }
            }

            team.setPlayers(players);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return team;
    }

    public List<Player> findPlayers(int page, int size) {
        if (page < 1 || size < 1) {
            throw new IllegalArgumentException("Page and size must be positive");
        }

        int offset = (page - 1) * size;

        final String sql = """
            SELECT
                p.id          AS player_id,
                p.name        AS player_name,
                p.age,
                p.position,
                t.id          AS team_id,
                t.name        AS team_name,
                t.continent
            FROM player p
            LEFT JOIN team t ON p.id_team = t.id
            ORDER BY p.id
            LIMIT ? OFFSET ?
            """;

        List<Player> players = new ArrayList<>();

        try (Connection conn = DBConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, size);
            ps.setInt(2, offset);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Team team = null;
                    int teamId = rs.getInt("team_id");

                    if (!rs.wasNull()) {
                        team = new Team(
                                teamId,
                                rs.getString("team_name"),
                                ContinentEnum.valueOf(rs.getString("continent")),
                                null
                        );
                    }

                    Player player = new Player(
                            rs.getInt("player_id"),
                            rs.getString("player_name"),
                            rs.getInt("age"),
                            PlayerPositionEnum.valueOf(rs.getString("position")),
                            team
                    );
                    players.add(player);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return players;
    }
}