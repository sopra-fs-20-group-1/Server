package ch.uzh.ifi.seal.soprafs20.rest.dto;

import ch.uzh.ifi.seal.soprafs20.constant.TournamentState;

public class TournamentPostDTO {

    private String tournamentName;
    private TournamentState tournamentState;
    private int breakDuration;
    private int gameDuration;
    private String startTime;
    private int numberTables;
    private int amountOfPlayers;
    private long managerId;
    private String informationBox;
    private String location;

    public int getBreakDuration() {
        return breakDuration;
    }
    public String getTournamentName() {
        return tournamentName;
    }
    public int getGameDuration() {
        return gameDuration;
    }
    public String getStartTime() {
        return startTime;
    }
    public int getNumberTables() {
        return numberTables;
    }
    public int getAmountOfPlayers() {
        return amountOfPlayers;
    }
    public long getManagerId() {
        return managerId;
    }
    public String getInformationBox() {
        return informationBox;
    }
    public String getLocation() {
        return location;
    }
    public TournamentState getTournamentState() {
        return tournamentState;
    }

    public void setBreakDuration(int breakDuration) {
        this.breakDuration = breakDuration;
    }
    public void setTournamentName(String tournamentName) {
        this.tournamentName = tournamentName;
    }
    public void setGameDuration(int gameDuration) {
        this.gameDuration = gameDuration;
    }
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }
    public void setNumberTables(int numberTables) {
        this.numberTables = numberTables;
    }
    public void setAmountOfPlayers(int amountOfPlayers) {
        this.amountOfPlayers = amountOfPlayers;
    }
    public void setManagerId(long managerId) {
        this.managerId = managerId;
    }
    public void setInformationBox(String informationBox) {
        this.informationBox = informationBox;
    }
    public void setLocation(String location) {
        this.location = location;
    }
    public void setTournamentState(TournamentState tournamentState) {
        this.tournamentState = tournamentState;
    }
}
