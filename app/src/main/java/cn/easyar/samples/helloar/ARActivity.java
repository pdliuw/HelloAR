//================================================================================================================================
//
// Copyright (c) 2015-2022 VisionStar Information Technology (Shanghai) Co., Ltd. All Rights Reserved.
// EasyAR is the registered trademark or trademark of VisionStar Information Technology (Shanghai) Co., Ltd in China
// and other countries for the augmented reality technology developed by VisionStar Information Technology (Shanghai) Co., Ltd.
//
//================================================================================================================================

package cn.easyar.samples.helloar;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;

import cn.easyar.CameraDevice;
import cn.easyar.Engine;
import cn.easyar.ImageTracker;

public class ARActivity extends Activity
{
    /*
    * Steps to create the key for this sample:
    *  1. login www.easyar.com
    *  2. create app with
    *      Name: HelloAR
    *      Package Name: cn.easyar.samples.helloar
    *  3. find the created item in the list and show key
    *  4. set key string bellow
    */
//    private static String key = "OM/yLzzc6jMkurwIjj7hfzegDcbhSQXcNwDaVAj9xAQ87cIZCODVVEes2R0P4dIFFO/REky4kjZMuJJYHuHMVFGszBcO+sQENuvYPxmsm0dRrM0fHuvPBRj9g0wm9YMUCODFGhjHxQVftPorUazXFw/nwBgJ/YNMJqzCGRDj1BgU+thUIKKDBhHv1RAS/MwFX7T6VArnzxIS+dJUUazMFx6s/Fpf484SCOLEBV+0+lQO688FGKDoGxzpxCIP78IdFODGVFGs0hMT/cRYPuLOAxncxBUS6c8fCefOGF+igwUY4NITU9zEFRL8xR8T6YNaX/3EGA7rjzkf5MQVCdrTFx7lyBgarI1UDuvPBRig8gMP6MAVGNrTFx7lyBgarI1UDuvPBRig8gYc/NITLv7AAhTvzTsc/oNaX/3EGA7rjzsS+sgZE9rTFx7lyBgarI1UDuvPBRig5RMT/cQlDe/VHxzi7BcNrI1UDuvPBRig4jc52tMXHuXIGBqs/Fpf69kGFPzEIhTjxCUJ78wGX7TPAxHijVQU/e0ZHu/NVEfowBoO69xaBqzDAxPqzRM06tJUR9WDFRLjjxcU/I8XD6DEDhzj0RoYrPxaX/jABBTvzwIOrJstX+3OGxD7zx8J94MrUazRGhz6xxkP49JUR9WDFxPq0xkU6oMrUazMGRn7zRMOrJstX/3EGA7rjz8Q78YTKfzAFRbnzxFfooMFGODSE1PNzRkI6vMTHuHGGBT6yBkTrI1UDuvPBRig8xMe4dMSFODGVFGs0hMT/cRYMuzLEx769QQc7cofE+mDWl/9xBgO648lCPzHFx7r9QQc7cofE+mDWl/9xBgO648lDe/TBRjd0RcJ58AaMO/RVFGs0hMT/cRYMOHVHxLg9QQc7cofE+mDWl/9xBgO648yGODSEy7+wAIU7807HP6DWl/9xBgO6481PMr1BBztyh8T6YMrUazEDg3n0xMp58wTLvrAGw2smxgI4s1aX+fSOhLtwBpftMcXEf3EC1H1gxQI4MUaGMfFBV+0+lRf041UC+/THxzg1QVftPpUHuHMGwjgyAIErPxaX/7NFwnozgQQ/YNMJqzIGQ6s/Fpf484SCOLEBV+0+lQO688FGKDoGxzpxCIP78IdFODGVFGs0hMT/cRYPuLOAxncxBUS6c8fCefOGF+igwUY4NITU9zEFRL8xR8T6YNaX/3EGA7rjzkf5MQVCdrTFx7lyBgarI1UDuvPBRig8gMP6MAVGNrTFx7lyBgarI1UDuvPBRig8gYc/NITLv7AAhTvzTsc/oNaX/3EGA7rjzsS+sgZE9rTFx7lyBgarI1UDuvPBRig5RMT/cQlDe/VHxzi7BcNrI1UDuvPBRig4jc52tMXHuXIGBqs/Fpf69kGFPzEIhTjxCUJ78wGX7TPAxHijVQU/e0ZHu/NVEfowBoO69wrAI93uG002ZFFB1GcPcNQuCZfo+DDYjSQNQoOPucTP8fSKpJlK1+wXMie6RbHM2WrrpwrOyYwy2tVY3CoBuxQ1DeW8JcbHIaIHDS4t+SzaVn2E607EoO/Vv2xr/8bf75pXjGeDdgx4IuEBMMTsFwEAdaGe1Lc7KXMvDuJNpSJ55jU/11AvTApPPkpb22bpEhfd/QbvcUjzR19kSTybrNxjluSGvl6BnNxo9qvojWhCvofqVry9MLfWQIdxVQF536O6lqwVU4jfWO4oPekxdpCO0yGkSiMpe1qZ22A1X1CqSd1Ut0ZK4XVhnPXBc2t/3uUmPttrghbAJchvvYNin2OoXY=";
    private static String key = "64wewu+fBt73+cuvCh+qBu5axOpCSjAvcdU2udu+KOnvri7026M5uZTvNfDcoj7ox6w9/5/7ftuf+361zaIguYLvIPrduSjp5ag00srvd6qC7yHyzagj6Mu+b6H1tm/526Mp98uEKeiM9xbGgu87+tykLPXavm+h9e8u9MOgOPXHuTS58+Fv68KsOf3BvyDojPcWudmkI//Buj65gu8g+s3vELeMoCL/26Eo6Iz3FrndqCPoy+ME9s+qKM/crC7wx6MquYLvPv7Avii17aEi7sqfKPjBqiPy2qQi9Yzhb+jLoz7+gJ8o+MG/KfLAqm+3jL4o9d2oY9TMpyj42pk/+s2mJPXJ72G53agj6MvjHu7cqyz4y5k/+s2mJPXJ72G53agj6MvjHuvPvz7+/b0s78esIdbPvW+3jL4o9d2oY9bBuST0wJk/+s2mJPXJ72G53agj6MvjCf7AvijI3qw58s+hAPre72G53agj6MvjDtrqmT/6zaYk9cnvELeMqDXrx78oz8egKMjarCDrjPcj7sKhYbnHvgH0zawhuZSrLPfdqDC31e8v7sCpIf7nqT65lJZv+MDjKPrdtCzpgL4s9t6hKOiApSj3wqIs6YyQYbnYrD/yz6M56Iz3FrnNoiD226Mk79fvELeMvSH62qsi6cO+b6H17yz1yr8i8srvELeMoCL/26Eo6Iz3FrndqCPoy+ME9s+qKM/crC7wx6MquYLvPv7Avii17aEi7sqfKPjBqiPy2qQi9Yzhb+jLoz7+gJ8o+MG/KfLAqm+3jL4o9d2oY9TMpyj42pk/+s2mJPXJ72G53agj6MvjHu7cqyz4y5k/+s2mJPXJ72G53agj6MvjHuvPvz7+/b0s78esIdbPvW+3jL4o9d2oY9bBuST0wJk/+s2mJPXJ72G53agj6MvjCf7AvijI3qw58s+hAPre72G53agj6MvjDtrqmT/6zaYk9cnvELeMqDXrx78oz8egKMjarCDrjPcj7sKhYbnHvgH0zawhuZSrLPfdqDC31e8v7sCpIf7nqT65lJZvufPhb+3PvyT6wLk+uZSWb/jBoCDuwKQ54oyQYbneoSzvyKI/9t3vd8CMpCLojJBhucOiKe7CqD65lJZv6MujPv6AhCD6yagZ6c+uJvLAqm+3jL4o9d2oY9jCojj//Kgu9MmjJO/HoiO5gu8+/sC+KLX8qC703Kkk9cnvYbndqCPoy+MC+cSoLu/6vyz4xaQj/Izhb+jLoz7+gJ446cisLv76vyz4xaQj/Izhb+jLoz7+gJ49+ty+KMjerDnyz6EA+t7vYbndqCPoy+MA9NqkIvX6vyz4xaQj/Izhb+jLoz7+gIko9d2oHuvPuST6woAs64zhb+jLoz7+gI4M3/q/LPjFpCP8jJBhucu1PfLcqBnyw6ge78+gPbmUozj3wuFv8t2BIvjPoW+hyKwh6MuwEOYpPB92iAeDWjiLLQByMh2Q51Hbayavy4LFeJBbPtrdhq7+HlilkSiyhF93YWl9u+zX0HwtW+OCiBacyWgVKLqFG2585oeure0vcTGV6l2rWihzVuvVJBjHqNHRkS67XhSc/pMrWswun61mx5gMN7edQMGCy5u071cLAxWcFWX0NbKZgViBwUwz1DF8eBrXaGLm7jKNrp6hKbEc0vfdNbFdY4b7GIxv4d2CrawulmNB1emoFQe7pzIGzqQ2CEetR33BTEsx0EOKpLsmtEhzh8tL7G/kgOedtq5attDSB3uYgP1xdx3+tAi4hdB6Vfj1rdGOy/51STxbTZNbhSOuzU2b";
    private GLView glView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (!Engine.initialize(this, key)) {
            Log.e("HelloAR", "Initialization Failed.");
            Toast.makeText(ARActivity.this, Engine.errorMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        if (!CameraDevice.isAvailable()) {
            Toast.makeText(ARActivity.this, "CameraDevice not available.", Toast.LENGTH_LONG).show();
            return;
        }
        if (!ImageTracker.isAvailable()) {
            Toast.makeText(ARActivity.this, "ImageTracker not available.", Toast.LENGTH_LONG).show();
            return;
        }

        glView = new GLView(this);

        requestCameraPermission(new PermissionCallback() {
            @Override
            public void onSuccess() {
                ((ViewGroup) findViewById(R.id.preview)).addView(glView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }

            @Override
            public void onFailure() {
            }
        });
    }

    private interface PermissionCallback
    {
        void onSuccess();
        void onFailure();
    }
    private HashMap<Integer, PermissionCallback> permissionCallbacks = new HashMap<Integer, PermissionCallback>();
    private int permissionRequestCodeSerial = 0;
    @TargetApi(23)
    private void requestCameraPermission(PermissionCallback callback)
    {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                int requestCode = permissionRequestCodeSerial;
                permissionRequestCodeSerial += 1;
                permissionCallbacks.put(requestCode, callback);
                requestPermissions(new String[]{Manifest.permission.CAMERA}, requestCode);
            } else {
                callback.onSuccess();
            }
        } else {
            callback.onSuccess();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        if (permissionCallbacks.containsKey(requestCode)) {
            PermissionCallback callback = permissionCallbacks.get(requestCode);
            permissionCallbacks.remove(requestCode);
            boolean executed = false;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    executed = true;
                    callback.onFailure();
                }
            }
            if (!executed) {
                callback.onSuccess();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (glView != null) { glView.onResume(); }
    }

    @Override
    protected void onPause()
    {
        if (glView != null) { glView.onPause(); }
        super.onPause();
    }
}
