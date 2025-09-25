package ofir.vander.firebase101;
import java.util.ArrayList;
import java.util.List;

public class GameGlobalsSingleton {
    private List<Question> questionList;
    private final int levelsInGame;
    private User currentUser;

    private GameGlobalsSingleton(){
        questionList= new ArrayList<>();
        levelsInGame = 5;
    }

    private static class SingletonHelper{
        private static final GameGlobalsSingleton INSTANCE = new GameGlobalsSingleton();
    }

    public User getCurrentUser(){
        return currentUser;
    }
    public void setCurrentUser(User user){
        currentUser = user;
    }

    public static GameGlobalsSingleton getInstance(){
        return SingletonHelper.INSTANCE;
    }

    public List<Question> getQuestionList(){
        return questionList;
    }

    public void setQuestionList(List<Question> list){
        questionList = list;
    }

    public int getLevelsInGame(){
        return levelsInGame;
    }

    public void clearQuestionList(){
        questionList.clear();
    }
}
