import java.sql.*;
import java.util.List;

public class Database {
    private static final String URL = "jdbc:mysql://localhost:3306/botappetito";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    static Connection connection;

    public Database() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            if (connection != null) {
                System.out.println("Connessione avvenuta con successo");
            }
        } catch (SQLException e) {
            System.err.println("Errore durante la connessione al database: " + e.getMessage());
        }
    }

    public static void closeConnection() {
        try {
            if (connection != null) {
                connection.close();
                System.out.println("Connessione chiusa.");
            }
        } catch (SQLException e) {
            System.err.println("Errore nella chiusura della connessione: " + e.getMessage());
        }
    }

    // Metodo per inserire una nuova ricetta nel database
    public void inserisciRicetta(String titolo, String procedimento, String tempo, String link, String immagine,List<String> nomiIngredienti) {
        String queryRicetta = "INSERT INTO ricette (titolo, procedimento, tempo_preparazione, link_originale) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmtRicetta = connection.prepareStatement(queryRicetta, Statement.RETURN_GENERATED_KEYS)) {
            // Inserisci la nuova ricetta senza immagine
            stmtRicetta.setString(1, titolo);
            stmtRicetta.setString(2, procedimento);
            stmtRicetta.setString(3, tempo);
            stmtRicetta.setString(4, link);
            stmtRicetta.executeUpdate();

            // Ottieni l'ID della ricetta appena inserita
            ResultSet generatedKeys = stmtRicetta.getGeneratedKeys();
            if (generatedKeys.next()) {
                int ricettaId = generatedKeys.getInt(1);

                // Aggiorna la ricetta con l'immagine (se disponibile)
                if (immagine != null && !immagine.isEmpty()) {
                    String queryUpdate = "UPDATE ricette SET immagine = ? WHERE id = ?";
                    try (PreparedStatement stmtUpdate = connection.prepareStatement(queryUpdate)) {
                        stmtUpdate.setString(1, immagine);
                        stmtUpdate.setInt(2, ricettaId);
                        stmtUpdate.executeUpdate();
                    }
                }

                // Aggiungi gli ingredienti alla tabella ingredienti e alla tabella ricetta_ingredienti
                for (String nomeIngrediente : nomiIngredienti) {
                    int ingredienteId = inserisciIngrediente(nomeIngrediente);
                    inserisciRelazioneRicettaIngrediente(ricettaId, ingredienteId, "q.b.");
                }
            }
            System.out.println("Ricetta inserita con successo.");
        } catch (SQLException e) {
            System.err.println("Errore nell'inserimento della ricetta: " + e.getMessage());
        }
    }


    private int inserisciIngrediente(String nomeIngrediente) {
        String querySelect = "SELECT id FROM ingredienti WHERE nome = ?";
        String queryInsert = "INSERT INTO ingredienti (nome) VALUES (?)";
        try (PreparedStatement stmtSelect = connection.prepareStatement(querySelect)) {
            stmtSelect.setString(1, nomeIngrediente);
            ResultSet resultSet = stmtSelect.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("id");
            } else {
                try (PreparedStatement stmtInsert = connection.prepareStatement(queryInsert, Statement.RETURN_GENERATED_KEYS)) {
                    stmtInsert.setString(1, nomeIngrediente);
                    stmtInsert.executeUpdate();
                    ResultSet generatedKeys = stmtInsert.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore nell'inserimento dell'ingrediente: " + e.getMessage());
        }
        return -1;
    }

    private void inserisciRelazioneRicettaIngrediente(int ricettaId, int ingredienteId, String quantita) {
        String query = "INSERT INTO ricetta_ingredienti (ricetta_id, ingrediente_id, quantita) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, ricettaId);
            stmt.setInt(2, ingredienteId);
            stmt.setString(3, quantita);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Errore nell'inserimento della relazione ricetta-ingrediente: " + e.getMessage());
        }
    }

    public void cancellaTuttiIDati() {
        String deleteRicettaIngrediente = "DELETE FROM ricetta_ingredienti";
        String deletePreferiti = "DELETE FROM preferiti";
        String deleteRicette = "DELETE FROM ricette";
        String deleteIngredienti = "DELETE FROM ingredienti";

        try (Statement stmt = connection.createStatement()) {
            // Cancella tutti i dati dalle tabelle
            stmt.executeUpdate(deleteRicettaIngrediente);
            stmt.executeUpdate(deletePreferiti);
            stmt.executeUpdate(deleteRicette);
            stmt.executeUpdate(deleteIngredienti);

            System.out.println("Dati cancellati con successo.");
        } catch (SQLException e) {
            System.err.println("Errore nella cancellazione dei dati: " + e.getMessage());
        }
    }

    public void resetAutoIncrement() {
        String resetAutoIncrementRicette = "ALTER TABLE ricette AUTO_INCREMENT = 1";
        String resetAutoIncrementIngredienti = "ALTER TABLE ingredienti AUTO_INCREMENT = 1";
        String resetAutoIncrementRicettaIngrediente = "ALTER TABLE ricetta_ingredienti AUTO_INCREMENT = 1";
        String resetAutoIncrementPreferiti = "ALTER TABLE preferiti AUTO_INCREMENT = 1";

        try (Statement stmt = connection.createStatement()) {
            // Reset dei contatori
            stmt.executeUpdate(resetAutoIncrementRicette);
            stmt.executeUpdate(resetAutoIncrementIngredienti);
            stmt.executeUpdate(resetAutoIncrementRicettaIngrediente);
            stmt.executeUpdate(resetAutoIncrementPreferiti);

            System.out.println("Contatori auto-increment resettati.");
        } catch (SQLException e) {
            System.err.println("Errore nel reset dell'auto-increment: " + e.getMessage());
        }
    }

}
