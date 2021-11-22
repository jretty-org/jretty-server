package org.jretty.server.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class EditorPreferenceActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (android.os.Build.VERSION.SDK_INT < 8)
            addPreferencesFromResource(R.xml.ijetty_preference);
        else
            addPreferencesFromResource(R.xml.ijetty_preference_froyo);
    }

    public static void show(Context context) {
        final Intent intent = new Intent(context, EditorPreferenceActivity.class);
        context.startActivity(intent);
    }
}
