package com.privatesecuredata.arch.ui.widget;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.privatesecuredata.arch.R;
import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.mvvm.android.MVVMFragment;
import com.privatesecuredata.arch.mvvm.android.MVVMRecyclerViewModelAdapter;
import com.privatesecuredata.arch.mvvm.vm.ComplexViewModel;
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

        if (null != savedInstanceState)
        {
            String typeName = savedInstanceState.getString(getInstanceStateTag());
            if (null != typeName) {
                ComplexViewModel<?> parentVM = getViewModel(typeName);
                if (parentVM != null)
                    updateParent(parentVM);
            }
        }
	}

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(getLayoutId(), container, false);

        lstView = (RecyclerView) view.findViewById(getRecyclerViewId());
        if (null == lstView)
            throw new ArgumentException("Could not find a Recycler-View!! Maybe you didn't set the correct id by overwriting getRecyclerViewId()?");
        lstView.setAdapter(adapter);

		return view;
	}

    public abstract MVVMRecyclerViewModelAdapter.IViewHolderFactory<T> getVhFactory();
    public int getLayoutId() { return R.layout.psdarch_list; }
    public int getRecyclerViewId() { return R.id.psdarch_recyclerview; }
    public abstract void configureAdapter(MVVMRecyclerViewModelAdapter<T, TVM> adapter, Bundle savedInstanceState);
    public String getInstanceStateTag() {
        return getClass().getCanonicalName();
    }
    public abstract void updateParent(ComplexViewModel<?> parent);

    public MVVMRecyclerViewModelAdapter<T, TVM> getAdapter() { return this.adapter; }

    /**
     * Set items without causing an update
     *
     * @param newItems New Items
     */
    public void setItems(IListViewModel<T, TVM> newItems)
    {
        this.items = newItems;
    }

    /**
     * Set the new items and cause a redraw on the adapter
     *
     * @param newItems
     */
    public void updateItems(IListViewModel<T, TVM> newItems)
    {
        setItems(newItems);
        doViewToVMMapping();
    }

    protected IListViewModel<T, TVM> getItems() { return items; }
    protected RecyclerView getListView() {
        return lstView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        adapter.dispose();
        adapter = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        MVVMRecyclerViewModelAdapter adapter = getAdapter();
        IListViewModel listVM = null;
        if (null != adapter)
            listVM = adapter.getData();

        ComplexViewModel parent = listVM != null ? listVM.getParentViewModel() : null;
        if ( (null != parent) && (null != parent.getModel()) ) {
            rememberInstanceState(parent);
            outState.putString(getInstanceStateTag(), parent.getModel().getClass().getCanonicalName());
        }

        super.onSaveInstanceState(outState);
    }

    /**
     * Put the items to the adapter and cause a redraw
     */
    @Override
    protected void doViewToVMMapping() {
        if (null != adapter)
            adapter.setData(items);
    }
}
