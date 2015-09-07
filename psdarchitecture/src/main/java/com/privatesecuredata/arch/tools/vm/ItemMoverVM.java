package com.privatesecuredata.arch.tools.vm;

import com.privatesecuredata.arch.R;
import com.privatesecuredata.arch.db.DbId;
import com.privatesecuredata.arch.db.IPersistable;
import com.privatesecuredata.arch.db.PersistanceManager;
import com.privatesecuredata.arch.mvvm.IGetListVMCommand;
import com.privatesecuredata.arch.mvvm.IGetVMCommand;
import com.privatesecuredata.arch.mvvm.IWriteModelCommand;
import com.privatesecuredata.arch.mvvm.vm.ComplexViewModel;
import com.privatesecuredata.arch.mvvm.vm.EncapsulatedListViewModel;
import com.privatesecuredata.arch.mvvm.vm.IListViewModel;
import com.privatesecuredata.arch.mvvm.vm.ListViewModel;
import com.privatesecuredata.arch.mvvm.vm.SimpleValueVM;
import com.privatesecuredata.arch.mvvm.vm.StringFormatVM;

import java.util.ArrayList;

/**
 * VM which can be used to move items (container-content) from one
 * container to another
 *
 * The containers have to have a ViewModel which is derived from
 * ComplexViewModel
 */
public class ItemMoverVM<V extends IPersistable> extends ComplexViewModel<V> {
    public static final String TAG_PSDARCH_ITEMMOVER = "psdarch_itemmover";
    private DbId srcContainerId;
    private ComplexViewModel srcContainerVM;
    private ArrayList<DbId> items = new ArrayList<DbId>();
    private SimpleValueVM<Integer> itemCount = new SimpleValueVM<Integer>(Integer.class, 0);
    private SimpleValueVM<Boolean> actionValid = new SimpleValueVM<Boolean>(false);
    private StringFormatVM itemsCountTxt;
    private PersistanceManager pm;
    private IGetListVMCommand getListCommand;

    public ItemMoverVM(PersistanceManager pm, ComplexViewModel<V> srcContainer, IGetListVMCommand getListCommand)
    {
        super(pm.createMVVM());
        registerChildVM(itemCount);
        registerChildVM(actionValid);
        this.setModel(srcContainer.getModel());
        itemsCountTxt = new StringFormatVM("%d %s", itemCount,
                new SimpleValueVM(getResources().getString(R.string.psdarch_fragment_action_move_number_of_items)));
        registerChildVM(itemsCountTxt);
        this.pm = pm;
        srcContainerId = srcContainer.getModel().getDbId();
        srcContainerVM = srcContainer;
        this.getListCommand = getListCommand;
    }

    public void addItem(DbId dbId)
    {
        items.add(dbId);
        itemCount.set(items.size());
    }

    public void removeItem(DbId dbId)
    {
        items.remove(dbId);
        itemCount.set(items.size());
    }

    public ComplexViewModel getSrcContainerVM() { return srcContainerVM; }

    public SimpleValueVM<String> getNumberOfItemsText() { return itemsCountTxt; }
    public SimpleValueVM<Boolean> getActionValid() { return actionValid; }


    /**
     * Move everything to destination container
     *
     * @param dstVM destination container
     * @param <T>
     * @param <ITEM>
     */
    public <T extends ComplexViewModel, ITEM> void move(T dstVM) {
        DbId dstId = ((IPersistable)dstVM.getModel()).getDbId();
        IListViewModel listVM = getListCommand.getVM(srcContainerVM);
        IListViewModel dstListVM = getListCommand.getVM(dstVM);
        pm.move(srcContainerId, ((IPersistable)dstVM.getModel()).getDbId(),
                ((ComplexViewModel)listVM).getModelField(),
                listVM.size(),
                dstListVM.size(),
                items);
        Object srcModel = pm.load(srcContainerId);
        srcContainerVM.updateModel(srcModel);

        Object dstModel = pm.load(dstId);
        dstVM.updateModel(dstModel);
    }

    @Override
    public void commit() {
        srcContainerVM.commit();
        super.commit();
    }

    /**
     * Checks if dstVM is a valid target to move the items to
     * @param dstVM
     * @return true if dstVM is a valid target, false if not
     */
    public boolean checkMove(ComplexViewModel dstVM) {
        boolean valid = (srcContainerVM.getClass().isAssignableFrom(dstVM.getClass()));
        actionValid.set(valid);
        return valid;
    }
}
