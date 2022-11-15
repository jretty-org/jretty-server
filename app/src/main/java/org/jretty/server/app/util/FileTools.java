package org.jretty.server.app.util;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class FileTools {
    private static final String TAG = "Jetty";
    public static final String __WORK_DIR = "work";
    public static final String __TMP_DIR = "tmp";
    public static final String __WEBAPP_DIR = "webapps";
    public static final String __ETC_DIR = "etc";
    public static final String __CONTEXTS_DIR = "contexts";

    private volatile static File __JETTY_DIR;

    public static void initJettyDir() {
        __JETTY_DIR = new File(Environment.getExternalStorageDirectory(), "jetty");
    }

    public static File getJettyDir() {
        if (__JETTY_DIR == null) {
            initJettyDir();
        }
        return __JETTY_DIR;
    }

    public static String getJettyDirPath() {
        if (__JETTY_DIR == null) {
            initJettyDir();
        }
        return __JETTY_DIR.getAbsolutePath();
    }

    public static int getStoredJettyVersion() {
        File jettyDir = getJettyDir();
        if (!jettyDir.exists()) {
            return -1;
        }
        File versionFile = new File(jettyDir, "version.code");
        if (!versionFile.exists()) {
            return -1;
        }
        int val = -1;
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(new FileInputStream(versionFile));
            val = ois.readInt();
            return val;
        } catch (Exception e) {
            Log.e(TAG, "Problem reading version.code", e);
            return -1;
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (Exception e) {
                    Log.d(TAG, "Error closing version.code input stream", e);
                }
            }
        }
    }

    public static void setStoredJettyVersion(int version) {
        File jettyDir = getJettyDir();
        if (!jettyDir.exists()) {
            return;
        }
        File versionFile = new File(jettyDir, "version.code");
        ObjectOutputStream oos = null;
        try {
            FileOutputStream fos = new FileOutputStream(versionFile);
            oos = new ObjectOutputStream(fos);
            oos.writeInt(version);
            oos.flush();
        } catch (Exception e) {
            Log.e(TAG, "Problem writing jetty version", e);
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (Exception e) {
                    Log.d(TAG, "Error closing version.code output stream", e);
                }
            }
        }
    }


}
