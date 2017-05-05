package com.privatesecuredata.arch.tools.vm;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.privatesecuredata.arch.billing.IabHelper;
import com.privatesecuredata.arch.billing.IabResult;
import com.privatesecuredata.arch.billing.Inventory;
import com.privatesecuredata.arch.billing.Purchase;
import com.privatesecuredata.arch.billing.SkuDetails;
import com.privatesecuredata.arch.mvvm.vm.ComplexViewModel;
import com.privatesecuredata.arch.mvvm.vm.SimpleValueVM;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

/**
 * Created by kenan on 11/20/15.
 */
public class PlayStoreVM extends ComplexViewModel {
    private static String TAG = "PlayStoreVM";
    private IabHelper _billingHelper;
    private String[] _appSkus;
    private SimpleValueVM<Boolean> _connected = new SimpleValueVM<Boolean>(false);
    private SimpleValueVM<Boolean> _error = new SimpleValueVM<Boolean>(false);
    private SimpleValueVM<String> _errorMsg = new SimpleValueVM<String>("");
    private Dictionary<String, SkuDetailsVM> _lstSkuDetails = new Hashtable<>();
    private SkuDetailsVM _skuInPurchase = null;

    IabHelper.QueryInventoryFinishedListener _gotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result,
                                             Inventory inventory) {

            if (result.isFailure()) {
                // handle error here
            }
            else {
                for (String sku : _appSkus) {
                    SkuDetails details = inventory.getSkuDetails(sku);
                    if (null == details)
                        continue;
                    boolean hasPurchase = inventory.hasPurchase(sku);
                    SkuDetailsVM vm = new SkuDetailsVM(details, hasPurchase);
                    _lstSkuDetails.put(sku, vm);
                    registerChildVM(vm);
                }
                notifyModelChanged();
            }
        }
    };

    IabHelper.OnIabPurchaseFinishedListener _purchaseFinishedListener
            = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase)
        {
            if (result.isFailure()) {
                Log.d(TAG, "Error purchasing: " + result);
                return;
            }
            else if (purchase.getSku().equals(_skuInPurchase.getSku())) {
                _skuInPurchase.buy();
            }
        }
    };

    public PlayStoreVM(IabHelper iabHelper, String[] skus){
        _billingHelper = iabHelper;
        _appSkus = skus;
        initAsync();
    }

    public List<SkuDetailsVM> getSkuList() {
        List<SkuDetailsVM> lst = null;
        if (_connected.get() && !_error.get()) {
            lst = new ArrayList<>();
            Enumeration<SkuDetailsVM> details = _lstSkuDetails.elements();
            while (details.hasMoreElements())
            {
                SkuDetailsVM skuDetailsVM = details.nextElement();
                lst.add(skuDetailsVM);
            }
        }

        return lst;
    }

    public void initAsync()
    {
        _billingHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    // shit...
                    Log.d("MyHoard", "Problem setting up In-app Billing: " + result);
                    _connected.set(false);
                    _error.set(true);
                    _errorMsg.set(result.toString());
                }

                _error.set(false);
                _connected.set(true);

                ArrayList<String> skus = new ArrayList<String>(_appSkus.length);

                for(String sku : _appSkus)
                    skus.add(sku);
                _billingHelper.queryInventoryAsync(true, skus, _gotInventoryListener);
            }
        });
    }

    public void dispose()
    {
        if (_billingHelper != null)
            _billingHelper.dispose();
        _billingHelper = null;
    }

    public boolean isSkuLicensed(String sku)
    {
        SkuDetailsVM skuVm = _lstSkuDetails.get(sku);
        if (null != skuVm) {
            return skuVm.isAvailable().get();

        }

        return false;
    }

    public void buy(Activity activity, SkuDetailsVM sku, int requestCode) {
        _skuInPurchase=sku;
        _billingHelper.launchPurchaseFlow(activity, sku.getSku().get(), requestCode,
                _purchaseFinishedListener, "");
    }

    public void checkShoppingResult(int requestCode, int resultCode, Intent intent) {
        if (_billingHelper.handleActivityResult(requestCode, resultCode, intent)) {
            _skuInPurchase.buy();
        }
    }
}
