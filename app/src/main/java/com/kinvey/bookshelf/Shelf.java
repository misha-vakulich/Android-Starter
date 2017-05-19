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

import com.kinvey.android.AsyncAppData;
import com.kinvey.android.Client;
import com.kinvey.android.callback.KinveyListCallback;
import com.kinvey.bookshelf.push.GCMService;
import com.kinvey.java.User;
import com.kinvey.java.auth.Credential;
import com.kinvey.java.auth.CredentialManager;
import com.kinvey.java.core.KinveyClientCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Shelf extends AppCompatActivity implements AdapterView.OnItemClickListener {

    Client client;
    BooksAdapter adapter;
    AsyncAppData<BookDTO> bookStore;
    private static final String username = "test";
    private static final String password = "test";
    private static final String redirectURI = "kinveyAuthDemo://";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shelf);
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        client = ((App) getApplication()).getSharedClient();
        client.enableDebugLogging();
        bookStore = client.appData(BookDTO.COLLECTION, BookDTO.class);

        if (client.user().isUserLoggedIn() && ((App) getApplication()).isFirstRun) {
            CredentialManager credentialManager = new CredentialManager(client.getStore());
            Credential credential = null;
            try {
                credential = credentialManager.loadCredential(client.user().getId());
            } catch (IOException e) {
                e.printStackTrace();
            }
            assert credential != null;
            credential.getRefreshToken();
            Credential newCredential = new Credential(client.user().getId(), "testString", credential.getRefreshToken());
            try {
                client.getStore().store(client.user().getId(), newCredential);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ((App) getApplication()).setFirstRun(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            checkLogin();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void get() {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("syncing");
        pd.show();

        bookStore.get(new KinveyListCallback<BookDTO>() {
            @Override
            public void onSuccess(BookDTO[] bookDTOs) {
                pd.dismiss();
                Toast.makeText(Shelf.this, "get complete", Toast.LENGTH_LONG).show();
                List<BookDTO> books = new ArrayList<BookDTO>();
                Collections.addAll(books, bookDTOs);
                updateBookAdapter(books);
            }

            @Override
            public void onFailure(Throwable throwable) {
                pd.dismiss();
                Toast.makeText(Shelf.this, "get failed", Toast.LENGTH_LONG).show();
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

    private void checkLogin() throws IOException {

        final ProgressDialog pd = new ProgressDialog(this);


        if (!client.user().isUserLoggedIn()) {
            pd.setIndeterminate(true);
            pd.setMessage("Logging in");
            pd.show();

            client.user().login(username, password, new KinveyClientCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    pd.dismiss();
                    Toast.makeText(Shelf.this, "logged in", Toast.LENGTH_LONG).show();
                    try {
                        Credential currentCred = client.getStore().load(user.getId());
                        Log.d(this.getClass().getName(), "refresh_token: " + currentCred.getRefreshToken());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    get();
                }

                @Override
                public void onFailure(Throwable throwable) {
                    pd.dismiss();
                    Toast.makeText(Shelf.this, "can't login to app", Toast.LENGTH_LONG).show();
                }
            });
            /*client.user().loginWithAuthorizationCodeAPI(username, password, redirectURI, new KinveyUserCallback() {
                @Override
                public void onSuccess(User user) {
                    pd.dismiss();
                    Toast.makeText(Shelf.this, "logged in", Toast.LENGTH_LONG).show();
                    try {
                        Credential currentCred = client.getStore().load(user.getId());
                        Log.d(this.getClass().getName(), "refresh_token: " + currentCred.getRefreshToken());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    enablePushNotification();
                    get();
                }

                @Override
                public void onFailure(Throwable throwable) {
                    pd.dismiss();
                    Toast.makeText(Shelf.this, "can't login to app", Toast.LENGTH_LONG).show();
                }
            });*/
        } else {
            get();
        }
    }

    private void enablePushNotification() {
        client.push(GCMService.class).initialize(getApplication(), new KinveyClientCallback() {
            @Override
            public void onSuccess(Object o) {
                Log.d(App.TAG, "enablePushNotification successful");
                Log.d(App.TAG, "isPushEnabled: " + client.push(GCMService.class).isPushEnabled());

            }

            @Override
            public void onFailure(Throwable throwable) {
                Log.d(App.TAG, String.valueOf(throwable.fillInStackTrace()));
            }
        });
    }

    private void disablePushNotification() {
        client.push(GCMService.class).initialize(getApplication(), new KinveyClientCallback() {
            @Override
            public void onSuccess(Object o) {
                Log.d(App.TAG, "enablePushNotification successful");
                Log.d(App.TAG, "isPushEnabled: " + client.push(GCMService.class).isPushEnabled());

            }

            @Override
            public void onFailure(Throwable throwable) {
                Log.d(App.TAG, String.valueOf(throwable.fillInStackTrace()));
            }
        });
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
        } else if (id == R.id.action_get) {
            get();
        } else if (id == R.id.action_logout) {
            logout();
        } else if (id == R.id.action_reqister_push) {
            enablePushNotification();
        } else if (id == R.id.action_unregister_push) {
            disablePushNotification();
        }
        return super.onOptionsItemSelected(item);
    }

    public void logout() {
        client.user().logout();
//        Toast.makeText(Shelf.this, "logout", Toast.LENGTH_LONG).show();
        finish();
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        BookDTO book = adapter.getItem(position);
        Intent i = new Intent(this, Book.class);
        i.putExtra("id", book.get("_id").toString());
        startActivity(i);
    }
}
