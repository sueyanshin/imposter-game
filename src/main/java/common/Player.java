package common;

import java.io.Serializable;

public class Player implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private boolean isImposter;
    private String word;
    private String hint;
    private boolean isAlive;
    private int votes;


    public Player(String name) {
        this.name = name;
        this.isAlive = true;
        this.votes = 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isImposter() {
        return isImposter;
    }

    public void setImposter(boolean imposter) {
        isImposter = imposter;
    }

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getHint() {
        return hint;
    }

    public void setHint(String hint) {
        this.hint = hint;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean alive) {
        isAlive = alive;
    }

    public int getVotes() {
        return votes;
    }

    public void setVotes(int votes) {
        this.votes = votes;
    }

    public void addVote() {
        this.votes++;
    }
}
