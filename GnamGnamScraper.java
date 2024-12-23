import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GnamGnamScraper {
    private static Database database = new Database();

    public static void extractCategoriesAndRecipes(String baseUrl) {
        try {
            Document categoryPageDoc = Jsoup.connect(baseUrl).get();
            Elements categoryLinks = categoryPageDoc.select("a[href^='/categoria/']");

            for (Element categoryLink : categoryLinks) {
                String relativeCategoryUrl = categoryLink.attr("href");
                String categoryUrl = baseUrl + relativeCategoryUrl;
                System.out.println("Elaborazione categoria: " + categoryUrl);
                extractRecipesFromCategory(categoryUrl);
            }
        } catch (IOException e) {
            System.err.println("Errore nel recuperare la pagina delle categorie: " + e.getMessage());
        }
    }

    public static void extractRecipesFromCategory(String categoryUrl) {
        try {
            String currentPageUrl = categoryUrl;
            while (currentPageUrl != null) {
                Document categoryDoc = Jsoup.connect(currentPageUrl).get();
                Elements recipeLinks = categoryDoc.select("a.art-box-recipe");

                for (Element recipeLink : recipeLinks) {
                    String recipeUrl = recipeLink.attr("href");
                    extractRecipeData(recipeUrl);
                }

                Element nextPageLink = categoryDoc.select("a.nextpostslink").first();
                currentPageUrl = nextPageLink != null ? nextPageLink.attr("href") : null;
            }
        } catch (IOException e) {
            System.err.println("Errore nel recuperare la pagina della categoria: " + e.getMessage());
        }
    }

    public static void extractRecipeData(String recipeUrl) {
        try {
            Document recipeDoc = Jsoup.connect(recipeUrl).get();

            // Estrazione titolo
            String title = recipeDoc.select("h1.title-solo").text();

            // Estrazione ingredienti
            List<String> ingredients = new ArrayList<>();
            Elements ingredientElements = recipeDoc.select("div.art-box-ingredienti ul li span[itemprop=recipeIngredient]");
            for (Element ingredient : ingredientElements) {
                ingredients.add(ingredient.text());
            }

            // Estrazione procedura
            StringBuilder procedure = new StringBuilder();
            Elements procedureElements = recipeDoc.select("table.art-table-processo tbody tr td p[itemprop=recipeInstructions]");
            for (Element step : procedureElements) {
                procedure.append(step.text()).append("\n");
            }

            // Estrazione tempo di preparazione
            String prepTime = recipeDoc.select("div.value[itemprop=prepTime]").text();

            // Estrazione immagine
            String imageUrl = extractImageUrl(recipeDoc);

            // Salvataggio dei dati nel database
            database.inserisciRicetta(title, procedure.toString(), prepTime, recipeUrl, imageUrl, ingredients);

        } catch (IOException e) {
            System.err.println("Errore nel recuperare la pagina della ricetta: " + e.getMessage());
        }
    }

    // Metodo per estrarre l'URL dell'immagine dalla pagina della ricetta
    private static String extractImageUrl(Document recipeDoc) {
        // Cerca il tag e restituisci l'URL dell'immagine
        Elements imageElements = recipeDoc.select("link[rel=preload][as=image]");
        if (!imageElements.isEmpty()) {
            return imageElements.first().attr("href");
        }
        return null;
    }
}
