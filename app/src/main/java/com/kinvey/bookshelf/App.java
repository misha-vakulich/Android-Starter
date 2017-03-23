package com.kinvey.bookshelf;

import android.support.multidex.MultiDexApplication;
import android.util.Log;

import com.kinvey.android.Client;
import com.kinvey.android.callback.KinveyClientBuilderCallback;

/**
 * Created by Prots on 3/15/16.
 */
public class App extends MultiDexApplication {

    public static final String TAG = "BOOKSHELF_APP: ";

    Client sharedClient;
    boolean isFirstRun = true;

    @Override
    public void onCreate() {
        super.onCreate();
        sharedClient = new Client.Builder(this).build();
    }

    public Client getSharedClient(){
        return sharedClient;
    }

    public boolean isFirstRun() {
        return isFirstRun;
    }

    public void setFirstRun(boolean firstRun) {
        isFirstRun = firstRun;
    }
}
