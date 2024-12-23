import java.util.List;

public class Ricetta {
    private int id;
    private String titolo;
    private String immagine;
    private String procedimento;
    private String linkOriginale;
    private List<String> ingredienti;
    private String tempoPreparazione;

    public Ricetta(int id, String titolo, String immagine, String procedimento, String linkOriginale, List<String> ingredienti, String tempoPreparazione) {
        this.id = id;
        this.titolo = titolo;
        this.immagine = immagine;
        this.procedimento = procedimento;
        this.linkOriginale = linkOriginale;
        this.ingredienti = ingredienti;
        this.tempoPreparazione = tempoPreparazione;
    }
    public Ricetta(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getTitolo() {
        return titolo;
    }

    public String getImmagine() {
        return immagine;
    }

    public String getProcedimento() {
        return procedimento;
    }

    public String getLinkOriginale() {
        return linkOriginale;
    }

    public List<String> getIngredienti() {
        return ingredienti;
    }

    public String getTempoPreparazione() {
        return tempoPreparazione;
    }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }

    public void setImmagine(String immagine) {
        this.immagine = immagine;
    }

    public void setProcedimento(String procedimento) {
        this.procedimento = procedimento;
    }
    public void setLinkOriginale(String linkOriginale) {
        this.linkOriginale = linkOriginale;
    }
    public void setIngredienti(List<String> ingredienti) {
        this.ingredienti = ingredienti;
    }

    public void setTempoPreparazione(String tempoPreparazione) {
        this.tempoPreparazione = tempoPreparazione;
    }
}
