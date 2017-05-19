package com.privatesecuredata.arch.licenseActivity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.privatesecuredata.arch.R;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;

/**
 * Created by kenan on 3/24/17.
 */

public class LicenseContentFragment extends Fragment {
    public static String TAG = "tag_fragment_licence_content";
    private static final String TAG_PRODUCT_DIR = "tag_license_activity_product_dir";

    private Product product;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_license_content, container, false);
        Consumer<StringBuilder> dataConsumer = new Consumer<StringBuilder>() {
            @Override
            public void accept(StringBuilder sb) throws Exception {
                TextView textView = (TextView) view.findViewById(R.id.txt_license_content);
                textView.setText(sb.toString());
            }
        };
        Consumer<Throwable> errorConsumer = new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage("Error reading the license text!");
            }
        };

        if (null == savedInstanceState) {
            LicenseDataReader.readLicense(getActivity(), product)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(dataConsumer, errorConsumer);
        }
        else
        {
            String dir = savedInstanceState.getString(TAG_PRODUCT_DIR);
            LicenseDataReader.readProduct(getActivity(), dir)
                .subscribe( product -> {
                    setProduct(product);
                    LicenseDataReader.readLicense(getActivity(), product)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(dataConsumer, errorConsumer);
                });
        }

        return view;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Product getProduct() {
        return this.product;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(TAG_PRODUCT_DIR, getProduct().getDir());
    }

}
