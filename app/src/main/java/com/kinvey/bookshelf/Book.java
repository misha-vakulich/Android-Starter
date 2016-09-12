package com.kinvey.bookshelf;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.kinvey.android.Client;
import com.kinvey.android.callback.KinveyDeleteCallback;
import com.kinvey.android.store.AsyncDataStore;
import com.kinvey.java.core.DownloaderProgressListener;
import com.kinvey.java.core.KinveyClientCallback;
import com.kinvey.java.core.MediaHttpDownloader;
import com.kinvey.java.core.MediaHttpUploader;
import com.kinvey.java.core.UploaderProgressListener;
import com.kinvey.java.model.FileMetaData;
import com.kinvey.java.store.StoreType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Prots on 3/15/16.
 */
public class Book extends AppCompatActivity implements View.OnClickListener {

    private static final String FILE_ID = "ff272245-a0d2-446d-9128-fc195e37a344";
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    Client client;

    EditText name;
    ImageView image;

    BookDTO book = new BookDTO();

    AsyncDataStore<BookDTO> bookStore;
    FileMetaData fileMetaData;
    File outputFile;
    // Storage Permissions

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.book);
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        client =  ((App)getApplication()).getSharedClient();

        name = (EditText) findViewById(R.id.name);
        image = (ImageView) findViewById(R.id.imageView);

        findViewById(R.id.save2).setOnClickListener(this);
        findViewById(R.id.save2cache).setOnClickListener(this);
        findViewById(R.id.get_from_cache).setOnClickListener(this);
        findViewById(R.id.clear_cache).setOnClickListener(this);
        findViewById(R.id.upload_to_internet).setOnClickListener(this);
        findViewById(R.id.download_from_internet).setOnClickListener(this);
        findViewById(R.id.remove).setOnClickListener(this);

        bookStore = client.dataStore(BookDTO.COLLECTION, BookDTO.class, StoreType.SYNC);
        verifyStoragePermissions(this);
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
        if (id != null){
            pd.show();
            bookStore.find(id, new KinveyClientCallback<BookDTO>() {
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
        switch (v.getId()){
            case R.id.save2:
                if (name.getText().toString().trim().isEmpty()){
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
            case R.id.upload_to_internet:
                uploadFileToNetwork();
                break;
            case R.id.download_from_internet:
                try {
                    downloadFileFromNetwork();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.save2cache:
                try {
                    saveToCache();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.get_from_cache:
                try {
                    getFromCache();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.clear_cache:
                    clearCache();
                break;
            case R.id.remove:
                try {
                    remove();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    private void remove() throws IOException {
        client.getFileStore(StoreType.NETWORK).remove(fileMetaData, new KinveyDeleteCallback() {
            @Override
            public void onSuccess(Integer integer) {
                Toast.makeText(getApplication(), "remove: onSuccess", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Throwable throwable) {
                Toast.makeText(getApplication(), "remove: onFailure", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void clearCache() {
        client.getFileStore(StoreType.CACHE).clear();
        Toast.makeText(getApplication(), "clearCache", Toast.LENGTH_SHORT).show();
    }

    private void getFromCache() throws IOException {
        outputFile = new File(Environment.getExternalStorageDirectory() + "/Kinvey/", "test"+"CACHE"+".jpg");
        if (!outputFile.exists()){
            outputFile.createNewFile();
        }
        final FileOutputStream fos = new FileOutputStream(outputFile);
        client.getFileStore(StoreType.CACHE).download(fileMetaData, fos, new KinveyClientCallback<FileMetaData>() {
            @Override
            public void onSuccess(FileMetaData fileMetaData) {
                try {
                    fos.write(outputFile.getAbsolutePath().getBytes());
                    setImage(outputFile);
                    Toast.makeText(getApplication(), "getFromCache: onSuccess", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                Toast.makeText(getApplication(), "getFromCache: onFailure", Toast.LENGTH_SHORT).show();
                if (outputFile.exists()){
                    outputFile.delete();
                }
            }
        }, new DownloaderProgressListener() {
            @Override
            public void progressChanged(MediaHttpDownloader mediaHttpDownloader) throws IOException {

            }

            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(getApplication(), "getFromCache: onSuccess", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Throwable throwable) {
                Toast.makeText(getApplication(), "getFromCache: onFailure", Toast.LENGTH_SHORT).show();
                if (outputFile.exists()){
                    outputFile.delete();
                }
            }
        });
    }

    private void saveToCache() throws IOException {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Uploading");
        pd.show();
        AssetManager am = getAssets();
        InputStream inputStream = null;
        try {
            inputStream = am.open("testImage.jpg");
        } catch (IOException e) {
            e.printStackTrace();
        }
        File file = createFileFromInputStream(inputStream);
        client.getFileStore(StoreType.CACHE).upload(file, new KinveyClientCallback<FileMetaData>() {
            @Override
            public void onSuccess(FileMetaData metaData) {
                fileMetaData = metaData;
                pd.dismiss();
                Toast.makeText(getApplication(), "saveToCache: onSuccess", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Throwable throwable) {
                Toast.makeText(getApplication(), "saveToCache: onFailure", Toast.LENGTH_SHORT).show();
            }
        }, new UploaderProgressListener() {
            @Override
            public void progressChanged(MediaHttpUploader mediaHttpUploader) throws IOException {

            }

            @Override
            public void onSuccess(FileMetaData metaData) {
                Toast.makeText(getApplication(), "saveToCache: onSuccess", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Throwable throwable) {
                Toast.makeText(getApplication(), "saveToCache: onFailure", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void downloadFileFromNetwork() throws IOException {
        outputFile = new File(Environment.getExternalStorageDirectory() + "/Kinvey/", "test"+"NETWORK"+".jpg");
        if (!outputFile.exists()){
            outputFile.createNewFile();
        }
        final FileOutputStream fos = new FileOutputStream(outputFile);
        client.getFileStore(StoreType.NETWORK).download(fileMetaData, fos, new KinveyClientCallback<FileMetaData>() {
            @Override
            public void onSuccess(FileMetaData fileMetaData) {

                try {
                    fos.write(outputFile.getAbsolutePath().getBytes());
                    setImage(outputFile);
                    Toast.makeText(getApplication(), "downloadFileFromNetwork: onSuccess", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onFailure(Throwable throwable) {
                Toast.makeText(getApplication(), "downloadFileFromNetwork: onFailure", Toast.LENGTH_SHORT).show();
            }
        }, new DownloaderProgressListener() {
            @Override
            public void progressChanged(MediaHttpDownloader mediaHttpDownloader) throws IOException {

            }

            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(getApplication(), "downloadFileFromNetwork: onSuccess", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Throwable throwable) {
                Toast.makeText(getApplication(), "downloadFileFromNetwork: onFailure", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void uploadFileToNetwork() {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Uploading");
        pd.show();
        AssetManager am = getAssets();
        InputStream inputStream = null;
        try {
            inputStream = am.open("testImage.jpg");
        } catch (IOException e) {
            e.printStackTrace();
        }
        File file = createFileFromInputStream(inputStream);
        try {
            assert file != null;
            client.getFileStore(StoreType.NETWORK).upload(file, new KinveyClientCallback<FileMetaData>() {
                @Override
                public void onSuccess(FileMetaData metaData) {
                    fileMetaData = metaData;
                    pd.dismiss();
                    Toast.makeText(getApplication(), "uploadFileToNetwork: onSuccess", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFailure(Throwable throwable) {
                    Toast.makeText(getApplication(), "uploadFileToNetwork: onFailure", Toast.LENGTH_SHORT).show();
                    pd.dismiss();
                }
            }, new UploaderProgressListener() {
                @Override
                public void progressChanged(MediaHttpUploader mediaHttpUploader) throws IOException {

                    Log.d("progressChanged", " progress: " + mediaHttpUploader.getProgress());
                }

                @Override
                public void onSuccess(FileMetaData metaData) {
                    fileMetaData = metaData;
                    Toast.makeText(getApplication(), "uploadFileToNetwork: onSuccess", Toast.LENGTH_SHORT).show();
                    pd.dismiss();
                }

                @Override
                public void onFailure(Throwable throwable) {
                    Toast.makeText(getApplication(), "uploadFileToNetwork: onFailure", Toast.LENGTH_SHORT).show();
                    pd.dismiss();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private File createFileFromInputStream(InputStream inputStream) {

        try{
            File f = new File(getCacheDir(), "testImage.jpg");
            OutputStream outputStream = new FileOutputStream(f);
            byte buffer[] = new byte[2048];
            int length = 0;

            while((length=inputStream.read(buffer)) > 0) {
                outputStream.write(buffer,0,length);
            }

            outputStream.close();
            inputStream.close();

            return f;
        }catch (IOException e) {
            //Logging exception
        }

        return null;
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
                public void onSuccess(Integer integer) {
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

    private void setImage(File file) {
        if(file.exists()){
            Bitmap myBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            image.setImageBitmap(myBitmap);
        }
    }


    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
}
