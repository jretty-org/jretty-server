package org.jretty.server.app.util;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.jretty.server.app.MainActivity;
import org.jretty.server.app.R;

public class PermissionRequestActivity extends Activity {
	private static final String TAG = "Jetty";
	/**
	 * The intent to start after acquiring the required permissions
	 */
	private Intent mCallbackIntent;

	@TargetApi(Build.VERSION_CODES.M)
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mCallbackIntent = getIntent().getExtras().getParcelable("callbackIntent");
		requestPermissions(getNeededPermissions(), 1234);
	}

	/**
	 * Called by Activity after the user interacted with the permission request
	 * Will launch the main activity if all permissions were granted, exits otherwise
	 *
	 * @param requestCode The code set by requestPermissions
	 * @param permissions Names of the permissions we got granted or denied
	 * @param grantResults Results of the permission requests
	 */
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		int grantedPermissions = 0;
		for (int result : grantResults) {
			if (result == PackageManager.PERMISSION_GRANTED)
				grantedPermissions++;
		}

		// set as finished before (possibly) killing ourselfs
		finish();

		if (grantedPermissions == grantResults.length) {


			if (mCallbackIntent != null) {
				// start the old intent but ensure to make it a new task & clear any old attached activites
				mCallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
				startActivity(mCallbackIntent);
			}
			// Hack: We *kill* ourselfs (while launching the main activity) to get startet
			// in a new process: This works around a bug/feature in 6.0 that would cause us
			// to get 'partial read' permissions (eg: reading from the content provider works
			// but reading from /sdcard doesn't)
			android.os.Process.killProcess(android.os.Process.myPid());
		}
	}

	/**
	 * Injects a warning that we are missing read permissions into the activity layout
	 *
	 * @param activity Reference to LibraryActivity
	 * @param intent The intent starting the parent activity
	 */
	public static void showWarning(final MainActivity activity, final Intent intent) {
		LayoutInflater inflater = LayoutInflater.from(activity);
		View view = inflater.inflate(R.layout.permission_request, null, false);

		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				PermissionRequestActivity.requestPermissions(activity, intent);
			}
		});

		ViewGroup parent = (ViewGroup)activity.findViewById(R.id.content); // main layout of library_content
		parent.addView(view, -1);
	}

	/**
	 * Launches a permission request dialog if needed
	 *
	 * @param activity The activitys context to use for the permission check
	 * @return boolean true if we showed a permission request dialog
	 */ 
	public static boolean requestPermissions(Activity activity, Intent callbackIntent) {
		boolean havePermissions = havePermissions(activity);

		if (havePermissions == false) {
			Intent intent = new Intent(activity, PermissionRequestActivity.class);
			intent.putExtra("callbackIntent", callbackIntent);
			activity.startActivity(intent);
		}

		return !havePermissions;
	}

	/**
	 * Checks if all required permissions have been granted
	 *
	 * @param context The context to use
	 * @return boolean true if all permissions have been granded
	 */
	public static boolean havePermissions(Context context) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			for (String permission : getNeededPermissions()) {
				if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
					return false;
				}
			}
		} // else: granted during installation
		return true;
	}

	private static String[] getNeededPermissions() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			return new String[] { Manifest.permission.READ_EXTERNAL_STORAGE };
		}
		return new String[] { Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE };
	}


	@TargetApi(30)
	private static boolean haveAllFilesAccessPermission() {
		return Environment.isExternalStorageManager();
	}

	@TargetApi(30)
	private static void requestAllFilesAccessPermission(Activity activity) {
		Boolean intentFailed = false;
		Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
		intent.setData(Uri.parse("package:" + activity.getPackageName()));
		try {
			ComponentName componentName = intent.resolveActivity(activity.getPackageManager());
			if (componentName != null) {
				String className = componentName.getClassName();
				if (className != null) {
					// Launch "Allow all files access?" dialog.
					activity.startActivity(intent);
					return;
				}
				intentFailed = true;
			} else {
				Log.w(TAG, "Request all files access not supported");
				intentFailed = true;
			}
		} catch (ActivityNotFoundException e) {
			Log.w(TAG, "Request all files access not supported", e);
			intentFailed = true;
		}
		if (intentFailed) {
			// Some devices don't support this request.
			Toast.makeText(activity, R.string.dialog_all_files_access_not_supported, Toast.LENGTH_LONG).show();
		}
	}


}
