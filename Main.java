import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) {
        try {
            // Inizializza l'API Telegram
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

            // Registra il bot
            botsApi.registerBot(new BotAppetitoBot());

            System.out.println("Il bot è attivo e in ascolto...");
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}

