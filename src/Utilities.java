import java.util.Random;

public class Utilities {
    
    public static final String USER_FILE_NAME = "users.dat";
    public static final String DECK_FILE_NAME = "clues.dat";
    public static final int    PORT           = 9999;
    
    private static Random random = new Random();
    
    public static synchronized void communicateError(String message, boolean isFatal) {
        if (isFatal) {
            System.out.println("Fatal error occurred: " + message);
        }
        else {
            System.out.println("Error occurred: " + message);
        }
    }
    
    public static synchronized String getRandomString(int length) {
        String token = "";
        synchronized (random) {
            for (int i = 0; i < length; i++) {
                token += "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".charAt(random.nextInt(52));
            }
        }
        return token;
    }
    
}