package ofir.vander.firebase101;

import android.os.Bundle;

import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Collections;


public class GameActivity extends AppCompatActivity {

    private static final String TAG = "GameActivity"; // For logging

    // Firebase references
    private FirebaseDatabase database;
    private DatabaseReference questionsRef; // A reference to the root or a specific path

    // Using ArrayList to store the Question objects
    private List<Question> questionList; // Declare the list
    private List<Question> tempQuestionsHolder; // A temporary list to hold questions while fetching
    private boolean initialLoadDone = false; // Flag to ensure initial load happens only once
    // private int levelsSuccessfullyFetched = 0; // Counter for successfully fetched levels

    // UI elements
    ChipGroup cgAnswers;
    Chip cAns1, cAns2, cAns3, cAns4;
    TextView tvQuestion;
    Button bSubmit;
    ImageView ivCorrect, ivWrong;

    // Game Logic Variables
    int correctAnswer = 0, selectedAnswer = 0;
    int correctAnswersCounter = 0;    // how many correct answers by user. keep score.
    int currentQuestionIndex = 0;
    int questionsInGame = GameGlobalsSingleton.getInstance().getLevelsInGame();
    // this should also be put as GameGlobal !!
    String currentCategory = "SolarSystem";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        initUI();

        // Initialize questionList
        questionList = GameGlobalsSingleton.getInstance().getQuestionList();

        loadNextQuestion();

        bSubmit.setOnClickListener(v -> {
            // see that any chip is checked
            if (!cgAnswers.getCheckedChipIds().isEmpty()) {
                // set selected answer
                selectedAnswer = chipIdToInt(cgAnswers.getCheckedChipId());
                // check if correct
                if (selectedAnswer == correctAnswer) {
                    correctAnswersCounter++;
                    flicker(ivCorrect);
                } else
                    flicker(ivWrong);

                currentQuestionIndex++;

                loadNextQuestion();
            } else
                Toast.makeText(this, "Pick one!", Toast.LENGTH_SHORT).show();
        });
    }

    private void flicker(ImageView iv) {
        // temp function - USE vAnimations !

        iv.animate().alpha(1).setDuration(500).withEndAction(() -> {
            iv.animate().alpha(0).setDuration(500).start();
        });
    }

    private int chipIdToInt(int chipId) {
        if (chipId == R.id.cAns1)
            return 0;
        else if (chipId == R.id.cAns2)
            return 1;
        else if (chipId == R.id.cAns3)
            return 2;
        else if (chipId == R.id.cAns4)
            return 3;
        else
            return -1;
    }

    private void loadNextQuestion() {

        if (questionList.isEmpty()) {
            Log.e(TAG, "loadNextQuestion called but questionList is empty.");
            //  "no questions" state
            tvQuestion.setText("OOPS.. No Questions found!");
            bSubmit.setEnabled(false);
            for (int i = 0; i < cgAnswers.getChildCount(); i++)
                cgAnswers.getChildAt(i).setEnabled(false);
            return;
        }

        // check what question is now
        if (currentQuestionIndex < questionsInGame) {
            // unchecks all chips
            cgAnswers.clearCheck();
            bSubmit.setEnabled(true);

            // retrieve next question
            Question currentQuestion = questionList.get(currentQuestionIndex);
            // here scramble answers function
            List<String> answers = scrambleAnswers(currentQuestion);
            // set question text and answers
            tvQuestion.setText(currentQuestion.getQueText());
            cAns1.setText(answers.get(0));
            cAns2.setText(answers.get(1));
            cAns3.setText(answers.get(2));
            cAns4.setText(answers.get(3));

        } else {
            // Game Over
            // here confetti animation
            if (correctAnswersCounter > GameGlobalsSingleton.getInstance().getCurrentUser().getHighestScore()) {
                // Toast message
                Toast.makeText(this, "New High Score of " + correctAnswersCounter + "!", Toast.LENGTH_LONG).show();
                // set in the GameGlobals user opbject
                GameGlobalsSingleton.getInstance().getCurrentUser().setHighestScore(correctAnswersCounter);
            }
            else
                Toast.makeText(this, "Game Over You got " + correctAnswersCounter + " out of " + questionsInGame + " correct!", Toast.LENGTH_LONG).show();
            // in any case update that a game was played
            // NEED TO IMPLEMENT A REPEAT GAME BUTTON AND A FINAL ACTIVITY WITH SCORES !!!!!
            GameGlobalsSingleton.getInstance().getCurrentUser().setTotalGamesPlayed(
                    GameGlobalsSingleton.getInstance().getCurrentUser().getTotalGamesPlayed() + 1);
            // we also want to update this info in the Firebase Realtime Database
            Log.d(TAG, "Updating user info in Firebase uid" + GameGlobalsSingleton.getInstance().getCurrentUser().getUid());
            // this is the Firebase Database Object
            database = FirebaseDatabase.getInstance();
            // this is the reference to the specific user node in the database
            DatabaseReference userRef = database.getReference("Users").child(GameGlobalsSingleton.getInstance().getCurrentUser().getUid());
            // set the up to date values from game globals
            userRef.child("highestScore").setValue(GameGlobalsSingleton.getInstance().getCurrentUser().getHighestScore());
            userRef.child("totalGamesPlayed").setValue(GameGlobalsSingleton.getInstance().getCurrentUser().getTotalGamesPlayed());
        }
    }

    private List<String> scrambleAnswers(Question question) {
        Random random = new Random();
        correctAnswer = random.nextInt(4);

        List<String> answers = new ArrayList<>();
        String correct = question.getAnsCorrect();
        answers.add(question.getAnsWrong1());
        answers.add(question.getAnsWrong2());
        answers.add(question.getAnsWrong3());

        Collections.shuffle(answers);
        answers.add(correctAnswer, correct);
        return answers;
    }

    private void initUI() {
        cgAnswers = findViewById(R.id.cgAnswers);
        cAns1 = findViewById(R.id.cAns1);
        cAns2 = findViewById(R.id.cAns2);
        cAns3 = findViewById(R.id.cAns3);
        cAns4 = findViewById(R.id.cAns4);
        tvQuestion = findViewById(R.id.tvQuestion);
        bSubmit = findViewById(R.id.bSubmit);
        bSubmit.setEnabled(false);
        ivCorrect = findViewById(R.id.ivCorrect);
        ivWrong = findViewById(R.id.ivWrong);
    }
}