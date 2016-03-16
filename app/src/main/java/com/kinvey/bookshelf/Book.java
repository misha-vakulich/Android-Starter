package com.kinvey.bookshelf;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.kinvey.android.Client;
import com.kinvey.java.core.KinveyClientCallback;
import com.kinvey.java.dto.User;

/**
 * Created by Prots on 3/15/16.
 */
public class Book extends AppCompatActivity implements View.OnClickListener {
    Client client;

    EditText name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.book);
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        client =  ((App)getApplication()).getSharedClient();

        name = (EditText) findViewById(R.id.name);

        findViewById(R.id.save2).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.save2:
                if (name.getText().toString().trim().isEmpty()){
                    Toast.makeText(this, "Name could not be empty", Toast.LENGTH_LONG).show();
                } else {

                    final ProgressDialog pd = new ProgressDialog(this);
                    pd.setMessage("Saving");

                    client.dataStore(BookDTO.COLLECTION, BookDTO.class).save(new BookDTO(name.getText().toString()),
                            new KinveyClientCallback<BookDTO>() {
                                @Override
                                public void onSuccess(BookDTO result) {
                                    pd.dismiss();
                                    finish();
                                }

                                @Override
                                public void onFailure(Throwable error) {
                                    pd.dismiss();
                                    Toast.makeText(Book.this, "Can't save: "+ error.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            });
                }
                break;
        }
    }
}
