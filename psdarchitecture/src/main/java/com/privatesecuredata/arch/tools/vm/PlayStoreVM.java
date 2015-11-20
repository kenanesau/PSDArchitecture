package com.privatesecuredata.arch.tools.vm;

import android.util.Log;

import com.privatesecuredata.arch.billing.IabHelper;
import com.privatesecuredata.arch.billing.IabResult;
import com.privatesecuredata.arch.billing.Inventory;
import com.privatesecuredata.arch.billing.SkuDetails;
import com.privatesecuredata.arch.mvvm.vm.ComplexViewModel;
import com.privatesecuredata.arch.mvvm.vm.SimpleValueVM;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;

/**
 * Created by kenan on 11/20/15.
 */
public class PlayStoreVM extends ComplexViewModel {

    private IabHelper _billingHelper;
    private String[] _appSkus;
    private SimpleValueVM<Boolean> _connected = new SimpleValueVM<Boolean>(false);
    private SimpleValueVM<Boolean> _error = new SimpleValueVM<Boolean>(false);
    private SimpleValueVM<String> _errorMsg = new SimpleValueVM<String>("");
    private Dictionary<String, SkuDetailsVM> _lstSkuDetails = new Hashtable<>();

    IabHelper.QueryInventoryFinishedListener _gotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result,
                                             Inventory inventory) {

            if (result.isFailure()) {
                // handle error here
            }
            else {
                for (String sku : _appSkus) {
                    SkuDetails details = inventory.getSkuDetails(sku);
                    boolean hasPurchase = inventory.hasPurchase(sku);
                    SkuDetailsVM vm = new SkuDetailsVM(details, hasPurchase);
                    _lstSkuDetails.put(sku, vm);
                    registerChildVM(vm);
                }
            }
        }
    };

    public PlayStoreVM(IabHelper iabHelper, String[] skus){
        _billingHelper = iabHelper;
        _appSkus = skus;
        initAsync();
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


}
