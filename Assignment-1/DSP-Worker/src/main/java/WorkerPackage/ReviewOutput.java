package WorkerPackage;

public class ReviewOutput {
    private String link;
    private int sentimentLvl;
    private String entities;
    private int sarcasmLvl;


    public ReviewOutput() {
        this.link = "";
        this.sentimentLvl = 0;
        this.entities = "";
        this.sarcasmLvl = 0;
    }

    public ReviewOutput(String link, int sentimentLvl, String entities, int sarcasmLvl) {
        this.link = link;
        this.sentimentLvl = sentimentLvl;
        this.entities = entities;
        this.sarcasmLvl = sarcasmLvl;
    }

    public int getSentimentLvl() {
        return sentimentLvl;
    }

    public String getEntities() {
        return entities;
    }

    public int getSarcasmLvl() {
        return sarcasmLvl;
    }

    public String getLink() {
        return link;
    }

    public void setSentimentLvl(int sentimentLvl) {
        this.sentimentLvl = sentimentLvl;
    }

    public void setEntities(String entities) {
        this.entities = entities;
    }

    public void setSarcasmLvl(int sarcasmLvl) {
        this.sarcasmLvl = sarcasmLvl;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
