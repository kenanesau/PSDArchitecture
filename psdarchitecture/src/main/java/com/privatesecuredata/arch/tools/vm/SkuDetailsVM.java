package com.privatesecuredata.arch.tools.vm;

import com.privatesecuredata.arch.billing.SkuDetails;
import com.privatesecuredata.arch.mvvm.vm.ComplexViewModel;
import com.privatesecuredata.arch.mvvm.vm.SimpleValueVM;

/**
 * Created by kenan on 11/20/15.
 */
public class SkuDetailsVM extends ComplexViewModel {

    private SimpleValueVM<String> _sku;
    private SimpleValueVM<String> _desc;
    private SimpleValueVM<String> _price;
    private SimpleValueVM<String> _title;
    private SimpleValueVM<String> _type;
    private SimpleValueVM<Boolean> _isAvailable;

    public SkuDetailsVM(SkuDetails details, boolean isAvailable) {
        _sku = new SimpleValueVM<String>(details.getSku());
        registerChildVM(_sku);

        _desc = new SimpleValueVM<String>(details.getDescription());
        registerChildVM(_desc);

        _price = new SimpleValueVM<String>(details.getPrice());
        registerChildVM(_price);

        _title = new SimpleValueVM<String>(details.getTitle());
        registerChildVM(_title);

        _type = new SimpleValueVM<String>(details.getType());
        registerChildVM(_type);

        _isAvailable = new SimpleValueVM<Boolean>(isAvailable);
        registerChildVM(_isAvailable);
    }

    public SimpleValueVM<String> getSku() {
        return _sku;
    }

    public SimpleValueVM<String> getDesc() {
        return _desc;
    }

    public SimpleValueVM<String> getPrice() {
        return _price;
    }

    public SimpleValueVM<String> getTitle() {
        return _title;
    }

    public SimpleValueVM<String> getType() {
        return _type;
    }

    public SimpleValueVM<Boolean> isAvailable() {
        return _isAvailable;
    }
}
