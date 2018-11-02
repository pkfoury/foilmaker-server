import java.util.*;
import java.io.*;

/**
 * Represents a game word - definition pair used in a round; a single "card" in the word deck.
 */
public class Clue {
    
    private String question;
    private String answer;
    
    private static ArrayList<Clue> deck = new ArrayList<Clue>();
    
    // Parses the format of clues used in file.
    public Clue(String rawInfo) throws IllegalArgumentException {
        String[] parts = rawInfo.split(":");
        if (parts.length == 2) {
            this.question = parts[0];
            this.answer = parts[1];
        }
        else {
            throw new IllegalArgumentException("Number of parts for clue is not two in String \"" + rawInfo + "\"");
        }
    }
        
    public Clue(String question, String answer) {
        this.question = question;
        this.answer = answer;
    }
    
    public String getQuestion() {
        return question;
    }
    
    public String getAnswer() {
        return answer;
    }
    
    public static boolean loadCluesFromFile() {
        String clueData = null;
        String fileName = Utilities.DECK_FILE_NAME;
        synchronized (deck) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(fileName));
                while ((clueData = br.readLine()) != null) {
                    Clue clue = new Clue(clueData);
                    deck.add(clue);
                }
                br.close();
                return true;
            }
            catch (IOException ioe) {
                Utilities.communicateError("Error loading clue data file \"" + fileName + "\"", true);
                return false;
            }
            catch (IllegalArgumentException iae) {
                Utilities.communicateError("Error line in clue data file \"" + fileName + "\"", false);
                return false;
            }
        }
    }
    
    public static Iterator<Clue> getDeckIterator() {
        return deck.iterator();
    }
    
}