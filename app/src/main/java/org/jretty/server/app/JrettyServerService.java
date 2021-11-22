package org.jretty.server.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import org.eclipse.jetty.http.HttpGenerator;
import org.eclipse.jetty.http.ssl.SslContextFactory;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.util.security.Credential;
import org.jretty.server.core.handler.DefaultHandler;
import org.jretty.server.app.util.AppTools;
import org.jretty.server.app.util.FileTools;
import org.jretty.server.core.deployer.AndroidContextDeployer;
import org.jretty.server.core.deployer.AndroidWebAppDeployer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * IJettyService
 * <p>
 * Android Service which runs the Jetty server, maintaining it in the active Notifications so that
 * the user can return to the IJetty Activity to control it at any time.
 */
public class JrettyServerService extends Service {
    private static final String TAG = "Jetty";

    private static Resources __resources;
    private static final String CONTENT_RESOLVER_ATTRIBUTE = "org.jretty.server.app.contentResolver";
    private static final String ANDROID_CONTEXT_ATTRIBUTE = "org.jretty.server.app.context";

    public static final int __START_PROGRESS_DIALOG = 0;
    public static final int __STARTED = 0;
    public static final int __NOT_STARTED = 1;
    public static final int __STOPPED = 2;
    public static final int __NOT_STOPPED = 3;
    public static final int __STARTING = 4;
    public static final int __STOPPING = 5;

    public static final String[] __configurationClasses =
            new String[]
                    {
                            "org.jretty.server.core.webapp.AndroidWebInfConfiguration",
                            "org.eclipse.jetty.webapp.WebXmlConfiguration",
                            "org.eclipse.jetty.webapp.JettyWebXmlConfiguration",
                            "org.eclipse.jetty.webapp.TagLibConfiguration"
                    };

    private static boolean __isRunning;

    private NotificationManager mNM;
    private Server server;
    private ContextHandlerCollection contexts;
    private boolean _useNIO;
    private boolean _useSSL;
    private int _port;
    private int _sslPort;
    private String _consolePassword;
    private String _keymgrPassword;
    private String _keystorePassword;
    private String _truststorePassword;
    private String _keystoreFile;
    private String _truststoreFile;
    private SharedPreferences preferences;
    private PackageInfo pi;
    private android.os.Handler _handler;

    private PowerManager.WakeLock wakeLock;
    private final IBinder binder = new LocalBinder();

