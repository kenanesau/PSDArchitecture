package com.privatesecuredata.arch.tools.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.privatesecuredata.arch.R;
import com.privatesecuredata.arch.mvvm.DataHive;
import com.privatesecuredata.arch.mvvm.IGetVMCommand;
import com.privatesecuredata.arch.mvvm.android.MVVMComplexVmAdapter;
import com.privatesecuredata.arch.mvvm.android.MVVMFragment;
import com.privatesecuredata.arch.mvvm.vm.ComplexViewModel;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;
import com.privatesecuredata.arch.mvvm.vm.SimpleValueVM;
import com.privatesecuredata.arch.tools.vm.ItemMoverVM;

/**
 * Created by kenan on 9/1/15.
 *
 * Fragment for the ItemMover. This is always displayed as long a move-operation
 * is imminent.
 */
public class ItemMoverFragment extends MVVMFragment {
    public static final String TAG = "psdarch_mover_fragment";

    private MVVMComplexVmAdapter<ItemMoverVM> mvvmAdapter;
    private ItemMoverVM vm;
    private View view;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        view = inflater.inflate(R.layout.psdarch_action_move, container, false);
        Bundle bundle = getActivity().getIntent().getExtras();

        if (savedInstanceState == null) {
            String uuid = (String) bundle.get(ItemMoverVM.TAG_PSDARCH_ITEMMOVER);
            if (null != uuid) {
                vm = DataHive.getInstance().remove(uuid);
            }

        } else {
            if (null != savedInstanceState) {
                /** Cancel action ... **/
                View btn = view.findViewById(R.id.psdarch_btn_action_move_cancel);
                if (btn != null)
                    btn.performClick();
            }
        }

        return view;
    }

    @Override
    protected void doViewToVMMapping() {
        if ( (null != this.view) && (null != this.vm)) {
            mvvmAdapter = new MVVMComplexVmAdapter<ItemMoverVM>(getMVVMActivity(), this.view, this.vm);
            mvvmAdapter.setMapping(String.class, R.id.psdarch_txt_action_move_number_of_items,
                    new IGetVMCommand<String>() {
                        @Override
                        public SimpleValueVM<String> getVM(IViewModel<?> vm) {
                            return ((ItemMoverVM) vm).getNumberOfItemsText();
                        }
                    });
            mvvmAdapter.setViewDisableMapping(R.id.psdarch_btn_action_move_ok,
                    new IGetVMCommand<Boolean>() {
                        @Override
                        public SimpleValueVM<Boolean> getVM(IViewModel<?> vm) {
                            return ((ItemMoverVM) vm).getActionValid();
                        }
                    });
        }
    }

    public void setItemMoverVM(ItemMoverVM vm) {
        this.vm = vm;

        doViewToVMMapping();
    }

    public ItemMoverVM getItemMover() {
        return this.vm;
    }

    public boolean checkMove()
    {
        if (null != this.vm)
            return this.vm.checkMove();
        else
            return false;
    }

    public boolean checkMove(ComplexViewModel dstVM)
    {
        if (null != this.vm)
            return this.vm.checkMove(dstVM);
        else
            return false;
    }

    public <T> void move()
    {
        this.getItemMover().move();
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .remove(this)
                .disallowAddToBackStack()
                .commit();
        this.getItemMover().clear();
        this.setItemMoverVM(null);
    }

    public <T> void move(ComplexViewModel dstVM)
    {
        this.getItemMover().move(dstVM);
        getActivity().getSupportFragmentManager()
                .beginTransaction()
                .remove(this)
                .disallowAddToBackStack()
                .commit();
        this.getItemMover().clear();
        this.setItemMoverVM(null);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (null != this.getItemMover())
            this.getItemMover().clear();
    }
}
