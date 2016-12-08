package com.kinvey.bookshelf;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.kinvey.android.Client;
import com.kinvey.android.callback.KinveyListCallback;
import com.kinvey.android.callback.KinveyPurgeCallback;
import com.kinvey.android.store.DataStore;
import com.kinvey.android.store.UserStore;
import com.kinvey.android.sync.KinveyPullCallback;
import com.kinvey.android.sync.KinveyPullResponse;
import com.kinvey.android.sync.KinveyPushCallback;
import com.kinvey.android.sync.KinveyPushResponse;
import com.kinvey.android.sync.KinveySyncCallback;
import com.kinvey.java.core.KinveyClientCallback;
import com.kinvey.java.dto.User;
import com.kinvey.java.store.StoreType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Shelf extends AppCompatActivity implements AdapterView.OnItemClickListener {

    Client client;
    BooksAdapter adapter;
    DataStore<BookDTO> bookStore;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shelf);
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        client =  ((App)getApplication()).getSharedClient();
        client.enableDebugLogging();
        bookStore = DataStore.collection(BookDTO.COLLECTION, BookDTO.class, StoreType.CACHE, client);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sync();
    }

    public void sync(){
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("syncing");
        pd.show();

        bookStore.sync(new KinveySyncCallback<BookDTO>() {
            @Override
            public void onSuccess(KinveyPushResponse kinveyPushResponse, KinveyPullResponse<BookDTO> kinveyPullResponse) {
                pd.dismiss();
                Toast.makeText(Shelf.this, "sync complete", Toast.LENGTH_LONG).show();
                getData();
            }

            @Override
            public void onPullStarted() {

            }

            @Override
            public void onPushStarted() {

            }

            @Override
            public void onPullSuccess(KinveyPullResponse<BookDTO> kinveyPullResponse) {

            }

            @Override
            public void onPushSuccess(KinveyPushResponse kinveyPushResponse) {

            }

            @Override
            public void onFailure(Throwable t) {
                pd.dismiss();
                Toast.makeText(Shelf.this, "sync failed", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void getData(){
        bookStore.find(new KinveyListCallback<BookDTO>() {
            @Override
            public void onSuccess(List<BookDTO> books) {
                updateBookAdapter(books);
            }

            @Override
            public void onFailure(Throwable error) {
            }
        });
    }

    private void updateBookAdapter(List<BookDTO> books) {
        if (books == null) {
            books = new ArrayList<BookDTO>();
        }
        ListView list = (ListView) findViewById(R.id.shelf);
        list.setOnItemClickListener(Shelf.this);
        adapter = new BooksAdapter(books, Shelf.this);
        list.setAdapter(adapter);
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_shelf, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        final ProgressDialog pd = new ProgressDialog(this);

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_new) {
            Intent i = new Intent(this, Book.class);
            startActivity(i);
            return true;
        } else if (id == R.id.action_sync){
            sync();
        } else if (id == R.id.action_pull){
            pd.setMessage("pulling");
            pd.show();
            bookStore.pull(null, new KinveyPullCallback<BookDTO>() {
                @Override
                public void onSuccess(KinveyPullResponse kinveyPullResponse) {
                    pd.dismiss();
                    getData();
                    Toast.makeText(Shelf.this, "pull success", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(Throwable error) {
                    pd.dismiss();
                    Toast.makeText(Shelf.this, "pull failed", Toast.LENGTH_LONG).show();
                }
            });
        } else if (id == R.id.action_push){
            pd.setMessage("pushing");
            pd.show();
            bookStore.push(new KinveyPushCallback() {
                @Override
                public void onSuccess(KinveyPushResponse kinveyPushResponse) {
                    pd.dismiss();
                    Toast.makeText(Shelf.this, "push success", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(Throwable error) {
                    pd.dismiss();
                    Toast.makeText(Shelf.this, "push failed", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onProgress(long current, long all) {

                }
            });
        } else if (id == R.id.action_purge){
            pd.setMessage("purging");
            pd.show();
            bookStore.purge(new KinveyPurgeCallback() {
                @Override
                public void onSuccess(Void result) {
                    pd.dismiss();
                    getData();
                    Toast.makeText(Shelf.this, "purge success", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(Throwable error) {
                    pd.dismiss();
                    Toast.makeText(Shelf.this, "purge failed", Toast.LENGTH_LONG).show();
                }

            });
        } else if (id == R.id.action_logout){
            UserStore.logout(client);
            Intent i = new Intent(this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        BookDTO book = adapter.getItem(position);
        Intent i = new Intent(this, Book.class);
        i.putExtra("id", book.get("_id").toString());
        startActivity(i);
    }
}