    /**
     * IJettyService always runs in-process with the IJetty activity.
     */
    public class LocalBinder extends Binder {
        JrettyServerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return JrettyServerService.this;
        }
    }

    public JrettyServerService() {
        super();
        _handler = new android.os.Handler() {
            public void handleMessage(Message msg) {
                switch (msg.getData().getInt("state")) {
                    case __STARTED: {
                        AppTools.showServiceToast(JrettyServerService.this, R.string.jetty_started);
                        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        // The PendingIntent to launch IJetty activity if the user selects this notification
                        PendingIntent contentIntent = PendingIntent.getActivity(JrettyServerService.this, 0,
                                new Intent(JrettyServerService.this, MainActivity.class), 0);

                        CharSequence text = getText(R.string.manage_jetty);

                        Notification notification = new Notification(R.drawable.ijetty_stat,
                                text,
                                System.currentTimeMillis());

//                        notification.setLatestEventInfo(IJettyService.this, getText(R.string.app_name),
//                                text, contentIntent);
//
//                        mNM.notify(R.string.jetty_started, notification);

                        Notification.Builder builder = new Notification.Builder(JrettyServerService.this);
                        builder.setContentTitle(getText(R.string.app_name));
                        builder.setContentText(text);
                        builder.setSmallIcon(R.drawable.icon                                        );
                        builder.setContentIntent(contentIntent);//执行intent
                        notification.flags |= Notification.FLAG_AUTO_CANCEL;//点击通知后通知消失
                        mNM.notify(R.string.jetty_started, notification);


                        Intent startIntent = new Intent(MainActivity.__START_ACTION);
                        startIntent.addCategory("default");
                        Connector[] connectors = server.getConnectors();
                        if (connectors != null) {
                            String[] tmp = new String[connectors.length];

                            for (int i = 0; i < connectors.length; i++)
                                tmp[i] = connectors[i].toString();

                            startIntent.putExtra("connectors", tmp);
                        }

                        sendBroadcast(startIntent);
                        break;
                    }
                    case __NOT_STARTED: {
                        AppTools.showServiceToast(JrettyServerService.this, R.string.jetty_not_started);
                        break;
                    }
                    case __STOPPED: {
                        // Cancel the persistent notification.
                        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                        mNM.cancel(R.string.jetty_started);
                        // Tell the user we stopped.
                        AppTools.showServiceToast(JrettyServerService.this, R.string.jetty_stopped);
                        Intent stopIntent = new Intent(MainActivity.__STOP_ACTION);
                        stopIntent.addCategory("default");
                        sendBroadcast(stopIntent);
                        break;
                    }

                    case __NOT_STOPPED: {
                        AppTools.showServiceToast(JrettyServerService.this, R.string.jetty_not_stopped);
                        break;
                    }
                    case __STARTING: {
                        AppTools.showServiceToast(JrettyServerService.this, R.string.jetty_starting);
                        break;
                    }
                    case __STOPPING: {
                        AppTools.showServiceToast(JrettyServerService.this, R.string.jetty_stopping);
                        break;
                    }
                }

            }

        };
    }


    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * Android Service create
     * @see Service#onCreate()
     */
    public void onCreate() {
        __resources = getResources();
        try {
            pi = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (Exception e) {
            Log.e(TAG, "Unable to determine running jetty version");
        }
    }

    /**
     * Android Service Start
     *
     * @see Service#onStart(Intent, int)
     */
    public void onStart(Intent intent, int startId) {
        if (server != null) {
            AppTools.showServiceToast(JrettyServerService.this, R.string.jetty_already_started);
            return;
        }

        try {
            preferences = PreferenceManager.getDefaultSharedPreferences(this);

            String portDefault = getText(R.string.pref_port_value).toString();
            String sslPortDefault = getText(R.string.pref_ssl_port_value).toString();
            String pwdDefault = getText(R.string.pref_console_pwd_value).toString();

            String nioEnabledDefault = getText(R.string.pref_nio_value).toString();
            String sslEnabledDefault = getText(R.string.pref_ssl_value).toString();

            String portKey = getText(R.string.pref_port_key).toString();
            String sslPortKey = getText(R.string.pref_ssl_port_key).toString();
            String pwdKey = getText(R.string.pref_console_pwd_key).toString();
            String nioKey = getText(R.string.pref_nio_key).toString();
            String sslKey = getText(R.string.pref_ssl_key).toString();

            _useSSL = preferences.getBoolean(sslKey, Boolean.valueOf(sslEnabledDefault));
            _useNIO = preferences.getBoolean(nioKey, Boolean.valueOf(nioEnabledDefault));
            _port = Integer.parseInt(preferences.getString(portKey, portDefault));
            if (_useSSL) {
                _sslPort = Integer.parseInt(preferences.getString(sslPortKey, sslPortDefault));
                String defaultValue = getText(R.string.pref_keystore_pwd_value).toString();
                String key = getText(R.string.pref_keystore_pwd_key).toString();
                _keystorePassword = preferences.getString(key, defaultValue);

                defaultValue = getText(R.string.pref_keymgr_pwd_value).toString();
                key = getText(R.string.pref_keymgr_pwd_key).toString();
                _keymgrPassword = preferences.getString(key, defaultValue);

                defaultValue = getText(R.string.pref_truststore_pwd_value).toString();
                key = getText(R.string.pref_truststore_pwd_key).toString();
                _truststorePassword = preferences.getString(key, defaultValue);

                defaultValue = getText(R.string.pref_keystore_file).toString();
                key = getText(R.string.pref_keystore_file_key).toString();
                _keystoreFile = preferences.getString(key, defaultValue);

                defaultValue = getText(R.string.pref_truststore_file).toString();
                key = getText(R.string.pref_truststore_file_key).toString();
                _truststoreFile = preferences.getString(key, defaultValue);
            }

            _consolePassword = preferences.getString(pwdKey, pwdDefault);

            Log.d("Jetty", "pref port = " + _port);
            Log.d("Jetty", "pref use nio = " + _useNIO);
            Log.d("Jetty", "pref use ssl = " + _useSSL);
            Log.d("Jetty", "pref ssl port = " + _sslPort);

            //Get a wake lock to stop the cpu going to sleep
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "IJetty:mywakelocktag");
            wakeLock.acquire();

            new JettyStarterThread(_handler).start();

            super.onStart(intent, startId);
        } catch (Exception e) {
            Log.e(TAG, "Error starting jetty", e);
            AppTools.showServiceToast(JrettyServerService.this, R.string.jetty_not_started);
        }
    }

    /**
     * Android Service destroy
     *
     * @see Service#onDestroy()
     */
    public void onDestroy() {
        try {
            if (wakeLock != null) {
                wakeLock.release();
                wakeLock = null;
            }

            if (server != null) {
                new JettyStopperThread(_handler).start();

            } else {
                Log.i(TAG, "Jetty not running");
                AppTools.showServiceToast(JrettyServerService.this, R.string.jetty_not_running);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping jetty", e);
            AppTools.showServiceToast(JrettyServerService.this, R.string.jetty_not_stopped);
        }
    }

    public void onLowMemory() {
        Log.i(TAG, "Low on memory");
        super.onLowMemory();
    }

    /**
     * Get a reference to the Jetty Server instance
     *
     * @return
     */
    public Server getServer() {
        return server;
    }

    /**
     * JettyStarterThread
     */
    public class JettyStarterThread extends Thread {
        android.os.Handler _handler;

        public JettyStarterThread(android.os.Handler handler) {
            _handler = handler;
        }

        public void run() {
            try {
                sendMessage(__STARTING);
                startJetty();
                sendMessage(__STARTED);

                Log.i(TAG, "Jetty started");
            } catch (Exception e) {
                sendMessage(__NOT_STARTED);
                Log.e(TAG, "Error starting jetty", e);

            }
        }

        public void sendMessage(int state) {
            Message msg = _handler.obtainMessage();
            Bundle b = new Bundle();
            b.putInt("state", state);
            msg.setData(b);
            _handler.sendMessage(msg);
        }
    }


    /**
     * JettyStopperThread
     */
    public class JettyStopperThread extends Thread {
        android.os.Handler _handler;

        public JettyStopperThread(android.os.Handler handler) {
            _handler = handler;
        }

        public void run() {
            try {
                sendMessage(__STOPPING);
                stopJetty();
                Log.i(TAG, "Jetty stopped");
                sendMessage(__STOPPED);
            } catch (Exception e) {
                sendMessage(__NOT_STOPPED);
                Log.e(TAG, "Error stopping jetty", e);
            }
        }

        public void sendMessage(int state) {
            Message msg = _handler.obtainMessage();
            Bundle b = new Bundle();
            b.putInt("state", state);
            msg.setData(b);
            _handler.sendMessage(msg);
        }
    }


    /**
     * Hack to get around bug in ResourceBundles
     *
     * @param id
     * @return
     */
    public static InputStream getStreamToRawResource(int id) {
        if (__resources != null)
            return __resources.openRawResource(id);
        else
            return null;
    }


    public static boolean isRunning() {
        return __isRunning;
    }


    protected Server newServer() {
        return new Server();
    }

    protected ContextHandlerCollection newContexts() {
        return new ContextHandlerCollection();
    }


    protected void configureConnectors() {
        if (server != null) {
            if (_useNIO) {
                SelectChannelConnector nioConnector = new SelectChannelConnector();
                nioConnector.setUseDirectBuffers(false);
                nioConnector.setPort(_port);
                server.addConnector(nioConnector);
                Log.i(TAG, "Configured " + SelectChannelConnector.class.getName() + " on port " + _port);
            } else {
                SocketConnector bioConnector = new SocketConnector();
                bioConnector.setPort(_port);
                server.addConnector(bioConnector);
                Log.i(TAG, "Configured " + SocketConnector.class.getName() + " on port " + _port);
            }

            if (_useSSL) {
                SslContextFactory sslContextFactory = new SslContextFactory();
                sslContextFactory.setKeyStore(_keystoreFile);
                sslContextFactory.setTrustStore(_truststoreFile);
                sslContextFactory.setKeyStorePassword(_keystorePassword);
                sslContextFactory.setKeyManagerPassword(_keymgrPassword);
                sslContextFactory.setKeyStoreType("bks");
                sslContextFactory.setTrustStorePassword(_truststorePassword);
                sslContextFactory.setTrustStoreType("bks");

                //TODO SslSelectChannelConnector does not work on android 1.6, but does work on android 2.2
                if (_useNIO) {
                    SslSelectChannelConnector sslConnector = new SslSelectChannelConnector(sslContextFactory);
                    sslConnector.setPort(_sslPort);
                    server.addConnector(sslConnector);
                    Log.i(TAG, "Configured " + sslConnector.getClass().getName() + " on port " + _sslPort);
                } else {
                    SslSocketConnector sslConnector = new SslSocketConnector(sslContextFactory);
                    sslConnector.setPort(_sslPort);
                    server.addConnector(sslConnector);
                    Log.i(TAG, "Configured " + sslConnector.getClass().getName() + " on port " + _sslPort);
                }

            }
        }
    }

    protected void configureHandlers() {
        if (server != null) {
            HandlerCollection handlers = new HandlerCollection();
            contexts = new ContextHandlerCollection();
            handlers.setHandlers(new Handler[]{contexts, new DefaultHandler()});
            server.setHandler(handlers);
        }
    }

    protected void configureDeployers(Server server) throws Exception {
        AndroidWebAppDeployer staticDeployer = new AndroidWebAppDeployer();
        AndroidContextDeployer contextDeployer = new AndroidContextDeployer();

        File jettyDir = FileTools.getJettyDir();
        Log.i(TAG, "jettyDir exists = " + jettyDir.exists());
        if (jettyDir.exists()) {
            Log.i(TAG, "ExternalStorageDirectory = " + Environment.getExternalStorageDirectory());
            Log.i(TAG, "jettyDir = " + jettyDir.getAbsolutePath());
            Log.i(TAG, "jettyDir/contexts .exists(): " + new File(jettyDir, FileTools.__CONTEXTS_DIR).exists());

            // If the webapps dir exists, start the static webapp deployer
            if (new File(jettyDir, FileTools.__WEBAPP_DIR).exists()) {
                staticDeployer.setWebAppDir(FileTools.getJettyDirPath() + "/" + FileTools.__WEBAPP_DIR);
                staticDeployer.setDefaultsDescriptor(FileTools.getJettyDirPath() + "/" + FileTools.__ETC_DIR + "/webdefault.xml");
                staticDeployer.setContexts(contexts);
                staticDeployer.setAttribute(CONTENT_RESOLVER_ATTRIBUTE, getContentResolver());
                staticDeployer.setAttribute(ANDROID_CONTEXT_ATTRIBUTE, (Context) JrettyServerService.this);
                staticDeployer.setConfigurationClasses(JrettyServerService.__configurationClasses);
                staticDeployer.setAllowDuplicates(false);
            }

            // Use a ContextDeploy so we can hot-deploy webapps and config at startup.
            if (new File(jettyDir, FileTools.__CONTEXTS_DIR).exists()) {
                contextDeployer.setScanInterval(10); // Don't eat the battery
                contextDeployer.setConfigurationDir(FileTools.getJettyDirPath() + "/" + FileTools.__CONTEXTS_DIR);
                contextDeployer.setAttribute(CONTENT_RESOLVER_ATTRIBUTE, getContentResolver());
                contextDeployer.setAttribute(ANDROID_CONTEXT_ATTRIBUTE, (Context) JrettyServerService.this);
                contextDeployer.setContexts(contexts);
            }

            if (server != null) {
                Log.i(TAG, "Adding context deployer: ");
                server.addBean(contextDeployer);
                Log.i(TAG, "Adding webapp deployer: ");
                server.addBean(staticDeployer);
            }
        } else {
            Log.w(TAG, "Not loading any webapps - none on SD card.");
        }
    }

    public void configureRealm(Server server, String _consolePassword) throws IOException {
        File realmProps = new File(FileTools.getJettyDirPath() + "/" + FileTools.__ETC_DIR + "/realm.properties");
        if (realmProps.exists()) {
            HashLoginService realm = new HashLoginService("Console", FileTools.getJettyDirPath() + "/" + FileTools.__ETC_DIR + "/realm.properties");
            realm.setRefreshInterval(0);
            if (_consolePassword != null) { //set the admin password for console webapp
                realm.putUser("admin", Credential.getCredential(_consolePassword), new String[]{"admin"});
            }
            server.addBean(realm);
        }
    }


    protected void startJetty() throws Exception {
        //Set jetty.home
        System.setProperty("jetty.home", FileTools.getJettyDirPath());

        //ipv6 workaround for froyo
        System.setProperty("java.net.preferIPv6Addresses", "false");

        server = newServer();

        configureConnectors();
        configureHandlers();
        configureDeployers(server);
        configureRealm(server, _consolePassword);

        server.start();

        __isRunning = true;

        //TODO
        // Less than ideal solution to the problem that dalvik doesn't know about manifests of jars.
        // A as the version field is private to Server, its difficult
        //if not impossible to set it any other way. Note this means that ContextHandler.SContext.getServerInfo()
        //will still return 0.0.
        HttpGenerator.setServerVersion("i-jetty " + pi.versionName);
    }

    protected void stopJetty() throws Exception {
        try {
            Log.i(TAG, "Jetty stopping");
            server.stop();
            Log.i(TAG, "Jetty server stopped");
            server = null;
            __resources = null;
            __isRunning = false;
        } finally {
            Log.i(TAG, "Finally stopped");
        }
    }


}
