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
import com.privatesecuredata.arch.mvvm.vm.IListViewModel;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;

public abstract class AbstractGenericListFragment<T, TVM extends IViewModel<T>> extends MVVMFragment {

	private IListViewModel<T, TVM> items;
	private MVVMRecyclerViewModelAdapter<T, TVM> adapter;
    private RecyclerView lstView;

    @Override
    public void onAttach(Context activity) {
        super.onAttach(activity);
    }

    /**
     * Override this function if you want to inject your own adapter;
     *
     * @return Implementation of MVVMRecyclerViewModelAdapter<T, TVM>
     */
    protected MVVMRecyclerViewModelAdapter<T, TVM> createAdapter() {
        return new MVVMRecyclerViewModelAdapter(getActivity(), getVhFactory());
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        adapter =  createAdapter();
		configureAdapter(adapter, savedInstanceState);

        if (null != items)
            adapter.setData(items);

        /**
        if (null != savedInstanceState)
        {
            String typeName = savedInstanceState.getString(TAG_TYPE_NAME);
            if (null != typeName) {
                ComplexViewModel<?> parentVM = getViewModel(typeName);
                if (parentVM != null)
                    updateParent(parentVM);
            }
        }*/
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
    public abstract void configureAdapter(MVVMRecyclerViewModelAdapter<T, TVM> adapter, Bundle savedInstanceState);

    public MVVMRecyclerViewModelAdapter<T, TVM> getAdapter() { return this.adapter; }
    public void updateItems(IListViewModel<T, TVM> newItems)
    {
        items = newItems;

        if (null != adapter) {
            adapter.setData(items);
        }
    }

    protected IListViewModel<T, TVM> getItems() { return items; }
    protected RecyclerView getListView() {
        return lstView;
    }

    @Override
    public void onStop() {
        super.onDestroyView();

        adapter.dispose();
    }

    @Override
    protected void doViewToVMMapping() {
        adapter.setData(items);
    }
}
