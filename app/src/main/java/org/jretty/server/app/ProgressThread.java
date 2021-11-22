package org.jretty.server.app;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.eclipse.jetty.util.IO;
import org.jretty.server.app.util.FileTools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * ProgressThread
 * <p>
 * Handles finishing install tasks for Jetty.
 */
public class ProgressThread extends Thread {
    private static final String TAG = "Jetty";
    private Activity activity;
    private Handler _handler;

    public ProgressThread(Activity activity, Handler h) {
        this.activity = activity;
        _handler = h;
    }

    public void sendProgressUpdate(int prog) {
        Message msg = _handler.obtainMessage();
        Bundle b = new Bundle();
        b.putInt("prog", prog);
        msg.setData(b);
        _handler.sendMessage(msg);
    }

    public void run() {
        boolean updateNeeded = isUpdateNeeded(activity);

        //create the jetty dir structure
        File jettyDir = FileTools.getJettyDir();
        if (!jettyDir.exists()) {
            boolean made = jettyDir.mkdirs();
            Log.i(TAG, "Made " + FileTools.getJettyDirPath() + ": " + made);
        }

        Log.i(TAG, "ExternalStorageDirectory = " + Environment.getExternalStorageDirectory());
        Log.i(TAG, "jettyDir = " + jettyDir.getAbsolutePath());
        Log.i(TAG, "jettyDir/contexts .exists(): " + new File(jettyDir, FileTools.__CONTEXTS_DIR).exists());

        sendProgressUpdate(10);

        //Do not make a work directory to preserve unpacked
        //webapps - this seems to clash with Android when
        //out-of-date webapps are deleted and then re-unpacked
        //on a jetty restart: Android remembers where the dex
        //file of the old webapp was installed, but it's now
        //been replaced by a new file of the same name. Strangely,
        //this does not seem to affect webapps unpacked to tmp?
        //Original versions of i-jetty created a work directory. So
        //we will delete it here if found to ensure webapps can be
        //updated successfully.
        File workDir = new File(jettyDir, FileTools.__WORK_DIR);
        if (workDir.exists()) {
            WebappInstaller.delete(workDir);
            Log.i(TAG, "removed work dir");
        }

        //make jetty/tmp
        File tmpDir = new File(jettyDir, FileTools.__TMP_DIR);
        if (!tmpDir.exists()) {
            boolean made = tmpDir.mkdirs();
            Log.i(TAG, "Made " + tmpDir + ": " + made);
        } else {
            Log.i(TAG, tmpDir + " exists");
        }

        //make jetty/webapps
        File webappsDir = new File(jettyDir, FileTools.__WEBAPP_DIR);
        if (!webappsDir.exists()) {
            boolean made = webappsDir.mkdirs();
            Log.i(TAG, "Made " + webappsDir + ": " + made);
        } else {
            Log.i(TAG, webappsDir + " exists");
        }

        //make jetty/etc
        File etcDir = new File(jettyDir, FileTools.__ETC_DIR);
        if (!etcDir.exists()) {
            boolean made = etcDir.mkdirs();
            Log.i(TAG, "Made " + etcDir + ": " + made);
        } else {
            Log.i(TAG, etcDir + " exists");
        }
        sendProgressUpdate(30);

        File webdefaults = new File(etcDir, "webdefault.xml");
        if (!webdefaults.exists() || updateNeeded) {
            //get the webdefaults.xml file out of resources
            try {
                InputStream is = activity.getResources().openRawResource(R.raw.webdefault);
                OutputStream os = new FileOutputStream(webdefaults);
                IO.copy(is, os);
                Log.i(TAG, "Loaded webdefault.xml");
            } catch (Exception e) {
                Log.e(TAG, "Error loading webdefault.xml", e);
            }
        }
        sendProgressUpdate(40);

        File realm = new File(etcDir, "realm.properties");
        if (!realm.exists() || updateNeeded) {
            try {
                //get the realm.properties file out resources
                InputStream is = activity.getResources().openRawResource(R.raw.realm_properties);
                OutputStream os = new FileOutputStream(realm);
                IO.copy(is, os);
                Log.i(TAG, "Loaded realm.properties");
            } catch (Exception e) {
                Log.e(TAG, "Error loading realm.properties", e);
            }
        }
        sendProgressUpdate(50);

        File keystore = new File(etcDir, "keystore");
        if (!keystore.exists() || updateNeeded) {
            try {
                //get the keystore out of resources
                InputStream is = activity.getResources().openRawResource(R.raw.keystore);
                OutputStream os = new FileOutputStream(keystore);
                IO.copy(is, os);
                Log.i(TAG, "Loaded keystore");
            } catch (Exception e) {
                Log.e(TAG, "Error loading keystore", e);
            }
        }
        sendProgressUpdate(60);

        //make jetty/contexts
        File contextsDir = new File(jettyDir, FileTools.__CONTEXTS_DIR);
        if (!contextsDir.exists()) {
            boolean made = contextsDir.mkdirs();
            Log.i(TAG, "Made " + contextsDir + ": " + made);
        } else {
            Log.i(TAG, contextsDir + " exists");
        }
        sendProgressUpdate(70);

        try {
            PackageInfo pi = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            if (pi != null) {
                FileTools.setStoredJettyVersion(pi.versionCode);
            }
        } catch (Exception e) {
            Log.w(TAG, "Unable to get PackageInfo for i-jetty");
        }

        //if there was a .update file indicating an update was needed, remove it now we've updated
        File update = new File(FileTools.getJettyDir(), ".update");
        if (update.exists())
            update.delete();

        sendProgressUpdate(100);
    }


    /**
     * We need to an update iff we don't know the current
     * jetty version or it is different to the last version
     * that was installed.
     *
     * @return
     */
    public static boolean isUpdateNeeded(Activity activity) {
        //if /sdcard/jetty/.update file exists, then update is required
        File alwaysUpdate = new File(FileTools.getJettyDir(), ".update");
        if (alwaysUpdate.exists()) {
            Log.i(TAG, "Always Update tag found " + alwaysUpdate);
            return true;
        }
        //if no previous version file, assume update is required
        int storedVersion = FileTools.getStoredJettyVersion();
        if (storedVersion <= 0) {
            return true;
        }
        try {
            //if different previous version, update is required
            PackageInfo pi = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
            if (pi == null || pi.versionCode != storedVersion)
                return true;
        } catch (Exception e) {
            //if any of these tests go wrong, best to assume update is true?
            return true;
        }
        return false;
    }

}
