import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.*;

public class BotAppetitoBot extends TelegramLongPollingBot {
    public String getBotUsername() {
        return "BotAppetito";
    }

    @Override
    public String getBotToken() {
        try {
            String token = new String(Files.readAllBytes(Paths.get("token.txt"))).trim();
            return token;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        System.out.println("Update ricevuto: " + update);

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText().trim();
            long chatId = update.getMessage().getChatId();

            if (messageText.equals("/start")) {
                sendWelcomeMessageWithButtons(chatId);
            } else if (messageText.equals("Cerca Ricetta con nome")) {
                sendResponse(chatId, "Perfetto! Inserisci il nome della ricetta da cercare.");
            } else if (messageText.equals("Cerca Ricetta con ingredienti")) {
                sendResponse(chatId, "Ottimo! Inserisci gli ingredienti separati da virgola.");
            } else if (messageText.contains(",")) {
                // Se contiene una virgola, ricerca ricette per ingredienti
                try {
                    searchRecipesByIngredients(chatId, messageText);
                } catch (SQLException e) {
                    e.printStackTrace();
                    sendResponse(chatId, "❌ Si è verificato un errore durante la ricerca delle ricette.");
                }
            } else {
                // Se non contiene una virgola, ricerca per nome della ricetta
                cercaRicetta(chatId, messageText);
            }
        }

        if (update.hasCallbackQuery() && update.getCallbackQuery().getData() != null) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            handleCallbackQuery(callbackData, chatId);
        }
    }

    private void handleCallbackQuery(String callbackData, long chatId) {
        System.out.println("Gestendo callback con dato: " + callbackData);

        if (callbackData.startsWith("add_favorite_")) {
            int ricettaId = Integer.parseInt(callbackData.split("_")[2]);
            String userId = String.valueOf(chatId);

            boolean success = DatabaseManager.aggiungiPreferiti(userId, ricettaId);

            if (success) {
                sendResponse(chatId, "✅ Ricetta aggiunta ai tuoi preferiti!");
            } else {
                sendResponse(chatId, "❌ Errore durante l'aggiunta ai preferiti.");
            }
        } else if (callbackData.startsWith("remove_favorite_")) {
            int ricettaId = Integer.parseInt(callbackData.split("_")[2]);
            String userId = String.valueOf(chatId);

            boolean success = DatabaseManager.rimuoviPreferiti(userId, ricettaId);

            if (success) {
                sendResponse(chatId, "❌ Ricetta rimossa dai tuoi preferiti.");
            } else {
                sendResponse(chatId, "❌ Errore durante la rimozione dai preferiti.");
            }
        } else {
            switch (callbackData) {
                case "Cerca_Ricetta_con_nome":
                    sendResponse(chatId, "Perfetto! Inserisci il nome della ricetta da cercare.");
                    break;
                case "Cerca_Ricetta_tramite_ingredienti":
                    sendResponse(chatId, "Ottimo! Inserisci gli ingredienti separati da virgola.");
                    break;
                case "Ricetta_Random":
                    sendRandomRecipe(chatId);
                    break;
                case "Mostra_Preferiti":
                    mostraPreferiti(chatId);
                    break;
                default:
                    sendResponse(chatId, "Mi dispiace, non ho capito la tua richiesta.");
            }
        }
    }

    private void mostraPreferiti(long chatId) {
        String userId = String.valueOf(chatId);
        List<Ricetta> preferiti = DatabaseManager.getPreferitiByUser(userId);

        if (preferiti.isEmpty()) {
            sendResponse(chatId, "Non hai ancora aggiunto ricette ai preferiti.");
        } else {
            for (Ricetta ricetta : preferiti) {
                StringBuilder preferitiDetails = new StringBuilder();
                preferitiDetails.append("🍽 *Titolo*: ").append(escapeMarkdown(ricetta.getTitolo())).append("\n");

                List<String> ingredienti = DatabaseManager.getIngredienti(ricetta.getId());
                if (!ingredienti.isEmpty()) {
                    preferitiDetails.append("🛒 *Ingredienti*: ").append(String.join(", ", ingredienti)).append("\n");
                } else {
                    preferitiDetails.append("🛒 Nessun ingrediente trovato.\n");
                }

                preferitiDetails.append("⏱ *Tempo di preparazione*: ").append(ricetta.getTempoPreparazione()).append("\n");
                preferitiDetails.append("📖 *Procedimento*: ").append(escapeMarkdown(ricetta.getProcedimento())).append("\n\n");
                preferitiDetails.append("🔗 [Link alla ricetta originale](").append(ricetta.getLinkOriginale()).append(")\n\n");

                sendRecipeWithPhoto(chatId, ricetta.getImmagine(), preferitiDetails.toString(), ricetta.getId(), true);
            }
        }
    }


    private void sendRandomRecipe(long chatId) {
        System.out.println("Invio richiesta di ricetta randomica per la chatId: " + chatId);
        Ricetta ricetta = DatabaseManager.getRandomRecipeWithDetails();

        if (ricetta != null) {
            StringBuilder recipeDetails = new StringBuilder();
            recipeDetails.append("🍽 *Titolo*: ").append(escapeMarkdown(ricetta.getTitolo())).append("\n");

            List<String> ingredienti = ricetta.getIngredienti();
            if (!ingredienti.isEmpty()) {
                recipeDetails.append("🛒 *Ingredienti*: ").append(String.join(", ", ingredienti)).append("\n");
            } else {
                recipeDetails.append("🛒 Nessun ingrediente trovato.\n");
            }

            recipeDetails.append("⏱ *Tempo di preparazione*: ").append(ricetta.getTempoPreparazione()).append("\n");
            recipeDetails.append("📖 *Procedimento*: ").append(escapeMarkdown(ricetta.getProcedimento())).append("\n\n");
            recipeDetails.append("🔗 [Link alla ricetta originale](").append(ricetta.getLinkOriginale()).append(")");

            sendRecipeWithPhoto(chatId, ricetta.getImmagine(), recipeDetails.toString(), ricetta.getId(), true);
        } else {
            sendResponse(chatId, "Non ho trovato alcuna ricetta randomica.");
        }
    }

    private void sendResponse(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendWelcomeMessageWithButtons(long chatId) {
        SendPhoto message = new SendPhoto();
        message.setChatId(String.valueOf(chatId));
        message.setCaption("Benvenuto su BotAppetito, il tuo bot di cucina numero 1! Scegli un'opzione:");

        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton i1 = new InlineKeyboardButton("Cerca Ricetta dal nome");
        i1.setCallbackData("Cerca_Ricetta_con_nome");

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton i2 = new InlineKeyboardButton("Cerca Ricetta tramite gli ingredienti");
        i2.setCallbackData("Cerca_Ricetta_tramite_ingredienti");

        row1.add(i1);
        row3.add(i2);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton i3 = new InlineKeyboardButton("Ricetta Randomica");
        i3.setCallbackData("Ricetta_Random");
        InlineKeyboardButton i4 = new InlineKeyboardButton("Mostra Preferiti");
        i4.setCallbackData("Mostra_Preferiti");
        row2.add(i3);
        row2.add(i4);

        rowList.add(row1);
        rowList.add(row3);
        rowList.add(row2);

        keyboardMarkup.setKeyboard(rowList);
        message.setReplyMarkup(keyboardMarkup);

        File imageFile = new File("BotAppetito.png");
        // Verifica se il file esiste prima di inviarlo
        if (imageFile.exists()) {
            message.setPhoto(new InputFile(imageFile));
        } else {
            System.out.println("Errore: Il file immagine non è stato trovato.");
            return;
        }

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendRecipeWithPhoto(long chatId, String photoPath, String recipeDetails, int ricettaId, boolean isFirstPart) {
        // Limita la lunghezza della didascalia a 1024 caratteri
        int maxCaptionLength = 1024;
        String caption = recipeDetails;

        // Se il messaggio è troppo lungo, lo dividiamo in più parti
        if (caption.length() > maxCaptionLength) {
            int numParts = (int) Math.ceil((double) caption.length() / maxCaptionLength);
            for (int i = 0; i < numParts; i++) {
                int start = i * maxCaptionLength;
                int end = Math.min(start + maxCaptionLength, caption.length());
                String part = caption.substring(start, end);

                // Calcola se è l'ultima parte
                boolean isLastPart = (i == numParts - 1);

                // Invia la parte del messaggio
                sendPartOfRecipe(chatId, photoPath, part, ricettaId, i == 0, isLastPart); // Passa entrambi i booleani
            }
        } else {
            // Se il messaggio non è troppo lungo, inviamo tutto in un unico messaggio
            // Passa true per isLastPart, poiché c'è una sola parte (è l'ultima parte anche in questo caso)
            sendPartOfRecipe(chatId, photoPath, caption, ricettaId, isFirstPart, true); // Sempre aggiungi i tasti alla fine
        }
    }




    private void sendPartOfRecipe(long chatId, String photoPath, String recipeDetails, int ricettaId, boolean isFirstPart, boolean isLastPart) {
        if (isFirstPart) {
            SendPhoto photoMessage = new SendPhoto();
            photoMessage.setChatId(String.valueOf(chatId));

            // Se il percorso è un URL valido
            if (photoPath != null && !photoPath.isEmpty()) {
                if (isValidURL(photoPath)) {
                    photoMessage.setPhoto(new InputFile(photoPath)); // Usa l'URL se valido
                }
                // Se il path è un file locale
                else if (new java.io.File(photoPath).exists()) {
                    photoMessage.setPhoto(new InputFile(new java.io.File(photoPath)));
                }
                else {
                    photoMessage.setPhoto(new InputFile("BotAppetito.png"));
                }
            }
            else {
                photoMessage.setPhoto(new InputFile("BotAppetito.png"));
            }

            // Limita la lunghezza del messaggio
            String caption = recipeDetails;
            if (caption.length() > 1024) {
                caption = caption.substring(0, 1020) + "...";
            }

            photoMessage.setCaption(caption);

            // Se è la prima e ultima parte, aggiungiamo i tasti dei preferiti
            if (isFirstPart && isLastPart) {
                photoMessage.setReplyMarkup(createFavoriteButtons(ricettaId));
            }

            try {
                execute(photoMessage);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        } else {
            // Seconda parte del messaggio: invia solo il testo senza foto
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));

            // Limita la lunghezza del testo
            String caption = recipeDetails;
            if (caption.length() > 1024) {
                caption = caption.substring(0, 1020) + "...";
            }

            message.setText(caption);

            // Se è l'ultima parte del messaggio, aggiungi i tasti dei preferiti
            if (isLastPart) {
                message.setReplyMarkup(createFavoriteButtons(ricettaId));
            }

            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }


    private InlineKeyboardMarkup createFavoriteButtons(int ricettaId) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowList = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();

        InlineKeyboardButton addToFavoritesButton = new InlineKeyboardButton("Aggiungi ai Preferiti");
        addToFavoritesButton.setCallbackData("add_favorite_" + ricettaId);

        InlineKeyboardButton removeFromFavoritesButton = new InlineKeyboardButton("Rimuovi dai Preferiti");
        removeFromFavoritesButton.setCallbackData("remove_favorite_" + ricettaId);

        row1.add(addToFavoritesButton);
        row1.add(removeFromFavoritesButton);

        rowList.add(row1);
        keyboardMarkup.setKeyboard(rowList);

        return keyboardMarkup;
    }



    private boolean isValidURL(String url) {
        try {
            new java.net.URL(url).toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }



    private void cercaRicetta(long chatId, String nomeRicetta) {
        List<Ricetta> ricetteTrovate = DatabaseManager.cercaRicettaPerNome(nomeRicetta);

        if (ricetteTrovate.isEmpty()) {
            sendResponse(chatId, "Mi dispiace, non ho trovato ricette con quel nome.");
        } else {
            // HashMap per tracciare le ricette già inviate
            Map<String, Ricetta> ricetteInviate = new HashMap<>();

            for (Ricetta ricetta : ricetteTrovate) {
                // Controlla se la ricetta è già stata inviata
                if (ricetteInviate.containsKey(ricetta.getTitolo())) {
                    continue; // Salta la ricetta se è già stata inviata
                }

                StringBuilder recipeDetails = new StringBuilder();
                recipeDetails.append("🍽 *Titolo*: ").append(escapeMarkdown(ricetta.getTitolo())).append("\n");

                List<String> ingredienti = DatabaseManager.getIngredienti(ricetta.getId());
                if (!ingredienti.isEmpty()) {
                    recipeDetails.append("🛒 *Ingredienti*: ").append(String.join(", ", ingredienti)).append("\n");
                } else {
                    recipeDetails.append("🛒 Nessun ingrediente trovato.\n");
                }

                recipeDetails.append("⏱ *Tempo di preparazione*: ").append(ricetta.getTempoPreparazione()).append("\n");
                recipeDetails.append("📖 *Procedimento*: ").append(escapeMarkdown(ricetta.getProcedimento())).append("\n\n");
                recipeDetails.append("🔗 [Link alla ricetta originale](").append(ricetta.getLinkOriginale()).append(")");

                sendRecipeWithPhoto(chatId, ricetta.getImmagine(), recipeDetails.toString(), ricetta.getId(), true);

                // Aggiungi la ricetta alla HashMap per evitare duplicati
                ricetteInviate.put(ricetta.getTitolo(), ricetta);
            }
        }
    }


    private String escapeMarkdown(String text) {
        return text.replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]");
    }
    private void searchRecipesByIngredients(long chatId, String ingredientsInput) throws SQLException {
        // Rimuovi spazi extra e dividi gli ingredienti
        String[] ingredientsArray = Arrays.stream(ingredientsInput.split(","))
                .map(String::trim)
                .toArray(String[]::new);

        if (ingredientsArray.length == 0 || ingredientsArray[0].isEmpty()) {
            sendResponse(chatId, "❌ Non hai fornito ingredienti validi. Riprova separandoli con una virgola.");
            return;
        }

        // Cerca le ricette nel database
        List<Ricetta> foundRecipes = DatabaseManager.cercaRicettePerIngredienti(ingredientsArray);

        if (foundRecipes.isEmpty()) {
            sendResponse(chatId, "❌ Non ho trovato ricette con gli ingredienti forniti.");
        } else {
            Set<String> sentRecipeNames = new HashSet<>(); // Set per tracciare le ricette già inviate

            for (Ricetta ricetta : foundRecipes) {
                // Controlla se la ricetta è già stata inviata
                if (sentRecipeNames.contains(ricetta.getTitolo())) {
                    continue; // Salta la ricetta se è già stata inviata
                }

                StringBuilder recipeDetails = new StringBuilder();
                recipeDetails.append("🍽 *Titolo*: ").append(escapeMarkdown(ricetta.getTitolo())).append("\n");

                List<String> ingredientiRicetta = ricetta.getIngredienti();
                if (!ingredientiRicetta.isEmpty()) {
                    recipeDetails.append("🛒 *Ingredienti*: ").append(String.join(", ", ingredientiRicetta)).append("\n");
                } else {
                    recipeDetails.append("🛒 Nessun ingrediente trovato.\n");
                }

                recipeDetails.append("⏱ *Tempo di preparazione*: ").append(ricetta.getTempoPreparazione()).append("\n");
                recipeDetails.append("📖 *Procedimento*: ").append(escapeMarkdown(ricetta.getProcedimento())).append("\n\n");
                recipeDetails.append("🔗 [Link alla ricetta originale](").append(ricetta.getLinkOriginale()).append(")\n\n");

                sendRecipeWithPhoto(chatId, ricetta.getImmagine(), recipeDetails.toString(), ricetta.getId(), true);

                // Aggiungi il nome della ricetta al Set per evitare duplicati
                sentRecipeNames.add(ricetta.getTitolo());
            }
        }
    }

}
