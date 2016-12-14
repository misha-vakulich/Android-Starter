package com.kinvey.bookshelf;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.kinvey.android.AsyncAppData;
import com.kinvey.android.Client;
import com.kinvey.android.callback.KinveyDeleteCallback;
import com.kinvey.java.core.KinveyClientCallback;
import com.kinvey.java.model.KinveyDeleteResponse;

/**
 * Created by Prots on 3/15/16.
 */
public class Book extends AppCompatActivity implements View.OnClickListener {

    private EditText name;

    Client client;
    private BookDTO book = new BookDTO();

    AsyncAppData<BookDTO> bookStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.book);
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        client = ((App) getApplication()).getSharedClient();
        name = (EditText) findViewById(R.id.name);
        findViewById(R.id.save).setOnClickListener(this);
        bookStore = client.appData(BookDTO.COLLECTION, BookDTO.class);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (book.containsKey("_id")) {
            getMenuInflater().inflate(R.menu.menu_book, menu);
            return true;
        } else {
            return false;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        String id = getIntent().getStringExtra("id");

        final ProgressDialog pd = new ProgressDialog(this);
        if (id != null) {
            pd.show();
            bookStore.getEntity(id, new KinveyClientCallback<BookDTO>() {
                @Override
                public void onSuccess(BookDTO bookDTO) {
                    book = bookDTO;
                    name.setText(book.getName());
                    invalidateOptionsMenu();
                    pd.dismiss();
                }

                @Override
                public void onFailure(Throwable throwable) {
                    pd.dismiss();
                }
            });
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.save:
                if (name.getText().toString().trim().isEmpty()) {
                    Toast.makeText(this, "Name could not be empty", Toast.LENGTH_LONG).show();
                } else {
                    final ProgressDialog pd = new ProgressDialog(this);
                    pd.setMessage("Saving");
                    pd.show();
                    book.setName(name.getText().toString());
                    bookStore.save(book,
                            new KinveyClientCallback<BookDTO>() {
                                @Override
                                public void onSuccess(BookDTO result) {
                                    pd.dismiss();
                                    finish();
                                }

                                @Override
                                public void onFailure(Throwable error) {
                                    pd.dismiss();
                                    Toast.makeText(Book.this, "Can't save: " + error.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                }
                break;
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        final ProgressDialog pd = new ProgressDialog(this);

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_delete) {
            pd.show();
            bookStore.delete(book.get("_id").toString(), new KinveyDeleteCallback() {
                @Override
                public void onSuccess(KinveyDeleteResponse kinveyDeleteResponse) {
                    pd.dismiss();
                    finish();
                }

                @Override
                public void onFailure(Throwable throwable) {
                    pd.dismiss();
                    Toast.makeText(Book.this, "Failed to delete", Toast.LENGTH_LONG).show();
                }
            });
        }

        return true;
    }

}
