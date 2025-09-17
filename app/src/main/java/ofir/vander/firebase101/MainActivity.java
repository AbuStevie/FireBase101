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

import java.util.ArrayList; // Import ArrayList
import java.util.List;     // Import List (good practice to use the interface)


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity"; // For logging

    // Firebase references
    private FirebaseDatabase database;
    private DatabaseReference questionsRef; // A reference to the root or a specific path

    // Using ArrayList to store the Question objects
    private List<Question> questionList; // Declare the list
    private boolean initialLoadDone = false; // Flag to ensure initial load happens only once

    // UI elements
    ChipGroup cgAnswers;
    Chip cAns1, cAns2, cAns3, cAns4;
    TextView tvQuestion;
    Button bSubmit;
    ImageView ivCorrect, ivWrong;

    // Game Logic Variables
    int correctAnswer=0, selectedAnswer=0;
    int currentQuestionIndex = 0, questionsInGame = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initUI();

        initFirebase();

        // Initialize your list
        questionList = new ArrayList<>();

        // fetch questions and populate list
        fetchQuestionsFromDB();

        // loadNextQuestion();
        // cannot load next question until list is populated
        // i moved the initial load to the fetchQuestionsFromDB method
        // best implement a fragment with a loading screen

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
            return 1;
        else if (chipId == R.id.cAns2)
            return 2;
        else if (chipId == R.id.cAns3)
            return 3;
        else if (chipId == R.id.cAns4)
            return 4;
        return 0;
    }

    private void loadNextQuestion(){

        if(questionList.isEmpty()){
            Log.e(TAG, "loadNextQuestion called but questionList is empty.");
            // Potentially show a "game over" or "no questions" state
            Toast.makeText(this, "No more questions or questions not loaded.", Toast.LENGTH_LONG).show();
            // You might want to disable UI elements here
            return;
        }

        // check what question is now
        if (currentQuestionIndex < questionsInGame && currentQuestionIndex < questionList.size()) {
            // unchecks all chips
            cgAnswers.clearCheck();

            // retrieve next question
            Question currentQuestion = questionList.get(currentQuestionIndex);
            // here scramble answers function
            correctAnswer = 4;
            // set question text and answers
            tvQuestion.setText(currentQuestion.getQueText());
            cAns4.setText(currentQuestion.getAnsCorrect());
            cAns1.setText(currentQuestion.getAnsWrong1());
            cAns2.setText(currentQuestion.getAnsWrong2());
            cAns3.setText(currentQuestion.getAnsWrong3());

        }
        else
            // here confetti animation
            Toast.makeText(this, "Game Over!", Toast.LENGTH_LONG).show();
    }

    private void initUI(){
        cgAnswers = findViewById(R.id.cgAnswers);
        cAns1 = findViewById(R.id.cAns1);
        cAns2 = findViewById(R.id.cAns2);
        cAns3 = findViewById(R.id.cAns3);
        cAns4 = findViewById(R.id.cAns4);
        tvQuestion = findViewById(R.id.tvQuestion);
        bSubmit = findViewById(R.id.bSubmit);
        ivCorrect = findViewById(R.id.ivCorrect);
        ivWrong = findViewById(R.id.ivWrong);
    }

    private void initFirebase(){
        // Initialize Firebase Database instance
        // This line gets the default database instance configured for your app
        database = FirebaseDatabase.getInstance();

        // Get a reference to your "Questions" node in Firebase
        // IMPORTANT: Make sure "Questions" exactly matches the key you used in Firebase.
        questionsRef = database.getReference("Questions");
    }

    private void fetchQuestionsFromDB() {
        // Add a ValueEventListener to the "Questions" reference
        questionsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.

                questionList.clear(); // Clear the list before adding new data to avoid duplicates on updates

                Log.d(TAG, "DataSnapshot exists: " + dataSnapshot.exists());
                Log.d(TAG, "Number of questions found: " + dataSnapshot.getChildrenCount());

                // Iterate through each child snapshot (each question object)
                for (DataSnapshot questionSnapshot : dataSnapshot.getChildren()) {
                    // Attempt to convert the snapshot into a Question object
                    Question question = questionSnapshot.getValue(Question.class);

                    if (question != null) {
                        questionList.add(question); // Add the parsed Question object to your list
                        Log.d(TAG, "Fetched Question: ID = " + question.getId() + ", Text = " + question.getQueText());
                    } else {
                        Log.w(TAG, "Question data is null for key: " + questionSnapshot.getKey());
                        // This can happen if the structure in Firebase doesn't match your Question.java class
                        // or if a specific node is malformed.
                    }
                }

                // Now your 'questionList' ArrayList is populated with Question objects
                // You can use it to update your UI, display in a RecyclerView, etc.
                Log.d(TAG, "Total questions in list: " + questionList.size());

                // Example: If you wanted to verify the contents
                /*for (Question q : questionList) {
                    Log.i(TAG, "Question in list: " + q.toString());

                }*/

                // until we implement a loading screen fragment, or a dedicated loading activity - initial loading is here
                if (!questionList.isEmpty() && !initialLoadDone) {
                    loadNextQuestion(); // Load the first question
                    initialLoadDone = true; // Set flag so it doesn't reload on subsequent data changes
                } else if (questionList.isEmpty()) {
                    // Handle the case where no questions were loaded
                    Log.e(TAG, "No questions loaded from Firebase.");
                    Toast.makeText(MainActivity.this, "OOPS.. problem fetching Questions..", Toast.LENGTH_LONG).show();

                }

                // IMPORTANT: If you only want to fetch the data ONCE and not listen for continuous updates,
                // you might prefer using questionsRef.addListenerForSingleValueEvent(...) instead of addValueEventListener.
                // For now, addValueEventListener is fine to see it working.
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Getting Post failed, log a message
               Log.w(TAG, "loadQuestions:onCancelled", databaseError.toException());
                // Handle the error, e.g., show a message to the user
            }
        });
    }

    // Optional: If you used addValueEventListener, remember to remove it when the activity is destroyed
    // to prevent memory leaks, though for a simple main activity it might not be strictly necessary
    // if the app closes. For fragments or long-lived listeners, it's crucial.
    /*
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // if (questionsRef != null && yourValueEventListener != null) {
        //     questionsRef.removeEventListener(yourValueEventListener);
        // }
    }
    */

}