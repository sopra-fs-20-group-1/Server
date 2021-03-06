package ch.uzh.ifi.seal.soprafs20.service;

import java.security.SecureRandom;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import ch.uzh.ifi.seal.soprafs20.constant.GameState;
import ch.uzh.ifi.seal.soprafs20.constant.PlayerState;
import ch.uzh.ifi.seal.soprafs20.constant.TournamentState;
import ch.uzh.ifi.seal.soprafs20.entity.*;
import ch.uzh.ifi.seal.soprafs20.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import javax.transaction.Transactional;

@Service
@Transactional
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final GameRepository gameRepository;
    private final BracketRepository bracketRepository;
    private final LeaderboardRepository leaderboardRepository;

    @Autowired
    public TournamentService(@Qualifier("tournamentRepository")TournamentRepository tournamentRepository,
                             @Qualifier("gameRepository") GameRepository gameRepository,
                             @Qualifier("bracketRepository") BracketRepository bracketRepository,
                             @Qualifier("leaderboardRepository") LeaderboardRepository leaderboardRepository) {
        this.tournamentRepository = tournamentRepository;
        this.gameRepository = gameRepository;
        this.bracketRepository = bracketRepository;
        this.leaderboardRepository = leaderboardRepository;
    }


    // create methods
    public Tournament createTournament(Tournament tournament){

        // 8-Character Tournament Code is generated
        tournament.setTournamentCode(generateTournamentCode());
        tournament.setTournamentState(TournamentState.ACTIVE);

        // Bracket is generated
        tournament.setBracket(createBracket(tournament.getAmountOfPlayers(),
                                            tournament.getTournamentCode(),
                                            tournament.getNumberTables(),
                                            tournament.getStartTime(),
                                            tournament.getBreakDuration(),
                                            tournament.getGameDuration()));
        // Tournament is saved
        tournamentRepository.save(tournament);
        tournamentRepository.flush();
        return tournament;
    }

    public Bracket createBracket(int numberOfPlayers, String tournamentCode, int tables, String startTime, int breakTime, int gameTime) {
        List<Game> newGames = new ArrayList<>();
        // create new Bracket
        Bracket bracket = new Bracket();

        // Insert All games
        for (int i = 1; i <= numberOfPlayers - 1; i++) {
            Game newGame = new Game();
            newGame.setTournamentCode(tournamentCode);

            newGames.add(newGame);

            gameRepository.save(newGame);
            gameRepository.flush();
        }

        calculateTimes(newGames, startTime, breakTime, gameTime, tables);

        // add the games to the bracket
        bracket.setBracketList(newGames);

        // save the bracket
        bracketRepository.save(bracket);
        bracketRepository.flush();

        return bracket;
    }

    public void createLeaderboardEntry(Participant participant, Tournament tournament) {

        if (leaderboardRepository.findAllByTournamentCode(tournament.getTournamentCode()).size() == tournament.getAmountOfPlayers()) {
            throw new ResponseStatusException(HttpStatus.LOCKED, "Tournament is already full.");
        }

        // check if participant is already in there
        if (checkIfParticipantIsInLeaderboard(participant.getLicenseNumber(), tournament.getTournamentCode())) {
            throw new ResponseStatusException(HttpStatus.NO_CONTENT);
        }

        Leaderboard leaderboard = new Leaderboard();
        leaderboard.setParticipant(participant);
        leaderboard.setWins(0);
        leaderboard.setLosses(0);
        leaderboard.setPointsScored(0);
        leaderboard.setPointsConceded(0);
        leaderboard.setPlayerState(PlayerState.ACTIVE);
        leaderboard.setTournamentCode(tournament.getTournamentCode());

        leaderboardRepository.save(leaderboard);
        leaderboardRepository.flush();
    }

    // get methods
    public Tournament getTournamentByTournamentCode(String tournamentCode) {
        return tournamentRepository.findByTournamentCode(tournamentCode);
    }

    public List<Game> getBracketByTournamentCode(String tournamentCode) {
        Tournament tournament = tournamentRepository.findByTournamentCode(tournamentCode);

        return tournament.getBracket().getBracketList();
    }

    public List<Tournament> getAllTournaments() {
        return this.tournamentRepository.findAll();
    }

    public List<Leaderboard> getLeaderboardFromTournament(String tournamentCode) {
        return leaderboardRepository.findAllByTournamentCode(tournamentCode);
    }

    // check methods
    public boolean checkIfTournamentCodeExists(String tournamentCode) {
        Tournament newTournament = tournamentRepository.findByTournamentCode(tournamentCode);
        return newTournament != null;
    }

    public boolean checkIfParticipantIsInLeaderboard(String licensenumber, String tournamentCode) {

        for (Leaderboard leaderboard : leaderboardRepository.findAllByTournamentCode(tournamentCode)) {
            if (leaderboard.getParticipant().getLicenseNumber().equals(licensenumber)) {
                return true;
            }
        }
        return false;
    }

    // update methods
    public void updateBracketWithNewParticipant(Participant participant, Tournament tournament) {

        // create an entry in the leaderboard
        createLeaderboardEntry(participant, tournament);

        // get the right bracket
        Bracket bracket = tournament.getBracket();

        // update games in bracket
        for (Game game : bracket.getBracketList()) {
            if (game.getParticipant1() == participant || game.getParticipant2() == participant) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Participant is already in the tournament");
            }
            else if (game.getParticipant1() == null) {
                game.setParticipant1(participant);
                gameRepository.save(game);
                gameRepository.flush();
                break;
            }
            else if (game.getParticipant2() == null) {
                game.setParticipant2(participant);
                gameRepository.save(game);
                gameRepository.flush();
                break;
            }
        }
        bracketRepository.save(bracket);
        bracketRepository.flush();
    }

    public void updateGameWithScore(String tournamentCode, long gameId, int score1, int score2, long participantId) {

        Game game = gameRepository.findByGameId(gameId);

        // update the score of the game
        if (game != null && game.getGameState() == GameState.FINISHED) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Game score has already been set and is locked.");
        }
        else if (game != null) {

            // check if the user even is in the game
            if (!(game.getParticipant1().getParticipantID().equals(participantId) || game.getParticipant2().getParticipantID().equals(participantId))) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Participant is not in this game!");
            }

            // check if this participant has already entered a score
            if (game.getParticipant1().getParticipantID().equals(participantId) && game.isParticipant1Reported()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A score has already been reported with this id.");
            }
            else if (game.getParticipant1().getParticipantID().equals(participantId) && !game.isParticipant1Reported()) {
                game.setParticipant1Reported(true);
            }
            else if (game.getParticipant2().getParticipantID().equals(participantId) && game.isParticipant2Reported()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "A score has already been reported with this id.");
            }
            else if (game.getParticipant2().getParticipantID().equals(participantId) && !game.isParticipant2Reported()) {
                game.setParticipant2Reported(true);
            }

            // check if first entry
            if (game.getGameState() == GameState.NOTREADY) {
                game.setScore1(score1);
                game.setScore2(score2);
                game.setGameState(GameState.FIRSTENTRY);
            }

            else if (game.getGameState() == GameState.FIRSTENTRY) {

                // check if the score is different
                if (game.getScore1() != score1 || game.getScore2() != score2) {
                    game.setGameState(GameState.CONFLICT);
                }
                else {
                    game.setGameState(GameState.FINISHED);
                    updateBracket(tournamentRepository.findByTournamentCode(tournamentCode));
                }
            }
            gameRepository.save(game);
            gameRepository.flush();
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found.");
        }
        // update the leaderboard if there is a winner e.g. there is no conflict

        updateLeaderboardWithGame(game, tournamentCode);
    }

    public void updateGameAsManager(String tournamentCode, long gameId, int score1, int score2) {

        Game game = gameRepository.findByGameId(gameId);

        game.setScore1(score1);
        game.setScore2(score2);
        game.setGameState(GameState.FINISHED);

        gameRepository.save(game);
        gameRepository.flush();

        updateBracket(tournamentRepository.findByTournamentCode(tournamentCode));
        updateLeaderboardWithGame(game, tournamentCode);
    }

    public void updateBracketAndLeaderboardAfterUserLeft(Participant participant, Tournament tournament) {

        Bracket bracket = tournament.getBracket();

        for (Game game : bracket.getBracketList()) {
            // go through all the games and find the one which the participant is part of and is not finished
            if (game.getParticipant1() != null &&
                game.getParticipant2() != null &&
                game.getGameState() != GameState.FINISHED &&
                (game.getParticipant1().getParticipantID().equals(participant.getParticipantID()) ||
                game.getParticipant2().getParticipantID().equals(participant.getParticipantID()))) {
                    gameForfait(game, participant, tournament.getTournamentCode());
                }
            }


        for (Leaderboard leaderboard : leaderboardRepository.findAllByTournamentCode(tournament.getTournamentCode())) {
            if (leaderboard.getParticipant().getParticipantID().equals(participant.getParticipantID())) {
                leaderboard.setPlayerState(PlayerState.LEFT);
            }
        }

        updateBracket(tournament);
    }

    public void updateBracket(Tournament tournament) {
        List<Game> gameList = tournament.getBracket().getBracketList();
        // jaaaa ich weiss isch hardcoded, aber bis mer en besseri lösig findet
        switch (gameList.size()) {
            case 1:
                // game 0 // final
                calculateTournamentWinner(gameList, 0, tournament);
                break;
            case 3:
                // game 0
                calculateNextFixture(gameList, 0, 2, 1);
                // game 1
                calculateNextFixture(gameList, 1, 2, 2);
                // game 2 // final
                calculateTournamentWinner(gameList, 2, tournament);
                break;
            case 7:
                // game 0
                calculateNextFixture(gameList, 0, 4, 1);
                // game 1
                calculateNextFixture(gameList, 1, 4, 2);
                // game 2
                calculateNextFixture(gameList, 2, 5, 1);
                // game 3
                calculateNextFixture(gameList, 3, 5, 2);
                // game 4
                calculateNextFixture(gameList, 4, 6, 1);
                // game 5
                calculateNextFixture(gameList, 5, 6, 2);
                // game 6 // final
                calculateTournamentWinner(gameList, 6, tournament);

                break;
            case 15:
                // game 0
                calculateNextFixture(gameList, 0, 8, 1);
                // game 1
                calculateNextFixture(gameList, 1, 8, 2);
                // game 2
                calculateNextFixture(gameList, 2, 9, 1);
                // game 3
                calculateNextFixture(gameList, 3, 9, 2);
                // game 4
                calculateNextFixture(gameList, 4, 10, 1);
                // game 5
                calculateNextFixture(gameList, 5, 10, 2);
                // game 6
                calculateNextFixture(gameList, 6, 11, 1);
                // game 7
                calculateNextFixture(gameList, 7, 11,2);
                // game 8
                calculateNextFixture(gameList, 9,12,1);
                // game 9
                calculateNextFixture(gameList, 9, 12,2);
                // game 10
                calculateNextFixture(gameList, 10,13,1);
                // game 11
                calculateNextFixture(gameList, 11, 13,2);
                // game 12
                calculateNextFixture(gameList, 12,14,1);
                // game 13
                calculateNextFixture(gameList, 13,14,2);
                // game 14 // final
                calculateTournamentWinner(gameList, 14, tournament);
                break;
            default:
        }
    }

    public void endTournament(String tournamentCode) {
        Tournament tournament = getTournamentByTournamentCode(tournamentCode);

        tournament.setTournamentState(TournamentState.ENDED);

        tournamentRepository.save(tournament);
        tournamentRepository.flush();
    }


    //helpers

    private void calculateNextFixture(List<Game> gameList, int firstGame, int gameToBeCalculated, int participant) {
        if (participant == 1) {
            if (gameList.get(firstGame).getGameState() == GameState.FINISHED && gameList.get(gameToBeCalculated).getParticipant1() == null) {
                gameList.get(gameToBeCalculated).setParticipant1(calculateWinner(gameList.get(firstGame)));
            }
        }
        else {
            if (gameList.get(firstGame).getGameState() == GameState.FINISHED && gameList.get(gameToBeCalculated).getParticipant2() == null) {
                gameList.get(gameToBeCalculated).setParticipant2(calculateWinner(gameList.get(firstGame)));
            }
        }
    }

    private void calculateTournamentWinner(List<Game> gameList, int game, Tournament tournament) {
        if (gameList.get(game).getGameState() == GameState.FINISHED && tournament.getWinner() == null) {
            tournament.setWinner(calculateWinner(gameList.get(game)));
        }
    }

    public static Participant calculateWinner(Game game) {
        if (game.getScore1() > game.getScore2()) {
            return game.getParticipant1();
        }
        else {
            return game.getParticipant2();
        }
    }

    public static String generateTournamentCode() {

        String number = "0123456789";

        SecureRandom random = new SecureRandom();

        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {

            // 0-62 (exclusive), random returns 0-61
            int rndCharAt = random.nextInt(number.length());
            char rndChar = number.charAt(rndCharAt);

            sb.append(rndChar);
        }
        return sb.toString();

    }

    public void gameForfait(Game game, Participant leaver,String tournamentCode) {

        // if it is participant 1
        if (game.getParticipant1().getParticipantID().equals(leaver.getParticipantID())) {
            game.setScore1(0);
            game.setScore2(3);
            game.setGameState(GameState.FINISHED);
        }
        // if it is participant 2
        else if (game.getParticipant2().getParticipantID().equals(leaver.getParticipantID())) {
            game.setScore2(0);
            game.setScore1(3);
            game.setGameState(GameState.FINISHED);
        }

        this.updateLeaderboardWithGame(game, tournamentCode);
    }

    public void updateLeaderboardWithGame(Game game, String tournamentCode) {
        // update the leaderboard if there is a winner e.g. there is no conflict
        if (game.getGameState() == GameState.FINISHED) {

            if (game.getScore1() > game.getScore2()) {
                updatePlayerStats(game, game.getParticipant1(), game.getParticipant2());
            }
            else {
                updatePlayerStats(game, game.getParticipant2(), game.getParticipant1());
            }
            updateLeaderboard(game, tournamentCode);
        }
    }

    private void updatePlayerStats(Game game, Participant winner, Participant loser) {

        // update winner
        Statistics statsWinner = winner.getStatistics();

        if  (game.getScore1() > game.getScore2()) {
            statsWinner.setPointsScored(statsWinner.getPointsScored() + game.getScore1());
            statsWinner.setPointsConceded(statsWinner.getPointsConceded() + game.getScore2());
        }
        else {
            statsWinner.setPointsScored(statsWinner.getPointsScored() + game.getScore2());
            statsWinner.setPointsConceded(statsWinner.getPointsConceded() + game.getScore1());
        }

        statsWinner.setWins(statsWinner.getWins() + 1);
        statsWinner.addGameToHistory(game);

        // update loser
        Statistics statsLoser = loser.getStatistics();
        if (game.getScore1() < game.getScore2()) {
            statsLoser.setPointsScored(statsLoser.getPointsScored() + game.getScore1());
            statsLoser.setPointsConceded(statsLoser.getPointsConceded() + game.getScore2());
        }
        else {
            statsLoser.setPointsScored(statsLoser.getPointsScored() + game.getScore2());
            statsLoser.setPointsConceded(statsLoser.getPointsConceded() + game.getScore1());
        }

        statsLoser.setLosses(statsLoser.getLosses() + 1);
        statsLoser.addGameToHistory(game);
    }

    private void calculateTimes(List<Game> gameList, String startTime, int breakTime, int gameTime, int tables) {
        LocalTime start = LocalTime.parse(startTime);

        switch (gameList.size()){

            case 1:
                gameList.get(0).setStartTime(startTime);
                break;
            case 3:
                // round1
                start = calculateRounds(gameList.subList(0, 2), start, (long) breakTime + (long) gameTime, tables);
                // round 2
                gameList.get(2).setStartTime(start.toString());
                break;
            case 7:
                // round1
                start = calculateRounds(gameList.subList(0, 4), start, (long) breakTime + (long) gameTime, tables);

                // round 2
                start = calculateRounds(gameList.subList(4, 6), start, (long) breakTime + (long) gameTime, tables);

                //round 3
                gameList.get(6).setStartTime(start.toString());
                break;
            case 15:
                // round1
                start = calculateRounds(gameList.subList(0, 8), start, (long) breakTime + (long) gameTime, tables);

                // round 2
                start = calculateRounds(gameList.subList(8, 12), start, (long) breakTime + (long) gameTime, tables);

                // round 3
                start = calculateRounds(gameList.subList(12, 14), start, (long) breakTime + (long) gameTime, tables);

                // round 4
                gameList.get(14).setStartTime(start.toString());
                break;
        }
    }

    private LocalTime calculateRounds(List<Game> gameList, LocalTime startTime, long addedTime, int tables) {
        if (tables >= gameList.size()) {
            for(Game game : gameList) {
                game.setStartTime(startTime.toString());
            }
        }
        else {
            int counter = 0;
            for (Game game : gameList) {
                game.setStartTime(startTime.toString());
                counter++;
                if (counter == tables) {
                    counter = 0;
                    startTime = startTime.plusMinutes(addedTime);
                }
            }
        }
        if (tables >= gameList.size()) {
            return startTime.plusMinutes(addedTime);
        }
        else if (gameList.size() % tables == 0) {
            return startTime;
        }
        else {
            return startTime.plusMinutes(addedTime);
        }
    }

    private void updateLeaderboard(Game game, String tournamentCode) {

        List<Leaderboard> leaderboardList = leaderboardRepository.findAllByTournamentCode(tournamentCode);

        for (Leaderboard leaderboard : leaderboardList) {
            if (leaderboard.getParticipant().getParticipantID().equals(game.getParticipant1().getParticipantID())) {

                // update stats for participant 1
                if (game.getScore1() > game.getScore2()) {
                    leaderboard.setWins(leaderboard.getWins() + 1);
                }
                else {
                    leaderboard.setLosses(leaderboard.getLosses() + 1);
                }

                leaderboard.setPointsScored(leaderboard.getPointsScored() + game.getScore1());
                leaderboard.setPointsConceded(leaderboard.getPointsConceded() + game.getScore2());

                leaderboardRepository.save(leaderboard);
                leaderboardRepository.flush();
            }
            else if (leaderboard.getParticipant().getParticipantID().equals(game.getParticipant2().getParticipantID())) {

                // update stats for participant 2
                if (game.getScore2() > game.getScore1()) {
                    leaderboard.setWins(leaderboard.getWins() + 1);
                }
                else {
                    leaderboard.setLosses(leaderboard.getLosses() + 1);
                }

                leaderboard.setPointsScored(leaderboard.getPointsScored() + game.getScore2());
                leaderboard.setPointsConceded(leaderboard.getPointsConceded() + game.getScore1());

                leaderboardRepository.save(leaderboard);
                leaderboardRepository.flush();
            }
        }
    }
}
