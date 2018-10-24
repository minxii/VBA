
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;

import java.util.ArrayList;

/**
 * 権限取得画面
 */
public class GrantPermissionActivity extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback {

    boolean permissionGtanted = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // version23以上の場合、手動権限取得が必要
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            verifyPermissions(this);
        } else {
            permissionGtanted = true;
            afterGrantPermission();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (permissionGtanted) {
            afterGrantPermission();
        }
    }

    /**
     * 権限取得後処理
     */
    private void afterGrantPermission() {
        // ログイン画面へ遷移
        Intent intent = new Intent(this.getApplicationContext(), LoginActivity.class);
        startActivity(intent);
    }

    private void verifyPermissions(Activity act) {
        // Camera issue
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
            builder.detectFileUriExposure();
        }

        // Check if we have necessary permissions
        int storagePermissionWrite = ActivityCompat.checkSelfPermission(act, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int storagePermissionRead = ActivityCompat.checkSelfPermission(act, Manifest.permission.READ_EXTERNAL_STORAGE);
        int cameraPermission = ActivityCompat.checkSelfPermission(act, Manifest.permission.CAMERA);

        ArrayList<String> permissions = new ArrayList<>();

        if (storagePermissionWrite != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
        if (storagePermissionRead != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA);
        }

        if (permissions.size() > 0) {
            String[] permissionArray = new String[permissions.size()];
            permissions.toArray(permissionArray);

            ActivityCompat.requestPermissions(act, permissionArray, 1);
        } else {
            permissionGtanted = true;

            afterGrantPermission();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 権限取得できた場合
                // 後続処理
                permissionGtanted = true;
                afterGrantPermission();
            } else {
                // 権限取得拒否の場合
                // アプリ終了
                appKill();
            }
        }
    }

    /**
     * アプリ終了処理
     */
    public void appKill() {
        moveTaskToBack(true);
        this.finish();
        System.exit(0);
    }

}