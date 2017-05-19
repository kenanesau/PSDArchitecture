package com.privatesecuredata.arch.licenseActivity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Toast;

import com.privatesecuredata.arch.R;

/**
 * Created by kenan on 3/24/17.
 */

public class LicenseActivity extends FragmentActivity {
    public static final String ARG_PRODUCT_NAME = "psdarch_license_activity_arg_product_name";
    public static final String ARG_PRODUCT_VERSION = "psdarch_license_activity_arg_product_version";

    private String productName;
    private String productVersion;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license);

        Bundle bundle = getIntent().getExtras();

        if (null != bundle) {
            productName = bundle.getString(ARG_PRODUCT_NAME, "P1");
            productVersion = bundle.getString(ARG_PRODUCT_VERSION, "V0.0.0");
        }

        if (savedInstanceState == null) {
            ProductListFragment productListFragment =
                    ProductListFragment.newInstance(productName, productVersion);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.license_main_content, productListFragment, ProductListFragment.TAG)
                    .commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        String msg = getResources().getString(R.string.toast_info_activity);

        Toast.makeText(this,
                String.format(msg, productName, productVersion),
                Toast.LENGTH_LONG).show();
    }

    public void onButtonClick(View view) {
        int id = view.getId();

        if (id == R.id.psdarch_btn_cancel) {
            finish();
        }
    }
}