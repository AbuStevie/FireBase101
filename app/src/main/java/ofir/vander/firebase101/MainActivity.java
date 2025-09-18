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


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity"; // For logging

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
    int correctAnswer=0, selectedAnswer=0;
    int currentQuestionIndex = 0, questionsInGame = 5;
    String currentCategory = "SolarSystem";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();

        initFirebase();

        // Initialize your list
        questionList = new ArrayList<>();
        tempQuestionsHolder = new ArrayList<>();

        // fetch questions and populate list
        fetchQuestionsPerLevel();

        bSubmit.setOnClickListener(v -> {
            // see that any chip is checked
            if (!cgAnswers.getCheckedChipIds().isEmpty()) {
                // set selected answer
                selectedAnswer = chipIdToInt(cgAnswers.getCheckedChipId()) ;
                // check if correct
                if ( selectedAnswer == correctAnswer)
                    flicker(ivCorrect);
                else
                    flicker(ivWrong);

                currentQuestionIndex++;

                loadNextQuestion();
            }
            else
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

    private void loadNextQuestion(){

        if(questionList.isEmpty()){
            Log.e(TAG, "loadNextQuestion called but questionList is empty.");
            //  "no questions" state
            tvQuestion.setText("OOPS.. No Questions found!");
            bSubmit.setEnabled(false);
            for(int i=0; i < cgAnswers.getChildCount(); i++)
                cgAnswers.getChildAt(i).setEnabled(false);
            return;
        }

        // check what question is now
        if (currentQuestionIndex < questionsInGame ) {
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

        }
        else
            // here confetti animation
            Toast.makeText(this, "Game Over!", Toast.LENGTH_LONG).show();
    }

    private List<String> scrambleAnswers(Question question) {
        Random random = new Random();
        correctAnswer= random.nextInt(4);

        List<String> answers = new ArrayList<>();
        String correct = question.getAnsCorrect();
        answers.add(question.getAnsWrong1());
        answers.add(question.getAnsWrong2());
        answers.add(question.getAnsWrong3());

        Collections.shuffle(answers);
        answers.add(correctAnswer, correct);
        return answers;
    }

    private void initUI(){
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

    private void initFirebase(){
        // Initialize Firebase Database instance
        // This line gets the default database instance configured for your app
        database = FirebaseDatabase.getInstance();

        // Get a reference to your "Questions" node in Firebase
        // IMPORTANT: Make sure "Questions" exactly matches the key you used in Firebase.
        questionsRef = database.getReference("Questions/SolarSystem");
    }

    private void fetchQuestionsPerLevel() {
        Log.d(TAG, "Attempting to fetch 1 random question per level for " + questionsInGame + " levels from category: " + currentCategory);

        // 1. For recurring games - Reset state variables from any previous game/fetch
        if (questionList == null) { // Ensure questionList is initialized if not already
            questionList = new ArrayList<>();
        }
        questionList.clear(); // Clear the main list that holds questions for the current game

        if (tempQuestionsHolder == null) { // Ensure temp list is initialized
            tempQuestionsHolder = new ArrayList<>();
        }
        tempQuestionsHolder.clear(); // Clear the temporary list used during fetching

        //levelsSuccessfullyFetched = 0; // Reset the counter for successfully fetched levels
        initialLoadDone = false;       // Reset the flag indicating if the initial load attempt is complete
        // This is important if you allow re-fetching or new games.

        // 2. (Optional) Update UI to show loading state
        // Example:
        tvQuestion.setText("Loading questions...");
        bSubmit.setEnabled(false);
        // You might want a ProgressBar to be visible here.

        // 3. Initiate the recursive fetching process starting with level 1
        if (database == null) {
            Log.e(TAG, "FirebaseDatabase instance is null. Cannot fetch questions. Ensure initFirebase() is called.");
            Toast.makeText(this, "Error initializing database. Cannot load questions.", Toast.LENGTH_LONG).show();
            // Potentially update UI to reflect this critical error
            initialLoadDone = true; // Mark attempt as done, even if failed critically
            checkAndStart(); // Call process to handle the error state gracefully
            return;
        }

        Log.i(TAG, "Starting chain: Calling fetchRandomQuestionForLevel(1)");
        fetchRandomQuestionForLevel(1); // Kick off the chain for Level 1
    }

    private void fetchRandomQuestionForLevel(final int levelToFetch) {
        // 1. Base Case: If we've attempted to fetch beyond the number of desired levels
        if (levelToFetch > questionsInGame) {
            Log.d(TAG, "Base case reached: All " + questionsInGame + " levels have been attempted. Processing results...");
            checkAndStart(); // All levels attempted, now process whatever we gathered
            return; // Stop recursion
        }

        Log.i(TAG, "Attempting to fetch a random question for Level " + levelToFetch + " in category '" + currentCategory + "'");

        // 2. Construct the DatabaseReference and Query for the current level
        DatabaseReference categoryRef = database.getReference("Questions").child(currentCategory);
        // Firebase queries for numerical equality often expect a Double, even if data is Integer
        Query levelSpecificQuery = categoryRef.orderByChild("level").equalTo((double) levelToFetch);

        // 3. Attach a one-time listener to this specific query
        levelSpecificQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Question> questionsFoundAtThisLevel = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot questionSnapshot : dataSnapshot.getChildren()) {
                        Question question = questionSnapshot.getValue(Question.class);
                        if (question != null) {
                            questionsFoundAtThisLevel.add(question);
                        } else {
                            Log.w(TAG, "Null question object encountered for key: " + questionSnapshot.getKey() + " while fetching level " + levelToFetch);
                        }
                    }
                } // else: dataSnapshot does not exist, meaning no questions matched the query for this level

                if (!questionsFoundAtThisLevel.isEmpty()) {
                    // 4. Randomly select one question from the fetched list for this level
                    Random random = new Random();
                    int randomIndex = random.nextInt(questionsFoundAtThisLevel.size());
                    Question selectedQuestion = questionsFoundAtThisLevel.get(randomIndex);

                    tempQuestionsHolder.add(selectedQuestion); // Add the chosen question to our temporary list
                    //levelsSuccessfullyFetched++; // Increment counter for successfully fetched levels
                    Log.d(TAG, "Level " + levelToFetch + ": Successfully selected '" + selectedQuestion.getQueText() + "' from " + questionsFoundAtThisLevel.size() + " available questions.");
                } else {
                    Log.w(TAG, "Level " + levelToFetch + ": No questions found or all were null in category '" + currentCategory + "'. This level will be skipped.");
                    // Note: If no question is found for a level, tempQuestionsHolder will not get an entry for it.
                    // processFetchedQuestions() will need to handle cases where not all NUM_LEVELS questions were found.
                }

                // 5. Recursive Call: Proceed to fetch for the next level
                Log.d(TAG, "Level " + levelToFetch + " processing complete. Proceeding to fetch Level " + (levelToFetch + 1));
                fetchRandomQuestionForLevel(levelToFetch + 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Database error fetching Level " + levelToFetch + " in category '" + currentCategory + "': " + databaseError.getMessage());
                // Even if one level fails, we still try to proceed to the next to fetch what we can.
                // processFetchedQuestions() will handle the outcome if not all levels were successful.
                Log.d(TAG, "Level " + levelToFetch + " fetch cancelled. Proceeding to fetch Level " + (levelToFetch + 1));
                fetchRandomQuestionForLevel(levelToFetch + 1);
            }
        });
    }

    private void checkAndStart() {
        Log.d(TAG, "Processing fetched questions. Temporary holder size: " + tempQuestionsHolder.size() +
                ", Expected levels: " + questionsInGame);

        questionList.clear(); // Ensure the main list is empty before populating

        // Check if we successfully got one question for each desired level
        if (tempQuestionsHolder.size() == questionsInGame) {
            questionList.addAll(tempQuestionsHolder);
            Log.i(TAG, "Successfully fetched " + questionList.size() + " questions for the game.");
            // Optional: Shuffle the questionList if you don't want them in level order
            // Collections.shuffle(questionList); // Make sure to import java.util.Collections
        } else {
            // Not enough questions were collected
            Log.e(TAG, "Failed to fetch a complete set of questions. Expected " + questionsInGame +
                    ", but got " + tempQuestionsHolder.size() + ".");
            tvQuestion.setText("Failed to load questions. Please check connection and try again.");
            bSubmit.setEnabled(false);
            for(int i=0; i < cgAnswers.getChildCount(); i++)
                cgAnswers.getChildAt(i).setEnabled(false);

        }

        initialLoadDone = true; // Mark that the initial loading attempt (successful or not) is complete
        currentQuestionIndex = 0; // Reset question index for the new game/set of questions

        // After processing, try to load the first question if the list isn't empty
        if (!questionList.isEmpty()) {
            Log.d(TAG, "Loading first question...");
            loadNextQuestion();
        } else {
            Log.w(TAG, "Question list is empty after processing. No game can be started.");
            // Update UI to reflect that no questions are available for the game
            tvQuestion.setText("OOPS.. No Questions found!");
            bSubmit.setEnabled(false);
            for(int i=0; i < cgAnswers.getChildCount(); i++)
                cgAnswers.getChildAt(i).setEnabled(false);
        }
    }


}