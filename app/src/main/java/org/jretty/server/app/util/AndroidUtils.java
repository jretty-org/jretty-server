package org.jretty.server.app.util;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.io.UnsupportedEncodingException;

public class AndroidUtils {

	public static PendingIntent getActivity(Context context, Intent intent) {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
			return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
		}
		return PendingIntent.getActivity(context, 0, intent, 0);
	}

	public static PendingIntent getService(Context context, Intent intent) {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
			return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
		}
		return PendingIntent.getService(context, 0, intent, 0);
	}
}
