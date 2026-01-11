package org.minifootball.dao;

import org.minifootball.db.DBConnection;
import org.minifootball.model.ContinentEnum;
import org.minifootball.model.Player;
import org.minifootball.model.PlayerPositionEnum;
import org.minifootball.model.Team;

import java.sql.*;
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
            SELECT id, name, age, position, goal_nb
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
                        System.out.println("Team id : " + id + " not found ! ");
                        return null;
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
                                team,
                                rs.getObject("goal_nb") != null ? rs.getInt("goal_nb") : null
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
                p.id AS player_id,
                p.name AS player_name,
                p.age,
                p.position,
                p.goal_nb,
                t.id AS team_id,
                t.name AS team_name,
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
                                new ArrayList<>()
                        );
                    }

                    Player player = new Player(
                            rs.getInt("player_id"),
                            rs.getString("player_name"),
                            rs.getInt("age"),
                            PlayerPositionEnum.valueOf(rs.getString("position")),
                            team,
                            rs.getObject("goal_nb") != null ? rs.getInt("goal_nb") : null
                    );
                    players.add(player);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return players;
    }

    public List<Player> createPlayers(List<Player> newPlayers) throws SQLException {
        if (newPlayers == null || newPlayers.isEmpty()) {
            return List.of();
        }

        final String checkNameExists = "SELECT 1 FROM player WHERE name = ? LIMIT 1";
        final String insertPlayer = """
            INSERT INTO player (name, age, position, id_team, goal_nb)
            VALUES (?, ?, ?::player_position, ?, ?)
            """;

        List<Player> created = new ArrayList<>();
        try (Connection conn = DBConnection.getDBConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement psCheck = conn.prepareStatement(checkNameExists)) {
                    for (Player p : newPlayers) {
                        psCheck.setString(1, p.getName());
                        try (ResultSet rs = psCheck.executeQuery()) {
                            if (rs.next()) {
                                throw new RuntimeException("A player named '" + p.getName() + "' already exists");
                            }
                        }
                    }
                }
                try (PreparedStatement psInsert = conn.prepareStatement(insertPlayer, Statement.RETURN_GENERATED_KEYS)) {
                    for (Player p : newPlayers) {
                        psInsert.setString(1, p.getName());
                        psInsert.setInt(2, p.getAge());
                        psInsert.setString(3, p.getPosition().name());
                        Integer teamId = (p.getTeam() != null) ? p.getTeam().getId() : null;
                        psInsert.setObject(4, teamId);
                        psInsert.setObject(5, p.getGoal_nb());

                        psInsert.executeUpdate();

                        try (ResultSet keys = psInsert.getGeneratedKeys()) {
                            if (keys.next()) {
                                p.setId(keys.getInt(1));
                            }
                        }
                        created.add(p);
                    }
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e instanceof SQLException ? (SQLException) e : new SQLException("Failed to create players", e);
            } finally {
                conn.setAutoCommit(true);
            }
        }
        return created;
    }

    public Team saveTeam(Team team) throws SQLException {
        if (team == null || team.getName() == null || team.getName().isBlank()) {
            throw new IllegalArgumentException("Team and its name are required");
        }

        final String insertTeam = "INSERT INTO team (name, continent) VALUES (?, ?::continent_enum)";
        final String updateTeam = "UPDATE team SET name = ?, continent = ?::continent_enum WHERE id = ?";
        final String associatePlayer = "UPDATE player SET id_team = ? WHERE id = ?";

        try (Connection conn = DBConnection.getDBConnection()) {
            conn.setAutoCommit(false);
            try {
                Integer teamId = team.getId();

                if (teamId == null) {
                    try (PreparedStatement ps = conn.prepareStatement(insertTeam, Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, team.getName());
                        ps.setString(2, team.getContinent().name());
                        ps.executeUpdate();
                        try (ResultSet keys = ps.getGeneratedKeys()) {
                            if (keys.next()) {
                                teamId = keys.getInt(1);
                                team.setId(teamId);
                            }
                        }
                    }
                } else {
                    try (PreparedStatement ps = conn.prepareStatement(updateTeam)) {
                        ps.setString(1, team.getName());
                        ps.setString(2, team.getContinent().name());
                        ps.setInt(3, teamId);
                        int updated = ps.executeUpdate();
                        if (updated == 0) {
                            throw new SQLException("Team with id " + teamId + " not found");
                        }
                    }
                }


                List<Player> players = team.getPlayers() != null ? team.getPlayers() : new ArrayList<>();


                if (players.isEmpty()) {
                    try (PreparedStatement ps = conn.prepareStatement("UPDATE player SET id_team = NULL WHERE id_team = ?")) {
                        ps.setInt(1, teamId);
                        ps.executeUpdate();
                    }
                } else {
                    StringBuilder sb = new StringBuilder("UPDATE player SET id_team = NULL WHERE id_team = ? AND id NOT IN (");
                    for (int i = 0; i < players.size(); i++) {
                        if (i > 0) sb.append(",");
                        sb.append("?");
                    }
                    sb.append(")");
                    try (PreparedStatement ps = conn.prepareStatement(sb.toString())) {
                        ps.setInt(1, teamId);
                        int paramIdx = 2;
                        for (Player p : players) {
                            ps.setInt(paramIdx++, p.getId());
                        }
                        ps.executeUpdate();
                    }
                }

                if (!players.isEmpty()) {
                    try (PreparedStatement ps = conn.prepareStatement(associatePlayer)) {
                        for (Player p : players) {
                            ps.setInt(1, teamId);
                            ps.setInt(2, p.getId());
                            ps.addBatch();
                            p.setTeam(team);
                        }
                        ps.executeBatch();
                    }
                }

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e instanceof SQLException ? (SQLException) e : new SQLException("Failed to save team", e);
            } finally {
                conn.setAutoCommit(true);
            }
        }
        return team;
    }

    public List<Team> findTeamsByPlayerName(String playerName) {
        List<Team> teams = new ArrayList<>();
        if (playerName == null || playerName.isBlank()) {
            return teams;
        }

        final String sql = """
            SELECT DISTINCT t.id, t.name, t.continent
            FROM team t
            JOIN player p ON p.id_team = t.id
            WHERE p.name ILIKE ?
            """;

        try (Connection conn = DBConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + playerName + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Team team = new Team(
                            rs.getInt("id"),
                            rs.getString("name"),
                            ContinentEnum.valueOf(rs.getString("continent")),
                            new ArrayList<>()
                    );
                    teams.add(team);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return teams;
    }

    public List<Player> findPlayersByCriteria(String playerName, PlayerPositionEnum position,
                                              String teamName, ContinentEnum continent,
                                              int page, int size) {
        if (page < 1 || size < 1) {
            throw new IllegalArgumentException("Page and size must be positive");
        }
        int offset = (page - 1) * size;

        StringBuilder sql = new StringBuilder("""
            SELECT
                p.id AS player_id,
                p.name AS player_name,
                p.age,
                p.position,
                p.goal_nb,
                t.id AS team_id,
                t.name AS team_name,
                t.continent
            FROM player p
            LEFT JOIN team t ON p.id_team = t.id
            WHERE 1=1
            """);

        List<Object> params = new ArrayList<>();

        if (playerName != null && !playerName.isBlank()) {
            sql.append(" AND p.name ILIKE ?");
            params.add("%" + playerName + "%");
        }
        if (position != null) {
            sql.append(" AND p.position = ?::player_position");
            params.add(position.name());
        }
        if (teamName != null && !teamName.isBlank()) {
            sql.append(" AND t.name ILIKE ?");
            params.add("%" + teamName + "%");
        }
        if (continent != null) {
            sql.append(" AND t.continent = ?::continent_enum");
            params.add(continent.name());
        }

        sql.append(" ORDER BY p.id LIMIT ? OFFSET ?");
        params.add(size);
        params.add(offset);

        List<Player> players = new ArrayList<>();
        try (Connection conn = DBConnection.getDBConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Team team = null;
                    int teamId = rs.getInt("team_id");
                    if (!rs.wasNull()) {
                        team = new Team(
                                teamId,
                                rs.getString("team_name"),
                                ContinentEnum.valueOf(rs.getString("continent")),
                                new ArrayList<>()
                        );
                    }

                    Player player = new Player(
                            rs.getInt("player_id"),
                            rs.getString("player_name"),
                            rs.getInt("age"),
                            PlayerPositionEnum.valueOf(rs.getString("position")),
                            team,
                            rs.getObject("goal_nb") != null ? rs.getInt("goal_nb") : null
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