package com.kinvey.bookshelf;

import android.support.multidex.MultiDexApplication;

import com.kinvey.android.Client;

/**
 * Created by Prots on 3/15/16.
 */
public class App extends MultiDexApplication {

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
