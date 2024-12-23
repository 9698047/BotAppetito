import java.sql.*;
import java.util.*;

public class DatabaseManager {

    private static Connection connection;

    private static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/botappetito", "root", "");
            } catch (SQLException e) {
                System.err.println("Errore nella connessione al database: " + e.getMessage());
                throw e;
            }
        }
        return connection;
    }

    // Recupera una ricetta randomica
    public static Ricetta getRandomRecipeWithDetails() {
        Ricetta ricetta = null;
        String sql = "SELECT id, titolo, immagine, procedimento, link_originale, tempo_preparazione FROM ricette ORDER BY RAND() LIMIT 1";
        try (PreparedStatement statement = getConnection().prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            if (rs.next()) {
                ricetta = new Ricetta(
                        rs.getInt("id"),
                        rs.getString("titolo"),
                        rs.getString("immagine"),
                        rs.getString("procedimento"),
                        rs.getString("link_originale"),
                        getIngredienti(rs.getInt("id")),
                        rs.getString("tempo_preparazione")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ricetta;
    }

    // Recupera gli ingredienti per una ricetta
    public static List<String> getIngredienti(int ricettaId) {
        List<String> ingredienti = new ArrayList<>();

        String query =
                "SELECT i.nome " +
                        "FROM ingredienti i " +
                        "JOIN ricetta_ingredienti ri ON i.id = ri.ingrediente_id " +
                        "WHERE ri.ricetta_id = ?";

        try (PreparedStatement stmt = getConnection().prepareStatement(query)) {
            stmt.setInt(1, ricettaId);
            ResultSet rs = stmt.executeQuery();

            // Aggiungi ogni ingrediente alla lista
            while (rs.next()) {
                ingredienti.add(rs.getString("nome"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ingredienti;
    }

    // Aggiungi la ricetta ai preferiti dell'utente
    public static boolean aggiungiPreferiti(String userId, int ricettaId) {
        if(isRicettaPreferita(userId, ricettaId)) {
            String sql = "INSERT INTO preferiti (user_id, ricetta_id) VALUES (?, ?)";
            try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
                statement.setString(1, userId);
                statement.setInt(2, ricettaId);
                int rowsAffected = statement.executeUpdate();
                return rowsAffected > 0;
            } catch (SQLException e) {
                System.err.println("Errore durante l'aggiunta ai preferiti: " + e.getMessage());
                return false;
            }
        }
        else {
            System.err.println("Errore durante l'aggiunta ai preferiti: la ricetta è gia presente nella tua lista preferiti");
            return false;
        }
    }

    // Rimuovi la ricetta dai preferiti dell'utente
    public static boolean rimuoviPreferiti(String userId, int ricettaId) {
        String sql = "DELETE FROM preferiti WHERE user_id = ? AND ricetta_id = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, userId);
            statement.setInt(2, ricettaId);
            int rowsAffected = statement.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Errore durante la rimozione dai preferiti: " + e.getMessage());
            return false;
        }
    }

    // Controlla se una ricetta è nei preferiti di un utente
    public static boolean isRicettaPreferita(String userId, int ricettaId) {
        String sql = "SELECT COUNT(*) FROM preferiti WHERE user_id = ? AND ricetta_id = ?";
        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, userId);
            statement.setInt(2, ricettaId);
            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                return count > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Recupera le ricette preferite dell'utente
    public static List<Ricetta> getPreferitiByUser(String userId) {
        List<Ricetta> preferiti = new ArrayList<>();
        String sql =
                "SELECT r.id, r.titolo, r.immagine, r.procedimento, r.link_originale, r.tempo_preparazione " +
                        "FROM ricette r " +
                        "JOIN preferiti p ON r.id = p.ricetta_id " +
                        "WHERE p.user_id = ?";

        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, userId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Ricetta ricetta = new Ricetta(
                        rs.getInt("id"),
                        rs.getString("titolo"),
                        rs.getString("immagine"),
                        rs.getString("procedimento"),
                        rs.getString("link_originale"),
                        getIngredienti(rs.getInt("id")),
                        rs.getString("tempo_preparazione")
                );
                preferiti.add(ricetta);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return preferiti;
    }

    // Cerca ricette per nome
    public static List<Ricetta> cercaRicettaPerNome(String nome) {
        List<Ricetta> ricette = new ArrayList<>();
        String sql =
                "SELECT id, titolo, immagine, procedimento, link_originale, tempo_preparazione " +
                        "FROM ricette WHERE titolo LIKE ?";

        try (PreparedStatement statement = getConnection().prepareStatement(sql)) {
            statement.setString(1, "%" + nome + "%");
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Ricetta ricetta = new Ricetta(
                        rs.getInt("id"),
                        rs.getString("titolo"),
                        rs.getString("immagine"),
                        rs.getString("procedimento"),
                        rs.getString("link_originale"),
                        getIngredienti(rs.getInt("id")),
                        rs.getString("tempo_preparazione")
                );
                ricette.add(ricetta);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ricette;
    }

    // Cerca ricette tramite ingredienti
    public static List<Ricetta> cercaRicettePerIngredienti(String[] ingredienti) throws SQLException {
        StringBuilder query = new StringBuilder(
                "SELECT r.id, r.titolo, r.procedimento, r.tempo_preparazione, r.link_originale, r.immagine, " +
                        "GROUP_CONCAT(DISTINCT i.nome ORDER BY i.nome ASC SEPARATOR ', ') AS ingredienti " +
                        "FROM ricette r " +
                        "JOIN ricetta_ingredienti ri ON r.id = ri.ricetta_id " +
                        "JOIN ingredienti i ON ri.ingrediente_id = i.id " +
                        "WHERE r.id IN (" +
                        "    SELECT DISTINCT r1.id " +
                        "    FROM ricette r1 " +
                        "    JOIN ricetta_ingredienti ri1 ON r1.id = ri1.ricetta_id " +
                        "    JOIN ingredienti i1 ON ri1.ingrediente_id = i1.id " +
                        "    WHERE LOWER(i1.nome) IN (");

        for (int i = 0; i < ingredienti.length; i++) {
            query.append("?").append(i < ingredienti.length - 1 ? ", " : "");
        }
        query.append(")) GROUP BY r.id");

        try (PreparedStatement stmt = getConnection().prepareStatement(query.toString())) {
            for (int i = 0; i < ingredienti.length; i++) {
                stmt.setString(i + 1, ingredienti[i].trim().toLowerCase());
            }

            ResultSet rs = stmt.executeQuery();
            Set<Ricetta> ricette = new HashSet<>(); // Usa un Set per evitare duplicati
            while (rs.next()) {
                Ricetta ricetta = new Ricetta(rs.getInt("id"));
                ricetta.setTitolo(rs.getString("titolo"));
                ricetta.setImmagine(rs.getString("immagine"));
                ricetta.setProcedimento(rs.getString("procedimento"));
                ricetta.setLinkOriginale(rs.getString("link_originale"));
                ricetta.setTempoPreparazione(rs.getString("tempo_preparazione"));

                // Aggiungi tutti gli ingredienti della ricetta
                String ingredientiString = rs.getString("ingredienti");
                if (ingredientiString != null && !ingredientiString.isEmpty()) {
                    ricetta.setIngredienti(Arrays.asList(ingredientiString.split(", ")));
                } else {
                    ricetta.setIngredienti(Collections.emptyList());
                }

                ricette.add(ricetta);
            }
            return new ArrayList<>(ricette); // Converte il Set in List
        }
    }

}
