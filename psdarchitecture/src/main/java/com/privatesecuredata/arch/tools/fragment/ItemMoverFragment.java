package com.privatesecuredata.arch.tools.fragment;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
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
    public static final String STATE_ITEM_MOVER_FRAG = "psdarch_item_mover_fragstate";
    public static final String TAG_PSDARCH_ITEMMOVER_VM_UUID = "tag_psdarch_itemmover_vm_uuikd";

    private MVVMComplexVmAdapter<ItemMoverVM> mvvmAdapter;
    private ItemMoverVM vm;
    private View view;

    public static ItemMoverFragment newInstance(ItemMoverVM vm) {
        ItemMoverFragment f = new ItemMoverFragment();

        String uuid = DataHive.getInstance().put(vm);
        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putString(TAG_PSDARCH_ITEMMOVER_VM_UUID, uuid);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        view = inflater.inflate(R.layout.psdarch_action_move, container, false);
        Bundle bundle = getArguments();

        if (savedInstanceState == null) {

            String uuid = null;
            if (bundle != null)
                uuid = (String) bundle.get(TAG_PSDARCH_ITEMMOVER_VM_UUID);

            if (null != uuid) {
                vm = DataHive.getInstance().remove(uuid);
            }

        }
        else {
            String uuid = savedInstanceState.getString(STATE_ITEM_MOVER_FRAG);
            if (null != uuid)
                vm = DataHive.getInstance().remove(uuid);
        }

        if ( (null == vm) || (vm.getItems().size() == 0) ) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .remove(this)
                    .disallowAddToBackStack()
                    .commit();
        }

        return view;
    }

    @Override
    protected void doViewToVMMapping() {
        if ( (null != this.view) && (null != this.vm)) {
            if (mvvmAdapter!=null)
                mvvmAdapter.dispose();

            if (vm.getItems().size() == 0) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .remove(this)
                        .disallowAddToBackStack()
                        .commit();
            }
            else {
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

    private static void addItemMoverFragment(FragmentManager fragMan, int id, ItemMoverVM vm) {
        ItemMoverFragment frag = (ItemMoverFragment) fragMan.findFragmentById(id);
        if (null == frag) {
            frag = ItemMoverFragment.newInstance(vm);
            fragMan.beginTransaction()
                    .add(id, frag, ItemMoverFragment.TAG)
                    .disallowAddToBackStack()
                    .commit();
        }
    }

    public static void addItemMoverFragment(AppCompatActivity ctx, int id, ItemMoverVM vm) {
        FragmentManager fragMan = ctx.getSupportFragmentManager();
        addItemMoverFragment(fragMan, id, vm);
    }

    public static void addItemMoverFragment(FragmentActivity ctx, int id, ItemMoverVM vm) {
        FragmentManager fragMan = ctx.getSupportFragmentManager();
        addItemMoverFragment(fragMan, id, vm);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        String vmUUID = DataHive.getInstance().put(vm);
        outState.putString(STATE_ITEM_MOVER_FRAG, vmUUID);
        super.onSaveInstanceState(outState);
    }
}
