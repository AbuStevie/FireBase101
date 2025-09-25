package ofir.vander.firebase101;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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


public class DBFetchWait extends AppCompatActivity {

    private static final String TAG = "DBFetchWait"; // For logging

    // Firebase references
    private FirebaseDatabase database;
    private DatabaseReference questionsRef; // A reference to the root or a specific path

    // Using ArrayList to store the Question objects
    private List<Question> questionList; // Declare the list
    private List<Question> tempQuestionsHolder; // A temporary list to hold questions while fetching
    private boolean initialLoadDone = false; // Flag to ensure initial load happens only once
    // private int levelsSuccessfullyFetched = 0; // Counter for successfully fetched levels

    // GameGlobal info
    int levelsinGame = GameGlobalsSingleton.getInstance().getLevelsInGame();
    // this too should be put as GameGlobal !!
    String currentCategory = "SolarSystem";
    // user info
    String userName=GameGlobalsSingleton.getInstance().getCurrentUser().getUserName();
    String userPermission=GameGlobalsSingleton.getInstance().getCurrentUser().getPermission();
    int highestScore=GameGlobalsSingleton.getInstance().getCurrentUser().getHighestScore();
    int totalGamesPlayed=GameGlobalsSingleton.getInstance().getCurrentUser().getTotalGamesPlayed();

    // UI elements
    TextView tvInfo;
    TextView tvErrors;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dbfetch_wait);

        // handle animation and UI
        initUI();

        //init DB and question list
        initFirebase();
        questionList = GameGlobalsSingleton.getInstance().getQuestionList();
        tempQuestionsHolder = new ArrayList<>();

        // fetch questions and populate list
        fetchQuestionsPerLevel();

    }

    private void initUI(){
        tvErrors = findViewById(R.id.tvErrors);
        tvInfo = findViewById(R.id.tvInfo);
        tvInfo.setText("Welcome Back " + userName +
                "\nToal of " + totalGamesPlayed + " games played so far" +
                "\nHighest Score is " + highestScore +
                "\nwaiting for questions from firebase" +
                "\ncool anmation for you");
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
        Log.d(TAG, "Attempting to fetch 1 random question per level for " + levelsinGame + " levels from category: " + currentCategory);

        if (tempQuestionsHolder == null) { // Ensure temp list is initialized
            tempQuestionsHolder = new ArrayList<>();
        }
        tempQuestionsHolder.clear(); // Clear the temporary list used during fetching

        //levelsSuccessfullyFetched = 0; // Reset the counter for successfully fetched levels
        initialLoadDone = false;       // Reset the flag indicating if the initial load attempt is complete
        // This is important if you allow re-fetching or new games.


        // Initiate the recursive fetching process starting with level 1
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
        if (levelToFetch > levelsinGame) {
            Log.d(TAG, "Base case reached: All " + levelsinGame + " levels have been attempted. Processing results...");
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

            // Success Callback --->
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

            // Failure Callback --->
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
                ", Expected levels: " + levelsinGame);

        questionList.clear(); // Ensure the main list is empty before populating

        // Check if we successfully got all questions - load the questions to Globla questionList
        if (tempQuestionsHolder.size() == levelsinGame) {   // check
            Log.i(TAG, "Successfully fetched " + tempQuestionsHolder.size() + " questions for the game.");
            questionList.addAll(tempQuestionsHolder);       // load
                    } else {
            // Not enough questions were collected
            Log.e(TAG, "Failed to fetch a complete set of questions. Expected " + levelsinGame +
                    ", but got " + tempQuestionsHolder.size() + ".");
            tvErrors.setText("Failed to load questions. Please check connection and try again.");
        }

        initialLoadDone = true; // Mark that the initial loading attempt (successful or not) is complete

        // double check the load and start the game
        if (!questionList.isEmpty()) {                  // double check
            Log.d(TAG, "Starting GameActivity");   // start the game
            Intent go2Game = new Intent(this, GameActivity.class);
            startActivity(go2Game);
        } else {
            Log.w(TAG, "Question list is empty after processing. No game can be started.");
            // Update UI to reflect that no questions are available for the game
            tvErrors.setText("OOPS.. No Questions found!");
        }
    }


}