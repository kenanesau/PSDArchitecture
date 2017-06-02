package com.privatesecuredata.arch.licenseActivity;

import android.app.Activity;

import com.privatesecuredata.arch.exceptions.ArgumentException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by kenan on 4/7/17.
 */
public class LicenseDataReader {

    /**
     * Get the Licence-Content from the file on "disk"
     * @param ctx current activity
     * @param product the product-object
     * @return Stringbuilder containing license-content
     */
    public static Flowable<StringBuilder> readLicense(Activity ctx, Product product) {
        return Flowable.just(product)
                .subscribeOn(Schedulers.io())
                .map(new Function<Product, StringBuilder>() {
                    @Override
                    public StringBuilder apply(Product prod) throws Exception {
                        InputStream licenseStream = ctx.getAssets().open(prod.getLicense().getFile());
                        BufferedReader reader = new BufferedReader(new InputStreamReader(licenseStream));

                        StringBuilder sb = new StringBuilder();
                        while (reader.ready()) {
                            String line = reader.readLine();
                            sb.append(line).append("\n");
                        }
                        reader.close();
                        return sb;
                    }
                });
    }

    /**
     * Read a single product-object from the corresponding files
     * @param ctx the current activity
     * @param dir the sub-directory-name for the product
     * @return Product-object
     */
    public static Flowable<Product> readProduct(Activity ctx, String dir) {
        return Flowable.just(dir)
                .subscribeOn(Schedulers.io())
                .map(new Function<String, Product>() {
                    @Override
                    public Product apply(String prodName) throws Exception {
                        Product product = null;
                        if (null != prodName) {
                            try {
                                product = Product.createProduct(ctx, prodName);
                            } catch (IOException e) {
                                throw new ArgumentException("Unable to restore state", e);
                            }
                        }

                        return product;
                    }
                });
    }
}
