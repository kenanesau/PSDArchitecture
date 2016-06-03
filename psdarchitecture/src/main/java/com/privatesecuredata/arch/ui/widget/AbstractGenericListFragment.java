package com.privatesecuredata.arch.ui.widget;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.privatesecuredata.arch.R;
import com.privatesecuredata.arch.mvvm.android.MVVMFragment;
import com.privatesecuredata.arch.mvvm.android.MVVMRecyclerViewModelAdapter;
import com.privatesecuredata.arch.mvvm.vm.EncapsulatedListViewModel;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;

public abstract class AbstractGenericListFragment<T, TVM extends IViewModel<T>> extends MVVMFragment {

    public interface OnItemClickedListener<T> {
        public void onStockItemClicked(T item);
    }

    private Context attachedActivity;
	private EncapsulatedListViewModel<T, TVM> items;
	private MVVMRecyclerViewModelAdapter<T, TVM> adapter;
    private RecyclerView lstView;

	public AbstractGenericListFragment()
	{
    }

    @Override
    public void onAttach(Context activity) {
        this.attachedActivity = activity;

        super.onAttach(activity);
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		adapter = configureAdapter(new MVVMRecyclerViewModelAdapter(attachedActivity, getVhFactory()));
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(getLayoutId(), container, false);
        lstView = (RecyclerView) view.findViewById(getRecyclerViewId());
        lstView.setAdapter(adapter);

		return view;
	}

    public abstract MVVMRecyclerViewModelAdapter.IViewHolderFactory<T> getVhFactory();
    public int getLayoutId() { return R.layout.psdarch_list; }
    public int getRecyclerViewId() { return R.id.psdarch_recyclerview; }
    public abstract MVVMRecyclerViewModelAdapter<T, TVM> configureAdapter(MVVMRecyclerViewModelAdapter<T, TVM> adapter);

    public MVVMRecyclerViewModelAdapter<T, TVM> getAdapter() { return this.adapter; }
    public void updateItems(EncapsulatedListViewModel<T, TVM> newItems)
    {
        items = newItems;

        if (null != adapter) {
            adapter.setData(items);
        }
    }

    protected RecyclerView getListView() {
        return lstView;
    }
}
