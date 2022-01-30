package Misc;

import java.util.Comparator;

public class BigramNPMI {
    private String first;
    private String second;
    private double npmi;


    public BigramNPMI(String first, String second, double npmi) {
        this.first = first;
        this.second = second;
        this.npmi = npmi;
    }

    public String getFirst() {
        return first;
    }

    public String getSecond() {
        return second;
    }

    public double getNpmi() {
        return npmi;
    }

    public boolean isCollocation(double minPmi, double relMinPmi, double sumMinPmi){
        if(minPmi > npmi){
            return false;
        }
        if(relMinPmi > npmi/sumMinPmi){
            return false;
        }
        return true;
    }

    public static class BigramComparator implements Comparator<BigramNPMI> {
        public int compare(BigramNPMI bigram1, BigramNPMI bigram2){ // compering in reverse order
            return Double.compare(bigram2.getNpmi(),bigram1.getNpmi());
        }
    }
}
