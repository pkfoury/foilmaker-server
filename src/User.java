import java.util.*;
import java.io.*;

/**
 * Represents a registered user.
 */
public class User {
    
    private String username;
    private String password;
    private int cumulativeScore;
    private int totalTimesDidFool;
    private int totalTimesWasFooled;
    
    private String userToken;
    private boolean isLoggedIn = false;
    private int scoreThisRound;
    private int totalTimesDidFoolThisRound;
    private int totalTimesWasFooledThisRound;
    
    // Master current list of registered users.
    private static HashMap<String, User> registeredUsers = new HashMap<String, User>();
    
    // Creates a User from a String in the format used in the user data file
    public User(String rawInfo) throws IllegalArgumentException {
        String[] parts = rawInfo.split(":");
        try {
            this.username = parts[0];
            this.password = parts[1];
            this.cumulativeScore = Integer.parseInt(parts[2]);
            this.totalTimesDidFool = Integer.parseInt(parts[3]);
            this.totalTimesWasFooled = Integer.parseInt(parts[4]);
        }
        catch (ArrayIndexOutOfBoundsException aioobe) {
            throw new IllegalArgumentException("Number of fields of user data is not five in String \"" + 
                                               rawInfo + "\"");
        }
        catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Integer not found where expected for user data in String \"" +
                                               rawInfo + "\"");
        }
    }
    
    public User(String username, String password) {
        this(username, password, 0, 0, 0);
    }
    
    public User(String username, String password, int cumulativeScore, int totalTimesDidFool,
                int totalTimesWasFooled) {
        this.username = username;
        this.password = password;
        this.cumulativeScore = cumulativeScore;
        this.totalTimesDidFool = totalTimesDidFool;
        this.totalTimesWasFooled = totalTimesWasFooled;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public int getCumulativeScore() {
        return cumulativeScore;
    }
    
    public void setCumulativeScore(int cumulativeScore) {
        this.cumulativeScore = cumulativeScore;
    }
    
    public int getTotalTimesDidFool() {
        return totalTimesDidFool;
    }
    
    public void setTotalTimesDidFool(int totalTimesDidFool) {
        this.totalTimesDidFool = totalTimesDidFool;
    }
    
    public int getTotalTimesWasFooled() {
        return totalTimesWasFooled;
    }
    
    public void setTotalTimesWasFooled(int totalTimesWasFooled) {
        this.totalTimesWasFooled = totalTimesWasFooled;
    }
    
    public int getScoreThisRound() {
        return scoreThisRound;
    }
    
    public void setScoreThisRound(int scoreThisRound) {
        this.scoreThisRound = scoreThisRound;
    }
    
    public int getTotalTimesDidFoolThisRound() {
        return totalTimesDidFoolThisRound;
    }
    
    public void setTotalTimesDidFoolThisRound(int totalTimesDidFoolThisRound) {
        this.totalTimesDidFoolThisRound = totalTimesDidFoolThisRound;
    }
    
    public int getTotalTimesWasFooledThisRound() {
        return totalTimesWasFooledThisRound;
    }
    
    public void setTotalTimesWasFooledThisRound(int totalTimesWasFooledThisRound) {
        this.totalTimesWasFooledThisRound = totalTimesWasFooledThisRound;
    }
    
    public String getUserToken() {
        if (userToken == null) {
            userToken = Utilities.getRandomString(10);
        }
        return userToken;
    }
    
    public void resetRoundData() {
        scoreThisRound = 0;
        totalTimesDidFoolThisRound = 0;
        totalTimesWasFooledThisRound = 0;
    }
    
    // Outputs a String in the same format as used in the user data file
    public String toString() {
        return String.format("%s:%s:%d:%d:%d", username, password, cumulativeScore, totalTimesDidFool,
                             totalTimesWasFooled);
    }
    
    public static boolean loadUsersFromFile() {
        String userData = null;
        String fileName = Utilities.USER_FILE_NAME;
        try {
            synchronized (registeredUsers) {
                registeredUsers.clear(); // Ensure hashmap is empty before filling it.
                BufferedReader br = new BufferedReader(new FileReader(fileName));
                while ((userData = br.readLine()) != null) {
                    User user = new User(userData);
                    registeredUsers.put(user.getUsername(), user);
                }
                br.close();
                return true;
            }
        }
        catch (IOException ioe) {
            Utilities.communicateError("Error loading user data file \"" + fileName + "\"", false);
            return false;
        }
        catch (IllegalArgumentException iae) {
            Utilities.communicateError("Error line in user data file \"" + fileName + "\"", false);
            return false;
        }
    }
    
    public static boolean saveUsersToFile() {
        User currentUser = null;
        String fileName = Utilities.USER_FILE_NAME;
        try {
            synchronized (registeredUsers) {
                BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
                Iterator<User> users = registeredUsers.values().iterator();
                while (users.hasNext()) {
                    currentUser = users.next();
                    String userString = currentUser.toString();
                    bw.write(userString, 0, userString.length());
                    if (users.hasNext()) {
                        bw.newLine();
                    }
                }
                bw.flush();
                bw.close();
                return true;
            }
        }
        catch (IOException ioe) {
            Utilities.communicateError("Error writing user data file \"" + Utilities.USER_FILE_NAME, false);
            return false;
        }
    }
    
    public static boolean doesUserExist(String username) {
        return registeredUsers.get(username) != null;
    }
    
    public static boolean isValidUsername(String username) {
        return 0 < username.length() && username.length() < 10 && username.matches("[a-zA-Z\\d_]+");
    }

    public static boolean isValidPassword(String password) {
        return 0 < password.length() && password.length() < 10 && password.matches("[a-zA-Z\\d\\#\\&\\$\\*]+") &&
            password.matches(".*[A-Z].*") && password.matches(".*\\d.*");
    }
    
    public static boolean isUserLoggedIn(String username) {
        return registeredUsers.get(username).isLoggedIn;
    }
    
    public static boolean isUserPlaying(String username) {
        return isPlaying(username);
    }
    
    public static boolean addNewUser(String username, String password) {
        User user = new User(username, password);
        synchronized (registeredUsers) {
            if (registeredUsers.containsKey(user.getUsername())) {
                Utilities.communicateError("Error adding user, a user with name \"" + user.getUsername() +
                                           "\" is already registered", false);
                return false;
            }
            else {
                registeredUsers.put(user.getUsername(), user);
                saveUsersToFile();
                return true;
            }
        }
    }
    
    public static boolean isAuthenticated(String username, String password) {
        synchronized (registeredUsers) {
            if (registeredUsers.containsKey(username) &&
                registeredUsers.get(username).getPassword().equals(password)) {
                return true;
            }
            return false;
        }
    }
    
    public static User login(String username, String password) {
        if (isAuthenticated(username, password) && doesUserExist(username) && !isUserLoggedIn(username)) {
            registeredUsers.get(username).isLoggedIn = true;
            return registeredUsers.get(username);
        }
        else {
            return null;
        }
    }
    
    public static User getUserByUserToken(String userToken) {
        synchronized (registeredUsers) {
            for (User user : registeredUsers.values()) {
                if (user.getUserToken().equals(userToken))
                    return user;
            }
        }
        return null;
    }
    
    public static boolean isPlaying(String username) {
        User user = null;
        synchronized (registeredUsers) {
            user = registeredUsers.get(username);
        }
        PlayerThread playerThread = GameThread.getPlayerThreadByPlayer(user);
        if(playerThread == null)
            return false;
        return playerThread.gameThread.isGameStarted();
                //GameThread.getPlayerThreadByPlayer(user) != null;
    }
    
    public static boolean modifyUsers(Runnable callback) {
        synchronized (registeredUsers) {
            callback.run();
        }
        return saveUsersToFile();
    }
    
}