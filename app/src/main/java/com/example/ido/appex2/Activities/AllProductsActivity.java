package com.example.ido.appex2.Activities;

import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.Toast;

import com.example.ido.appex2.Adapter.AudioBookAdapter;
import com.example.ido.appex2.Adapter.AudioBookWithKey;
import com.example.ido.appex2.R;
import com.example.ido.appex2.entities.AudioBook;
import com.example.ido.appex2.entities.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AllProductsActivity extends AppCompatActivity
{
    public static final String TAG = "AllProductsActivity";
    private AudioBookAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    @BindView(R.id.my_recycler_view)
    public RecyclerView mRecyclerView;


    private DatabaseReference mAllBooksRef;
    private DatabaseReference mMyUserRef;
    private List<AudioBookWithKey> m_BooksList = new ArrayList<>();;
    private User mUser;




    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_products);


        try
        {

            ButterKnife.bind(this);
            mRecyclerView = findViewById(R.id.my_recycler_view);
            mRecyclerView.setHasFixedSize(true);
            mLayoutManager = new LinearLayoutManager(this);
            mRecyclerView.setLayoutManager(mLayoutManager);
            mRecyclerView.setItemAnimator(new DefaultItemAnimator());


            FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();

            if (fbUser != null)
            {
                mMyUserRef = FirebaseDatabase.getInstance().getReference("Users/" + fbUser.getUid());
                mMyUserRef.addValueEventListener(new ValueEventListener()
                {
                    @Override
                    public void onDataChange(DataSnapshot snapshot)
                    {

                        Log.e(TAG, "onDataChange(User) >> " + snapshot.getKey());
                        mUser = snapshot.getValue(User.class);
                        getAllBooks();
                        Log.e(TAG, "onDataChange(User) <<");

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError)
                    {
                        Log.e(TAG, "onCancelled(Users) >>" + databaseError.getMessage());
                    }
                });

                Log.e(TAG, "onCreate() <<");
            }
            else
            {
                getAllBooks();
            }
        }
        catch(Exception e)
        {
            Log.e(TAG, "Fuck: ) >>" + e.toString());
            Toast.makeText(getApplicationContext(),e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    private void getAllBooks()
    {
        Log.e(TAG, "getAllBooks() >>");

        m_BooksList.clear();
        mAdapter = new AudioBookAdapter(m_BooksList, mUser);
        mRecyclerView.setAdapter(mAdapter);
        getAllBooksUsingChildListenrs();
        Log.e(TAG, "getAllBooks <<");

    }

    private void getAllBooksUsingChildListenrs()
    {
        Log.e(TAG, "getAllBooksUsingChildListenrs() >>");

        mAllBooksRef = FirebaseDatabase.getInstance().getReference("AudioBooks");

        mAllBooksRef.addChildEventListener(new ChildEventListener()
        {
            @Override
            public void onChildAdded(DataSnapshot snapshot, String previousChildName)
            {
                Log.e(TAG, "onChildAdded(Books) >> " + snapshot.getKey());

                AudioBookWithKey bookWithKey = new AudioBookWithKey(snapshot.getKey(),snapshot.getValue(AudioBook.class));

                m_BooksList.add(bookWithKey);
                mRecyclerView.getAdapter().notifyDataSetChanged();

                Log.e(TAG, "onChildAdded(Books) <<");

            }
            @Override
            public void onChildChanged(DataSnapshot snapshot, String previousChildName){

                Log.e(TAG, "onChildChanged(Songs) >> " + snapshot.getKey());

                AudioBook book =snapshot.getValue(AudioBook.class);
                String key = snapshot.getKey();

                for (int i = 0 ; i < m_BooksList.size() ; i++) {
                    AudioBookWithKey bookWithKey = (AudioBookWithKey) m_BooksList.get(i);
                    if (bookWithKey.getKey().equals(snapshot.getKey())) {
                        bookWithKey.setAudioBook(book);
                        mRecyclerView.getAdapter().notifyDataSetChanged();
                        break;
                    }
                }

                Log.e(TAG, "onChildChanged(Songs) <<");

            }
            @Override
            public void onChildMoved(DataSnapshot snapshot, String previousChildName)
            {

                Log.e(TAG, "onChildMoved(Songs) >> " + snapshot.getKey());


                Log.e(TAG, "onChildMoved(Songs) << Doing nothing");

            }
            @Override
            public void onChildRemoved(DataSnapshot snapshot)
            {

                Log.e(TAG, "onChildRemoved(Songs) >> " + snapshot.getKey());

                AudioBook book =snapshot.getValue(AudioBook.class);
                String key = snapshot.getKey();

                for (int i = 0 ; i < m_BooksList.size() ; i++)
                {
                    AudioBookWithKey songWithKey = (AudioBookWithKey) m_BooksList.get(i);
                    if (songWithKey.getKey().equals(snapshot.getKey()))
                    {
                        m_BooksList.remove(i);
                        mRecyclerView.getAdapter().notifyDataSetChanged();
                        Log.e(TAG, "onChildRemoved(Songs) >> i=" + i);
                        break;
                    }
                }

                Log.e(TAG, "onChildRemoved(Songs) <<");
            }

            @Override
            public void onCancelled(DatabaseError databaseError)
            {
                Log.e(TAG, "onCancelled(Songs) >>" + databaseError.getMessage());
            }
        });

        Log.e(TAG, "getAllBooksUsingChildListenrs <<");

    }
}
