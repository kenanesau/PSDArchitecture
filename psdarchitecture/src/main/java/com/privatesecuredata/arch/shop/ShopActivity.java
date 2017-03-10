package com.privatesecuredata.arch.shop;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.privatesecuredata.arch.mvvm.android.MVVMActivity;
import com.privatesecuredata.arch.tools.vm.PlayStoreVM;
import com.privatesecuredata.arch.tools.vm.SkuDetailsVM;
import com.privatesecuredata.arch.R;

/**
 * Created by kenan on 11/21/15.
 */
public class ShopActivity extends MVVMActivity implements SkuListFragment.OnSkuClickedListener {
    boolean canceled = false;
    public static final int REQUESTCODE_SHOP = 0xF123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.psdarch_activity_shop);
    }

    @Override
    public void finish() {
        SkuListFragment frag = (SkuListFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_shop);

        if (canceled) {
            setResult(RESULT_CANCELED);
        }
        else {
            Intent intent = new Intent();
            setResult(RESULT_OK, intent);
        }

        super.finish();
    }

    public void onButtonClick(View view) {
        int id = view.getId();

        if (R.id.psdarch_btn_cancel == id) {
            canceled = true;
            finish();
        }
        if (R.id.psdarch_btn_ok == id) {
            finish();
        }
    }

    @Override
    public void onSkuClicked(SkuDetailsVM sku) {
        SkuListFragment frag = (SkuListFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_shop);
        PlayStoreVM store = frag.getPlayStore();

        if (store.isSkuLicensed(sku.getSku().get()))
        {
            new AlertDialog.Builder(ShopActivity.this).setTitle(getResources().getString(R.string.psdarch_already_bought_title))
                    .setMessage(getResources().getString(R.string.psdarch_already_bought_message))
                    .setPositiveButton(getResources().getString(R.string.psdarch_ok), null)
                    .show();
        }
        else
            store.buy(this, sku, REQUESTCODE_SHOP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Bundle bundle = data.getExtras();

            switch (requestCode) {
                case REQUESTCODE_SHOP:
                    SkuListFragment frag = (SkuListFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_shop);
                    PlayStoreVM store = frag.getPlayStore();
                    store.checkShoppingResult(requestCode, resultCode, data);
                    break;
                default:
                    break;
            }
        }
    }
}
