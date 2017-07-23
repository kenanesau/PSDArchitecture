package com.privatesecuredata.arch.shop;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.privatesecuredata.arch.billing.SkuDetails;
import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.mvvm.DataHive;
import com.privatesecuredata.arch.mvvm.IGetVMCommand;
import com.privatesecuredata.arch.mvvm.MVVM;
import com.privatesecuredata.arch.mvvm.android.MVVMComplexVmAdapterTemplate;
import com.privatesecuredata.arch.mvvm.android.MVVMFragment;
import com.privatesecuredata.arch.mvvm.android.ViewModelListAdapter;
import com.privatesecuredata.arch.mvvm.vm.IListViewModel;
import com.privatesecuredata.arch.mvvm.vm.IModelChangedListener;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;
import com.privatesecuredata.arch.mvvm.vm.SimpleValueVM;
import com.privatesecuredata.arch.tools.vm.PlayStoreVM;
import com.privatesecuredata.arch.tools.vm.SkuDetailsVM;
import com.privatesecuredata.arch.R;

import io.reactivex.Observable;

public class SkuListFragment extends MVVMFragment
			implements OnItemClickListener, OnItemLongClickListener, OnCreateContextMenuListener, IModelChangedListener {
    public static final String TAG_SHOP = "tag_shop_uuid";
    private Context attachedActivity;
    private ViewModelListAdapter adapter;
    private AbsListView lstView;
    private PlayStoreVM playStoreVM;

    @Override
    public void notifyModelChanged(IViewModel<?> vm, IViewModel<?> originator) {
        if ( (adapter != null) && (playStoreVM != null) )
            adapter.setData(playStoreVM.getSkuList());
    }

    public class SkuListRowAdapterTemplate extends MVVMComplexVmAdapterTemplate<SkuDetailsVM>
    {
        public SkuListRowAdapterTemplate() {
            super(true); //make ReadOnly

            setMapping(String.class, R.id.txt_sku_details_title, new IGetVMCommand<String>() {
                @Override
                public SimpleValueVM<String> getVM(IViewModel<?> vm) {
                    return ((SkuDetailsVM) vm).getTitle();
                }
            });

            setMapping(String.class, R.id.txt_sku_details_price, new IGetVMCommand<String>() {
                @Override
                public SimpleValueVM<String> getVM(IViewModel<?> vm) {
                    return ((SkuDetailsVM) vm).getPrice();
                }
            });

            setViewVisibilityMapping(R.id.pic_sku_details_available, new IGetVMCommand<Boolean>() {

                @Override
                public SimpleValueVM<Boolean> getVM(IViewModel<?> vm) {
                    return ((SkuDetailsVM) vm).isAvailable();
                }
            });

            setViewVisibilityMapping(R.id.pic_sku_details_buy, new IGetVMCommand<Boolean>() {

                @Override
                public SimpleValueVM<Boolean> getVM(IViewModel<?> vm) {
                    return ((SkuDetailsVM) vm).isBuyable();
                }
            });

            setMapping(String.class, R.id.txt_sku_details_description, new IGetVMCommand<String>() {
                @Override
                public SimpleValueVM<String> getVM(IViewModel<?> vm) {
                    return ((SkuDetailsVM) vm).getDesc();
                }
            });

        }
    }

    public interface OnSkuClickedListener {
		void onSkuClicked(SkuDetailsVM container);
	}

    @Override
	public void onAttach(Context activity) {
		if (activity instanceof SkuListFragment.OnSkuClickedListener)
			this.attachedActivity = activity;
		else
			throw new ArgumentException("Activity has to implement ContainerListFragmen.OnContainerClickedListener");

		super.onAttach(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Bundle bundle = getActivity().getIntent().getExtras();
        if (playStoreVM == null) {
            String uuid = (String) bundle.get(TAG_SHOP);
            if (null != uuid) {
                playStoreVM = DataHive.getInstance().get(uuid);
            }
        }

        MVVMComplexVmAdapterTemplate mappingTemplate = new SkuListRowAdapterTemplate();
        playStoreVM.addModelListener(this);
        adapter = new ViewModelListAdapter(mappingTemplate, playStoreVM.getSkuList(), getMVVMActivity());
        adapter.setRowViewId(R.layout.psdarch_sku_details_long);
	}

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.psdarch_fragment_sku_list, container, false);
		lstView = (AbsListView)view.findViewById(R.id.sku_list);

		lstView.setOnItemClickListener(this);
		lstView.setAdapter(adapter);

		return view;
	}

    @Override
    protected void doViewToVMMapping() {
        super.doViewToVMMapping();
        adapter.notifyDataSetChanged();
    }

    @Override
	public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
		SkuDetailsVM skuVM = (SkuDetailsVM)adapter.getItem(pos);
		if (null != attachedActivity)
			((OnSkuClickedListener)attachedActivity).onSkuClicked(skuVM);
	}
	
	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {

		return false;
	}

    public PlayStoreVM getPlayStore() { return playStoreVM; }

    @Override
    public void onDestroy() {
        super.onDestroy();

        playStoreVM.delModelListener(this);
    }
}