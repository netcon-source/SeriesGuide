/*
 * Copyright 2012 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.uwetrottmann.androidutils;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;

public class AndroidUtils {

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    public static boolean isJellyBeanOrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    public static boolean isICSOrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    public static boolean isHoneycombOrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean isGingerbreadOrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    public static boolean isFroyoOrHigher() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    /**
     * Checks if {@link Environment}.MEDIA_MOUNTED is returned by
     * {@code getExternalStorageState()} and therefore external storage is read-
     * and writeable.
     * 
     * @return
     */
    public static boolean isExtStorageAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /**
     * Whether there is any network with a usable connection.
     * 
     * @param context
     * @return
     */
    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo != null) {
            return activeNetworkInfo.isConnected();
        }
        return false;
    }

    /**
     * Whether WiFi has an active, usable connection.
     * 
     * @param context
     * @return
     */
    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetworkInfo = connectivityManager
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (wifiNetworkInfo != null) {
            return wifiNetworkInfo.isConnected();
        }
        return false;
    }

    /**
     * Copies the contents of one file to the other using {@link FileChannel}s.
     * 
     * @param src source {@link File}
     * @param dst destination {@link File}
     * @throws IOException
     */
    public static void copyFile(File src, File dst) throws IOException {
        FileInputStream in = new FileInputStream(src);
        FileOutputStream out = new FileOutputStream(dst);
        FileChannel inChannel = in.getChannel();
        FileChannel outChannel = out.getChannel();

        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null) {
                inChannel.close();
            }
            if (outChannel != null) {
                outChannel.close();
            }
        }

        in.close();
        out.close();
    }

    /**
     * Copies data from one input stream to the other using a buffer of 8
     * kilobyte in size.
     * 
     * @param input {@link InputStream}
     * @param output {@link OutputStream}
     * @return
     * @throws IOException
     */
    public static int copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    /**
     * Execute an {@link AsyncTask} on a thread pool.
     * 
     * @param task Task to execute.
     * @param args Optional arguments to pass to
     *            {@link AsyncTask#execute(Object[])}.
     * @param <T> Task argument type.
     */
    @TargetApi(11)
    public static <T> void executeAsyncTask(AsyncTask<T, ?, ?> task, T... args) {
        // TODO figure out how to subclass abstract and generalized AsyncTask,
        // then put this there
        if (AndroidUtils.isHoneycombOrHigher()) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, args);
        } else {
            task.execute(args);
        }
    }

    /**
     * Returns an {@link InputStream} using {@link HttpURLConnection} to connect
     * to the given URL.
     */
    public static InputStream downloadUrl(String urlString) throws IOException {
        HttpURLConnection conn = buildHttpUrlConnection(urlString);
        conn.connect();

        InputStream stream = conn.getInputStream();
        return stream;
    }

    /**
     * Returns an {@link HttpURLConnection} using sensible default settings for
     * mobile and taking care of buggy behavior prior to Froyo.
     */
    public static HttpURLConnection buildHttpUrlConnection(String urlString)
            throws MalformedURLException, IOException {
        AndroidUtils.disableConnectionReuseIfNecessary();

        URL url = new URL(urlString);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(10000 /* milliseconds */);
        conn.setConnectTimeout(15000 /* milliseconds */);
        conn.setDoInput(true);
        conn.setRequestMethod("GET");
        return conn;
    }

    /**
     * Prior to Android 2.2 (Froyo), {@link HttpURLConnection} had some
     * frustrating bugs. In particular, calling close() on a readable
     * InputStream could poison the connection pool. Work around this by
     * disabling connection pooling.
     */
    public static void disableConnectionReuseIfNecessary() {
        // HTTP connection reuse which was buggy pre-froyo
        if (!isFroyoOrHigher()) {
            System.setProperty("http.keepAlive", "false");
        }
    }

    /**
     * Display an info toast on long clicking the given view, describing the
     * functionality if the given view would have been clicked instead. It will
     * be positioned just above the view (set your own listener if your view is
     * at the very top of the window).
     */
    public static void setInfoToast(final Context context, View view, final int stringResource) {
        view.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Toast infoToast = Toast.makeText(context, stringResource, Toast.LENGTH_SHORT);

                // position toast near view
                int[] location = new int[2];
                v.getLocationOnScreen(location);
                infoToast.setGravity(Gravity.TOP | Gravity.LEFT, location[0] - v.getWidth() / 2,
                        location[1] - v.getHeight() - v.getHeight() / 2);

                infoToast.show();
                return true;
            }
        });
    }
}
