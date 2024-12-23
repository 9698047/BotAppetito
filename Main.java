import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(new BotAppetitoBot());
            System.out.println("Il bot è attivo e in ascolto...");

            Database db = new Database();

            // Chiedi all'utente se vuole avviare lo scraper
            Scanner scanner = new Scanner(System.in);
            System.out.println("Vuoi avviare lo scraper? (si/no)");
            String risposta = scanner.nextLine().trim().toLowerCase();

            if (risposta.equals("si")) {
                db.cancellaTuttiIDati();
                db.resetAutoIncrement();
                String baseUrl = "https://www.gnamgnam.it";
                // Avvia lo scraper
                System.out.println("Avvio dello scraper...");
                GnamGnamScraper.extractCategoriesAndRecipes(baseUrl);
            } else {
                System.out.println("Scraper non avviato.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
