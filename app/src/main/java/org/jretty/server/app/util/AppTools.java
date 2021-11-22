package org.jretty.server.app.util;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.jretty.server.app.R;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;

public class AppTools {
    private static final String TAG = "Jetty";

    public static void showServiceToast(Context context, int messageId) {
        showToast(context, messageId, Toast.LENGTH_LONG);
    }

    private static void showToast(Context context, int messageId, int duration) {
        final View view = LayoutInflater.from(context).inflate(R.layout.service_toast, null);
        ((TextView) view.findViewById(R.id.message)).setText(messageId);

        Toast toast = new Toast(context);
        toast.setDuration(duration);
        toast.setView(view);

        toast.show();
    }

    public static void showQuickToast(Context context, int messageId) {
        showToast(context, messageId, Toast.LENGTH_SHORT);
    }

    public static String formatJettyInfoLine(String format, Object... args) {
        String ms = "";
        if (format != null)
            ms = String.format(format, args);
        return ms + "<br/>";
    }

    public static void printNetworkInterfaces(StringBuilder consoleBuffer) {
        try {
            Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface ni : Collections.list(nis)) {
                Enumeration<InetAddress> iis = ni.getInetAddresses();
                for (InetAddress ia : Collections.list(iis)) {
                    consoleBuffer.append(formatJettyInfoLine("Network interface: %s: %s", ni.getDisplayName(), ia.getHostAddress()));
                }
            }
        } catch (SocketException e) {
            Log.w(TAG, e);
        }
    }
}
