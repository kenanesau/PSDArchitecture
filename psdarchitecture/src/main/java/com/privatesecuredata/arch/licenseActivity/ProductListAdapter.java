package com.privatesecuredata.arch.licenseActivity;

import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.privatesecuredata.arch.R;

import java.util.List;

/**
 * Created by kenan on 3/24/17.
 */

public class ProductListAdapter extends RecyclerView.Adapter {
    public interface IClickListener {
        void onClick(View v, int pos);
    };

    public interface ILongClickListener {
        boolean onLongClick(View v, int pos);
    };

    private List<Product> data;

    @Nullable
    private IClickListener onClickCb;
    private ILongClickListener onLongClickCb;

    public class ProductViewHolder extends RecyclerView.ViewHolder
    implements View.OnClickListener, View.OnLongClickListener {
        TextView name;
        TextView version;

        public ProductViewHolder(View rowView) {
            super(rowView);

            rowView.setOnClickListener(this);
            rowView.setOnLongClickListener(this);
            name = (TextView)rowView.findViewById(R.id.txt_product_name);
            version = (TextView)rowView.findViewById(R.id.txt_product_version);
        }

        public void bind(Product data) {
            name.setText(data.getName());
            version.setText(String.format("V %s", data.getVersion()));
        }

        @Override
        public void onClick(View v) {
            if (null != ProductListAdapter.this.onClickCb)
                ProductListAdapter.this.onClickCb.onClick(v, getAdapterPosition());
        }

        @Override
        public boolean onLongClick(View v) {
            boolean ret = false;
            if (null != ProductListAdapter.this.onLongClickCb)
                ret = ProductListAdapter.this.onLongClickCb.onLongClick(v, getAdapterPosition());
            return ret;
        }
    }

    public ProductListAdapter() {
    }

    public ProductListAdapter(List<Product> lst) {
        setItems(lst);
    }

    public void setItems(List<Product> lst) {
        data = lst;
        notifyDataSetChanged();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.product_list_item, parent, false);
        ProductViewHolder vh = new ProductViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Product product = data.get(position);
        ((ProductViewHolder)holder).bind(product);
    }

    Product get(int pos) {
        return data.get(pos);
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    public void setOnClickCb(IClickListener onClickCb) {
        this.onClickCb = onClickCb;
    }

    public void setOnLongClickCb(ILongClickListener onLongClickCb) {
        this.onLongClickCb = onLongClickCb;
    }
}
