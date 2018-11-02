import java.util.*;

public class GameThread extends Thread {
    
    private String gameToken;
    private Iterator<Clue> deck;
    private ArrayList<PlayerThread> playerThreads = new ArrayList<PlayerThread>();
    private PlayerThread leader;
    
    private ArrayList<Submission> suggestions = new ArrayList<Submission>();
    private ArrayList<Submission> choices = new ArrayList<Submission>();
    private volatile boolean gameStarted = false;
    private volatile boolean gameEnded = false;
    
    private static HashMap<String, GameThread> gameThreads = new HashMap<String, GameThread>();
    
    // The leader PlayerThread should call this.
    public GameThread(PlayerThread leaderThread) {
        this.leader = leaderThread;
        this.gameToken = Utilities.getRandomString(3);
        this.deck = Clue.getDeckIterator();
        synchronized (gameThreads) {
            gameThreads.put(gameToken, this);
        }
    }

    public synchronized boolean logout(PlayerThread player) {
        if (playerThreads.contains(player)) {
            playerThreads.remove(player);
            User.saveUsersToFile();
            return true;
        }
        return false;
    }

    public boolean isGameStarted(){
        return gameStarted;
    }

    // Finds a GameThread based on the game token. Returns the GameThread with that token or null if none is found.
    public static GameThread getGameThreadByGameToken(String gameToken) {
        return gameThreads.get(gameToken);
    }
    
    public boolean startGame() {
        if (!gameStarted && playerThreads.size() >= 2) {
            start();
            gameStarted = true;
            return true;
        } else{
            return false;
        }
    }
    
    public void endGameAfterRound() {
        gameEnded = true;
    }
    
    // Adds a player to the game. All players, including the user, must call this.
    public boolean addPlayerToGame(PlayerThread player) {
        if (!gameStarted) {
            //playerThreads.add(GameThread.getPlayerThreadByPlayer());
            playerThreads.add(player);
        }
        return !gameStarted;
    }
    
    // Submits a player's suggested synonym for the current game word.
    public synchronized boolean submitPlayerSuggestion(String suggestion, User player) {
        suggestions.add(new Submission(suggestion, player));
        if (suggestions.size() == playerThreads.size()) {
            synchronized (this) {
                notify(); // All the suggestions have been received.
            }
        }
        return true;
    }
    
    // Submits a player's guess of the current definition.
    public boolean submitPlayerChoice(String choice, User player) {
        choices.add(new Submission(choice, player));
        if (choices.size() == playerThreads.size()) {
            synchronized (this) {
                notify(); // All the choices have been received.
            }
        }
        return true;
    }
    
    // Returns the game token associated with this game.
    public String getGameToken() {
        return gameToken;
    }
    
    public PlayerThread getLeader() {
        return leader;
    }
    
    public void run() {
        gameStarted = true;
        gameEnded = !deck.hasNext();
        while (!gameEnded) {
            Clue clue = deck.next();
            String question = clue.getQuestion();
            suggestions.clear();
            choices.clear();
            for (PlayerThread playerThread : playerThreads) {
                //playerThread.getUser().resetRoundData();
                playerThread.sendQuestion(question, clue.getAnswer());
            }
            try {
                synchronized (this) {
                    wait();
                }
            }
            catch (InterruptedException ie) {
                Utilities.communicateError("Error waiting for suggestions to be collected", true);
            }
            String[] suggestionsArray = getSuggestionsArray(clue.getAnswer());
            for (PlayerThread playerThread : playerThreads) {
                playerThread.sendSuggestions(suggestionsArray);
            }
            try {
                synchronized (this) {
                    wait();
                }
            }
            catch (InterruptedException ie) {
                Utilities.communicateError("Error waiting for choices to be collected", true);
            }
            String[] scoreMessageArray = getScoreMessageArray(clue.getAnswer());
            for (PlayerThread playerThread : playerThreads) {
                playerThread.sendScores(scoreMessageArray);
            }
            User.saveUsersToFile();
            gameEnded = gameEnded || !deck.hasNext();
        }
        for (PlayerThread playerThread : playerThreads) {
            playerThread.sendGameover();
        }
        gameThreads.remove(getGameToken());
    }
    
    private String[] getSuggestionsArray(String correctAnswer) {
        int length = suggestions.size();
        ArrayList<String> suggestionsList = new ArrayList<String>();
        for (int i = 0; i < length; i++) {
            suggestionsList.add(suggestions.get(i).getInput());
        }
        suggestionsList.add(correctAnswer);
        Collections.shuffle(suggestionsList);
        return suggestionsList.toArray(new String[0]);
    }
    
    private String[] getScoreMessageArray(String correctAnswer) {
        HashMap<String, User> suggestionsMapToUser = new HashMap<String, User>();
        for (int i = 0; i < playerThreads.size(); i++) {
            Submission choice = suggestions.get(i);
            suggestionsMapToUser.put(choice.getInput(), choice.getPlayer());
        }
        String[] messages = new String[5 * playerThreads.size()];
        for (int i = 0; i < messages.length; i++) {
            messages[i] = "";
        }
        for (int i = 0; i < choices.size(); i++) {
            Submission choice = choices.get(i);
            final User current = choice.getPlayer();
            if (choice.getInput().equals(correctAnswer)) {
                current.setCumulativeScore(current.getCumulativeScore() + 10);
                current.setScoreThisRound(current.getScoreThisRound()+10);
                messages[5 * i + 1] = "You got it right!" + messages[5 * i + 1];//Ensure it is first part of the String
            }
            for (int j = 0; j < choices.size(); j++) {
                Submission opponentChoice = choices.get(j);
                final User opponent = opponentChoice.getPlayer();
                User suggestionWriter = suggestionsMapToUser.get(opponentChoice.getInput());
                if (suggestionWriter == current && i != j) {
                    messages[5 * i + 1] += " You fooled " + opponent.getUsername() + ".";
                    messages[5 * j + 1] += " You were fooled by " + current.getUsername() + ".";
                    User.modifyUsers(new Runnable() {
                        public void run() {
                            current.setCumulativeScore(current.getCumulativeScore() + 5);
                            current.setScoreThisRound(current.getScoreThisRound() + 5);
                            current.setTotalTimesDidFool(current.getTotalTimesDidFool() + 1);
                            current.setTotalTimesDidFoolThisRound(current.getTotalTimesDidFoolThisRound() + 1);
                            opponent.setTotalTimesWasFooled(opponent.getTotalTimesWasFooled() + 1);
                            opponent.setTotalTimesWasFooledThisRound(opponent.getTotalTimesWasFooledThisRound() + 1);
                        }
                    });
                }
            }
        }
        for (int i = 0; i < choices.size(); i++) {
            User user = choices.get(i).getPlayer();
            messages[5 * i + 0] = user.getUsername();
            messages[5 * i + 1] = messages[5 * i + 1].trim();
            messages[5 * i + 2] = "" + user.getScoreThisRound();
            messages[5 * i + 3] = "" + user.getTotalTimesDidFoolThisRound();
            messages[5 * i + 4] = "" + user.getTotalTimesWasFooledThisRound();
        }
        return messages;
    }
    
    public static PlayerThread getPlayerThreadByPlayer(User player) {
        synchronized (gameThreads) {
            for (GameThread g : gameThreads.values()) {
                for (int i = 0; i < g.playerThreads.size(); i++) {
                    PlayerThread playerThread = g.playerThreads.get(i);
                    if (playerThread.getUser().getUsername().equals(player.getUsername())) {
                        return playerThread;
                    }
                }
            }
        }
        return null;
    }
    
}