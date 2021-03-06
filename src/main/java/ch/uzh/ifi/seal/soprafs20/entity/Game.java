package ch.uzh.ifi.seal.soprafs20.entity;

import ch.uzh.ifi.seal.soprafs20.constant.GameState;

import javax.persistence.*;

@Entity(name = "Game")
public class Game {

    @Id
    @GeneratedValue
    public long gameId;

    @Column
    public String startTime;

    @Column
    public GameState gameState = GameState.NOTREADY;

    @Column
    public int score1;

    @Column
    public int score2;

    @OneToOne
    public Participant participant1;

    @Column
    public boolean participant1Reported;

    @Column
    public boolean participant2Reported;

    @OneToOne
    public Participant participant2;

    @Column
    public String tournamentCode;


    public long getGameId() {
        return gameId;
    }
    public String getStartTime() {
        return startTime;
    }
    public GameState getGameState() {
        return gameState;
    }
    public int getScore1() {
        return score1;
    }
    public int getScore2() {
        return score2;
    }
    public Participant getParticipant1() {
        return participant1;
    }
    public Participant getParticipant2() {
        return participant2;
    }
    public String getTournamentCode() {
        return tournamentCode;
    }
    public boolean isParticipant1Reported() {
        return participant1Reported;
    }
    public boolean isParticipant2Reported() {
        return participant2Reported;
    }

    public void setGameId(long gameId) {
        this.gameId = gameId;
    }
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }
    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }
    public void setScore1(int score1) {
        this.score1 = score1;
    }
    public void setScore2(int score2) {
        this.score2 = score2;
    }
    public void setParticipant1(Participant participant1) {
        this.participant1 = participant1;
    }
    public void setParticipant2(Participant participant2) {
        this.participant2 = participant2;
    }
    public void setTournamentCode(String tournamentCode) {
        this.tournamentCode = tournamentCode;
    }
    public void setParticipant1Reported(boolean participant1Reported) {
        this.participant1Reported = participant1Reported;
    }
    public void setParticipant2Reported(boolean participant2Reported) {
        this.participant2Reported = participant2Reported;
    }




}

