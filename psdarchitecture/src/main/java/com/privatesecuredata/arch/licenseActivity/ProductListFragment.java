package com.privatesecuredata.arch.licenseActivity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.privatesecuredata.arch.ui.widget.SimpleDivider;
import com.privatesecuredata.arch.R;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by kenan on 3/24/17.
 */

public class ProductListFragment extends Fragment {
    public static final String TAG = "tag_psdarch_product_list_fragment";
    private static final String ARG_PRODUCT_NAME = "psdarch_arg_product_name";
    private static final String ARG_PRODUCT_VERSION = "psdarch_arg_product_version";

    private ProductListAdapter adapter = new ProductListAdapter();

    public static ProductListFragment newInstance(String productName, String productVersion) {
        ProductListFragment f = new ProductListFragment();

        Bundle args = new Bundle();
        args.putString(ProductListFragment.ARG_PRODUCT_NAME, productName);
        args.putString(ProductListFragment.ARG_PRODUCT_VERSION, productVersion);
        f.setArguments(args);

        return f;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_product_list, container, false);
        RecyclerView rv = (RecyclerView)view.findViewById(R.id.lst_products);

        Bundle bundle = getArguments();
        String productName = bundle.getString(ARG_PRODUCT_NAME, "ProductName");
        String productVersion = bundle.getString(ARG_PRODUCT_VERSION, "V0.0.0.0");

        rv.setAdapter(adapter);
        rv.addItemDecoration(
                new SimpleDivider(getActivity(),
                        R.drawable.psdarch_simple_divider,
                        R.dimen.raster4));

        Flowable.just("licenses")
            .subscribeOn(Schedulers.io())
            .map( val -> getActivity().getAssets().list(val) )
            .map( prodList -> {
                List lst = new ArrayList<Product>(prodList.length);

                for(String prod : prodList) {
                    Product product = Product.createProduct(getActivity(), prod);
                    lst.add(product);
                }

                return lst;
            })
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                    productList -> {
                        adapter.setItems(productList);
                        adapter.setOnClickCb( (v, pos) -> {
                            Product prod = adapter.get(pos);

                            FragmentManager fragMan = getActivity().getSupportFragmentManager();
                            Fragment frag = fragMan.findFragmentByTag(LicenseContentFragment.TAG);

                            if (frag == null) {
                                Fragment productListFrag = fragMan.findFragmentById(R.id.license_main_content);
                                LicenseContentFragment licenceContentFrag = new LicenseContentFragment();
                                licenceContentFrag.setProduct(prod);
                                fragMan.beginTransaction()
                                        .remove(productListFrag)
                                        .add(R.id.license_main_content, licenceContentFrag, LicenseContentFragment.TAG)
                                        .addToBackStack(null)
                                        .commit();
                            }
                        } );
                    },
                    ex -> {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                        builder.setMessage("Error reading the product list");
                    });

        return view;
    }
}
