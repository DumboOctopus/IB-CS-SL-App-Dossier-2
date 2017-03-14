package com.example.neilprajapati.appdosier2;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

public class ContinuousMoneyChangeActivity extends AppCompatActivity {
    // List of tags
    private List<String> tags = new ArrayList<>();

    // Fields for Firebase database functionalities
    private FirebaseAuth mFirebaseAuth;
    private DatabaseReference mDatabase;
    private FirebaseUser mFirebaseUser;
    private String mUserId;
    private Balance balance;



    private ArrayAdapter<String> adapter;
    private AutoCompleteTextView textView;


    private ArrayAdapter<String> listviewAdaptar;
    private ArrayList<ContinuousMoneyChange> listviewAsArrayList;

    /**
     * Initializes the app functionality and views. If the user is not logged in, it also redirects the user to the login if they are not logged in.
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle). Note: Otherwise it is null.
     **/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_continuous_money_change);

        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        System.out.println("starting app");

        if(mFirebaseUser == null){
            System.out.println("user not loginned in");
            Intent intent = new Intent(ContinuousMoneyChangeActivity.this, LogInActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else{
            mUserId = mFirebaseUser.getUid();



            final ListView lview = (ListView) findViewById(R.id.listy);
            listviewAdaptar = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
            listviewAsArrayList = new ArrayList<>();
            lview.setAdapter(listviewAdaptar);

            lview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                 public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                     balance.getContinuousMoneyChanges().remove(position);
                     mDatabase.child("users").child(mUserId).child("balance").child("obj").setValue(balance);
                     listviewAdaptar.remove(listviewAdaptar.getItem(position));
                     listviewAdaptar.notifyDataSetChanged();
                 }
            });

            setUpTags();
            setUpBalance();


            adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, tags);
            textView = (AutoCompleteTextView) findViewById(R.id.tagValue);
            textView.setAdapter(adapter); // links the set of tags to the autocomplete text view





            final EditText moneyChangeAmount = (EditText) findViewById(R.id.moneyChangeValue);
            final EditText timePeriod = (EditText) findViewById(R.id.timePeriod);

            final Button addExpenseButton = (Button) findViewById(R.id.addExpense);
            addExpenseButton.setOnClickListener(new View.OnClickListener() {
                /**
                 * Checks if the data in the inputs are not empty and their formats are acceptable. If they are acceptable, it creates a 
                 * OneTimeMoneyChange expense from the data and pushes it to the database.
                 *
                 * @param v the current view
                 **/
                public void onClick(View v) {
                    System.out.println("Inside tags: "+tags);
                    if(moneyChangeAmount.getText().toString().matches("[-+]?\\d*\\.?\\d+") && !textView.getText().toString().equals(""))
                        appendMoneyChange(
                                new ContinuousMoneyChange(
                                        -Double.parseDouble(moneyChangeAmount.getText().toString()),
                                        Integer.parseInt(timePeriod.getText().toString()),
                                        textView.getText().toString()
                                )
                        );

                }
            });



            final Button addIncomeButton = (Button) findViewById(R.id.addIncome);
            addIncomeButton.setOnClickListener(new View.OnClickListener() {
                /**
                 * Checks if the data in the inputs are not empty and their format is acceptable. If they are acceptable, it creates a                
                 * OneTimeMoneyChange from the data and pushes it to the database.
                 *
                 * @param v the current view
                 **/
                public void onClick(View v) {

                    System.out.println("Inside tags: "+tags);
                    if(moneyChangeAmount.getText().toString().matches("[-+]?\\d*\\.?\\d+") && !textView.getText().toString().equals(""))

                        appendMoneyChange(
                                new ContinuousMoneyChange(
                                        Double.parseDouble(moneyChangeAmount.getText().toString()),
                                        Integer.parseInt(timePeriod.getText().toString()),
                                        textView.getText().toString().trim()
                                )
                        );

                }
            });

            final Button logoutButton = (Button) findViewById(R.id.logoutButton);
            logoutButton.setOnClickListener(new View.OnClickListener() {
                /**
                 * Logs out the current user when the button is pressed.
                 *
                 * @param v the current view
                 **/
                @Override
                public void onClick(View v) {
                    mFirebaseAuth.signOut();
                    Intent intent = new Intent(ContinuousMoneyChangeActivity.this, LogInActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            });


        }


    }
    /**
     * Sets up a child listener for the balance node in the firebase and sets up a queue system for storing premature requests which are latter pushed
     * to the database.
     **/
    private void setUpBalance() {

        mDatabase.child("users").child(mUserId).child("balance").addChildEventListener(new ChildEventListener() {
            /**
             * When the balance object is loaded from the database, this method instantiates a local copy of it, handles the premature requests
             * and updates the GUI.
             **/
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if(dataSnapshot.getValue() == null) return;
                balance = dataSnapshot.getValue(Balance.class);


                balance.cleanFields();
                listviewAdaptar.clear();
                listviewAsArrayList.clear();
                for(ContinuousMoneyChange c: balance.getContinuousMoneyChanges()) {
                    listviewAdaptar.add(c.toString());
                    listviewAsArrayList.add(c);
                }
            }

            /**
             * When the balance object is updated, this method copies the changes and updates the GUI.
             **/
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                balance = dataSnapshot.getValue(Balance.class);
                balance.cleanFields();

                listviewAdaptar.clear();
                listviewAsArrayList.clear();
                for(ContinuousMoneyChange c: balance.getContinuousMoneyChanges()) {
                    listviewAdaptar.add(c.toString());
                    listviewAsArrayList.add(c);
                }
            }
            /**
             * These methods are unused but still must exist within the class.
             **/
            public void onChildRemoved(DataSnapshot dataSnapshot) {}
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("Inside: Canceled");
            }
        });
    }
    /**
     * Sets up the a queue for handling premature new tag requests, and sets up a child listener for retrieving new data from the base.
     **/
    private void setUpTags() {
        mDatabase.child("users").child(mUserId).child("tags").addChildEventListener(new ChildEventListener() {
            /**
             * When a tag is loaded from the database, this method first handles all prematures tag push requests.
             *
             *@param dataSnapshot the current package of data being sent.
             *@param s the key of the previous node loaded.
             **/
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                tags.add((String) dataSnapshot.getValue());
                Collections.sort(tags);
                adapter.add((String) dataSnapshot.getValue());
                adapter.notifyDataSetChanged();
                textView.setAdapter(adapter);
            }


            public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
            public void onChildRemoved(DataSnapshot dataSnapshot) { }
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
            public void onCancelled(DatabaseError databaseError) { }
        });
    }


    /**
     * Increases the unit count for the system to multiply the base cost of an exchange of funds, It has no limit.
     * It is held in the “android:onClick” attribute of a button to allow it to run.
     *
     * @param v the View that the button is in, provides context surrounding the button that calls it.
     **/
    public void addUnit(View v) {
        TextView unitQuantity = (TextView) findViewById(R.id.unitCount);
        int currentValue = Integer.parseInt(unitQuantity.getText().toString());

        currentValue++;

        unitQuantity.setText(currentValue + "");
    }

    /**
     * Reduces the unit count for the system to multiply the base cost of an exchange of funds, but only if the resulting value exceeds one unit.
     * It is held in the “android:onClick” attribute of a button to allow it to run.
     *
     * @param v the View that the button is in, provides context surrounding the button that calls it.
     **/
    public void removeUnit(View v) {
        TextView unitQuantity = (TextView) findViewById(R.id.unitCount);
        int currentValue = Integer.parseInt(unitQuantity.getText().toString());

        if (currentValue > 1) {
            currentValue--;
        }

        unitQuantity.setText(currentValue + "");
    }

    /**
     * Creates a request to update the balance and the tags if possible. If its not possible (balance is not initialized), its registers data in the queues.
     * @param change the OneTimeMoneyChange to be pushed to the database
     **/
    public void appendMoneyChange(ContinuousMoneyChange change){
        if(balance == null){
            return;
        }
        balance.add(change);
        mDatabase.child("users").child(mUserId).child("balance").child("obj").setValue(balance);
        if(!tags.contains(change.getTag())){
            mDatabase.child("users").child(mUserId).child("tags").push().setValue(change.getTag());
            //that is handled by the child listener
//            tags.add(change.getTag());
//            Collections.sort(tags);
        }
    }

}
