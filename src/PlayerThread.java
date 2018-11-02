/**
 * Created by peter on 11/29/2016.
 */

import java.net.*;
import java.io.*;

public class PlayerThread extends Thread {

    boolean connected;
    Socket socket;
    BufferedReader inFromClient;
    PrintWriter outToClient;
    String response = "RESPONSE";
    String startNewGameToken;
    User user;
    GameThread gameThread;
    boolean playerChoiceExpected = false;
    boolean playerSuggestionExpected = false;
    PlayerThread leader;

    public PlayerThread(Socket socket) {
        this.socket = socket;
    }

    public void sendGameover() {
        outToClient.println("GAMEOVER--");
    }

    public boolean sendQuestion(String question, String answer) {
        outToClient.println("NEWGAMEWORD--" + question + "--" + answer + "--");
        return true;
    }

    public boolean sendSuggestions(String[] suggestions) {
        String joined = String.join("--", suggestions);
        outToClient.println("ROUNDOPTIONS--" + joined + "--");
        return true;
    }

    public boolean sendScores(String[] scores) {
        String joinedScores = String.join("--", scores);
        outToClient.println("ROUNDRESULT--" + joinedScores);
        playerSuggestionExpected = true;
        return true;
    }

    public User getUser() {
        return user;
    }

    public void run() {
        if (socket != null) {
            connected = true;
        }
        try {
            outToClient = new PrintWriter(socket.getOutputStream(), true);
            inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.out.println("IOException");
        }
        while (connected) {
            try {
                String rawRequest = inFromClient.readLine();
                String[] request = rawRequest.split("--");
                String command = request[0];
                switch (command) {

                    // register user case
                    case "CREATENEWUSER":
                        // validations
                        if (request.length != 3) {
                            outToClient.println(response + "--" + command + "--" + "INVALIDMESSAGEFORMAT--");
                            break;
                        }
                        String newUsername = request[1];
                        String newPassword = request[2];
                        if (newUsername.length() == 0 || !User.isValidUsername(newUsername)) {
                            outToClient.println(response + "--" + command + "--" + "INVALIDUSERNAME--");
                        } else if (!User.isValidPassword(newPassword)) {
                            outToClient.println(response + "--" + command + "--" + "INVALIDUSERPASSWORD--");
                        } else if (User.doesUserExist(newUsername)) {
                            outToClient.println(response + "--" + command + "--" + "USERALREADYEXISTS--");
                        }
                        //register
                        else {
                            boolean register = User.addNewUser(newUsername, newPassword);
                            if (register)
                                outToClient.println(response + "--" + command + "--" + "SUCCESS--");
                        }
                        break;

                    // login case
                    case "LOGIN":
                        //validations
                        if (request.length != 3) {
                            outToClient.println(response + "--" + command + "--" + "INVALIDMESSAGEFORMAT--");
                            break;
                        }
                        String username = request[1];
                        String password = request[2];
                        if (!User.doesUserExist(username)) {
                            outToClient.println(response + "--" + command + "--" + "UNKNOWNUSER--");
                        } else if (!User.isAuthenticated(username, password)) {
                            outToClient.println(response + "--" + command + "--" + "INVALIDUSERPASSWORD--");
                        } else if (User.isUserLoggedIn(username)) {
                            outToClient.println(response + "--" + command + "--" + "USERALREADYLOGGEDIN--");
                        }
                        //login
                        else {
                            user = User.login(username, password);
                            outToClient.println(response + "--" + command + "--" + "SUCCESS--" + user.getUserToken() + "--");
                        }
                        break;

                    // new game case
                    case "STARTNEWGAME":
                        //validations
                        if (request.length != 2) {
                            outToClient.println(response + "--" + command + "--" + "INVALIDMESSAGEFORMAT--");
                            break;
                        }
                        startNewGameToken = request[1];
                        if (!user.getUserToken().equals(startNewGameToken)) {
                            outToClient.println(response + "--" + command + "--" + "USERNOTLOGGEDIN--");
                        } else if (User.isUserPlaying(user.getUsername())) {
                            outToClient.println(response + "--" + command + "--" + "FAILURE--");
                        }
                        //new game
                        else {
                            gameThread = new GameThread(this);
                            gameThread.addPlayerToGame(this);
                            String gameToken = gameThread.getGameToken();

                            outToClient.println(response + "--" + command + "--" + "SUCCESS" + "--" + gameToken);
                        }
                        break;

                    // join game case
                    case "JOINGAME":
                        //validation
                        String userToken = request[1];
                        String gameToken = request[2];
                        gameThread = GameThread.getGameThreadByGameToken(gameToken);
                        if (!user.getUserToken().equals(userToken)) {
                            outToClient.println(response + "--" + command + "--" + "USERNOTLOGGEDIN--");
                        } else if (gameThread == null) {
                            outToClient.println(response + "--" + command + "--" + "GAMEKEYNOTFOUND--");
                        } else if (User.isPlaying(user.getUsername())) {
                            outToClient.println(response + "--" + command + "--" + "FAILURE--");
                        }
                        //join game
                        else {
                            // call method to get leader
                            // leader.outtoclient.println("NEWPARTICIPANT"+"--"+ username + "--" + score);
                            leader = gameThread.getLeader();
                            String joinerName = (this.getUser()).getUsername();
                            int joinerScore = (this.getUser()).getCumulativeScore();
                            leader.outToClient.println("NEWPARTICIPANT--" + joinerName + "--" + joinerScore + "--");

                            gameThread.addPlayerToGame(this);
                            outToClient.println(response + "--" + command + "--" + "SUCCESS" + "--" + gameToken + "--");
                            playerSuggestionExpected = true;
                        }
                        break;


                    case "ALLPARTICIPANTSHAVEJOINED":
                        String userTokenLaunch = request[1];
                        String gameTokenLaunch = request[2];
                        if (!user.getUserToken().equals(userTokenLaunch)) {
                            outToClient.println(response + "--" + command + "--" + "USERNOTLOGGEDIN--");
                        } else if (!gameTokenLaunch.equals(gameThread.getGameToken())) {
                            outToClient.println(response + "--" + command + "--" + "INVALIDGAMETOKEN--");
                        } else if (User.isPlaying(user.getUsername())) {
                            outToClient.println(response + "--" + command + "--" + "USERNOTGAMELEADER--");
                        } else {
                            playerSuggestionExpected = true;
                            if(!gameThread.startGame()){
                                outToClient.println(response + "--" + command + "--" + "NOTENOUGHPLAYERS--");
                            }
                        }
                        break;


                    case "PLAYERSUGGESTION":
                        if (request.length != 4) {
                            outToClient.println(response + "--" + command + "--" + "INVALIDMESSAGEFORMAT--");
                            break;
                        }
                        String userTokenSuggestion = request[1];
                        String gameTokenSuggestion = request[2];
                        String suggestion = request[3];
                        if (!user.getUserToken().equals(userTokenSuggestion)) {
                            outToClient.println(response + "--" + command + "--" + "USERNOTLOGGEDIN--");
                        } else if (!gameTokenSuggestion.equals(gameThread.getGameToken())) {
                            outToClient.println(response + "--" + command + "--" + "INVALIDGAMETOKEN--");
                        } else if (playerSuggestionExpected == false) {
                            outToClient.println(response + "--" + command + "--" + "UNEXPECTEDMESSAGETYPE--");
                        } else {
                            playerSuggestionExpected = false;
                            gameThread.submitPlayerSuggestion(suggestion, user);
                            playerChoiceExpected = true;
                        }

                        break;


                    case "PLAYERCHOICE":
                        if (request.length != 4) {
                            outToClient.println(response + "--" + command + "--" + "INVALIDMESSAGEFORMAT--");
                            break;
                        }
                        String userTokenChoice = request[1];
                        String gameTokenChoice = request[2];
                        String choice = request[3];
                        if (!user.getUserToken().equals(userTokenChoice)) {
                            outToClient.println(response + "--" + command + "--" + "USERNOTLOGGEDIN--");
                        } else if (!gameTokenChoice.equals(gameThread.getGameToken())) {
                            outToClient.println(response + "--" + command + "--" + "INVALIDGAMETOKEN--");
                        } else if (playerChoiceExpected == false) {
                            outToClient.println(response + "--" + command + "--" + "UNEXPECTEDMESSAGETYPE--");
                        } else {
                            playerChoiceExpected = false;
                            gameThread.submitPlayerChoice(choice, user);
                        }
                        break;


                    case "LOGOUT":
                        connected = false;
                        if (!User.isUserLoggedIn(user.getUsername())) {
                            outToClient.println(response + "--" + command + "--" + "USERNOTLOGGEDIN--");
                        } else {
                            gameThread.logout(this);
                            outToClient.println(response + "--" + command + "--SUCCESS--");
                            outToClient.close();
                            inFromClient.close();
                            return;
                        }
                        break;

                    default:
                        outToClient.println(response + "--" + command + "--" + "INVALIDMESSAGEFORMAT--");
                        break;
                }
            } catch (IOException e) {
                System.out.println("IOException");
                return;
            }
        }
    }
}
