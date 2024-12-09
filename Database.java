import java.sql.*;

public class Database {
    static Connection connection;

    // Costruttore per inizializzare la connessione
    public Database() {
        String percorso = "jdbc:mysql://localhost:3306/botappetito";
        try {
            connection = DriverManager.getConnection(percorso, "root", "");
            if (connection != null) {
                System.out.println("Connessione avvenuta con successo");
            }
        } catch (SQLException e) {
            System.err.println("Errore durante la connessione al database: " + e.getMessage());
        }
    }

    // Metodo per chiudere la connessione
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
    public void inserisciRicetta(String titolo, String procedimento, String tempo, String link,
                                 String[] nomiIngredienti, String[] quantitaIngredienti) {
        String queryRicetta = "INSERT INTO ricette (titolo, procedimento, tempo_preparazione, link_originale) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmtRicetta = connection.prepareStatement(queryRicetta, Statement.RETURN_GENERATED_KEYS)) {
            // Inserisce la nuova ricetta
            stmtRicetta.setString(1, titolo);
            stmtRicetta.setString(2, procedimento);
            stmtRicetta.setString(3, tempo);
            stmtRicetta.setString(4, link);
            stmtRicetta.executeUpdate();

            // Ottieni l'ID della ricetta appena inserita
            ResultSet generatedKeys = stmtRicetta.getGeneratedKeys();
            if (generatedKeys.next()) {
                int ricettaId = generatedKeys.getInt(1);

                // Aggiungiamo gli ingredienti alla tabella ingredienti e alla tabella ricetta_ingredienti
                for (int i = 0; i < nomiIngredienti.length; i++) {
                    int ingredienteId = inserisciIngrediente(nomiIngredienti[i]); // Inserisce o recupera l'ID dell'ingrediente
                    inserisciRelazioneRicettaIngrediente(ricettaId, ingredienteId, quantitaIngredienti[i]);
                }
            }
            System.out.println("Ricetta inserita con successo.");
        } catch (SQLException e) {
            System.err.println("Errore nell'inserimento della ricetta: " + e.getMessage());
        }
    }

    // Metodo per aggiungere un nuovo ingrediente o recuperare il suo ID
    private int inserisciIngrediente(String nomeIngrediente) {
        String querySelect = "SELECT id FROM ingredienti WHERE nome = ?";
        String queryInsert = "INSERT INTO ingredienti (nome) VALUES (?)";
        try (PreparedStatement stmtSelect = connection.prepareStatement(querySelect)) {
            stmtSelect.setString(1, nomeIngrediente);
            ResultSet resultSet = stmtSelect.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("id"); // L'ingrediente esiste già, restituiamo il suo ID
            } else {
                try (PreparedStatement stmtInsert = connection.prepareStatement(queryInsert, Statement.RETURN_GENERATED_KEYS)) {
                    stmtInsert.setString(1, nomeIngrediente);
                    stmtInsert.executeUpdate();
                    ResultSet generatedKeys = stmtInsert.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1); // Restituiamo l'ID dell'ingrediente appena creato
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore nell'inserimento dell'ingrediente: " + e.getMessage());
        }
        return -1; // Errore, ID non trovato
    }

    // Metodo per inserire una relazione nella tabella ricetta_ingredienti
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

    // Metodo per ottenere le ricette preferite di un utente
    public String getRicettePreferite(String userId) {
        StringBuilder result = new StringBuilder();
        String query = "SELECT r.titolo, i.nome, ri.quantita FROM ricette r " +
                "JOIN preferiti p ON r.id = p.ricetta_id " +
                "JOIN ricetta_ingredienti ri ON r.id = ri.ricetta_id " +
                "JOIN ingredienti i ON ri.ingrediente_id = i.id " +
                "WHERE p.user_id = ? ORDER BY r.titolo";

        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, userId);
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    result.append("Titolo: ").append(resultSet.getString("titolo")).append("\n")
                            .append("- Ingrediente: ").append(resultSet.getString("nome"))
                            .append(" (Quantità: ").append(resultSet.getString("quantita")).append(")\n\n");
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore nel recupero delle ricette preferite: " + e.getMessage());
        }
        return result.toString();
    }

    // Metodo per aggiungere una ricetta ai preferiti di un utente
    public void aggiungiPreferito(String userId, int ricettaId) {
        String query = "INSERT INTO preferiti (user_id, ricetta_id) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, userId);
            stmt.setInt(2, ricettaId);
            stmt.executeUpdate();
            System.out.println("Ricetta aggiunta ai preferiti.");
        } catch (SQLException e) {
            System.err.println("Errore nell'aggiungere la ricetta ai preferiti: " + e.getMessage());
        }
    }

    // Metodo per rimuovere una ricetta dai preferiti di un utente
    public void rimuoviPreferito(String userId, int ricettaId) {
        String query = "DELETE FROM preferiti WHERE user_id = ? AND ricetta_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, userId);
            stmt.setInt(2, ricettaId);
            stmt.executeUpdate();
            System.out.println("Ricetta rimossa dai preferiti.");
        } catch (SQLException e) {
            System.err.println("Errore nella rimozione della ricetta dai preferiti: " + e.getMessage());
        }
    }
}
