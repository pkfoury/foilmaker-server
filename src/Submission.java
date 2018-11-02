public class Submission {

    private String input;
    private User player;
    
    public Submission(String input, User player) {
        this.input = input;
        this.player = player;
    }
    
    public String getInput() {
        return input;
    }
    
    public User getPlayer() {
        return player;
    }
    
}