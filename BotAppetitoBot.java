import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

public class BotAppetitoBot extends TelegramLongPollingBot {

    @Override
    public void onUpdateReceived(Update update) {
        // Controlla se il messaggio contiene testo
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText(); // Testo del messaggio ricevuto
            long chatId = update.getMessage().getChatId(); // Chat ID dell'utente

            // Rispondi con un messaggio personalizzato
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId)); // Chat ID del destinatario
            message.setText("Ciao! Hai scritto: " + messageText); // Testo della risposta


            //prova per i bottoni
            InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rowList = new ArrayList<>();

            // Prima riga di bottoni
            List<InlineKeyboardButton> row1 = new ArrayList<>();
            InlineKeyboardButton button1 = new InlineKeyboardButton();
            button1.setText("Cerca Ricetta con nome");
            button1.setCallbackData("Cerca_Ricetta_con_nome");

            InlineKeyboardButton button2 = new InlineKeyboardButton();
            button2.setText("Cerca Ricetta tramite ingredienti");
            button2.setCallbackData("Cerca_Ricetta_tramite_ingredienti");

            row1.add(button1);
            row1.add(button2);

            // Seconda riga con un link
            List<InlineKeyboardButton> row2 = new ArrayList<>();
            InlineKeyboardButton linkButton = new InlineKeyboardButton();
            linkButton.setText("Vai al sito");
            linkButton.setUrl("https://www.google.com"); // URL del sito

            row2.add(linkButton);

            rowList.add(row1);
            rowList.add(row2);

            keyboardMarkup.setKeyboard(rowList);
            message.setReplyMarkup(keyboardMarkup);


            try {
                execute(message); // Esegui il comando per inviare il messaggio
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
