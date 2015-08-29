package com.privatesecuredata.arch.tools.vm;

import com.privatesecuredata.arch.db.DbId;
import com.privatesecuredata.arch.db.IPersistable;
import com.privatesecuredata.arch.db.PersistanceManager;
import com.privatesecuredata.arch.mvvm.IWriteModelCommand;
import com.privatesecuredata.arch.mvvm.vm.ComplexViewModel;
import com.privatesecuredata.arch.mvvm.vm.SimpleValueVM;

import java.util.ArrayList;

/**
 * VM which can be used to move items (container-content) from one
 * container to another
 *
 * The containers have to have a ViewModel which is derived from
 * ComplexViewModel
 */
public class ItemMoverVM<V extends IPersistable> extends ComplexViewModel {
    DbId srcContainerId;
    ComplexViewModel srcContainerVM;
    private ArrayList<DbId> items = new ArrayList<DbId>();
    private SimpleValueVM<Integer> itemCount = new SimpleValueVM<Integer>(Integer.class, 0);
    private PersistanceManager pm;

    public ItemMoverVM(PersistanceManager pm, ComplexViewModel<V> srcContainer)
    {
        super(pm.createMVVM());
        registerChildVM(itemCount);
        this.pm = pm;
        srcContainerId = srcContainer.getModel().getDbId();
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

    /**
     * Move everything to the destination container.
     *
     * @param dstVM
     * @param addItemCmd
     * @param <T>
     * @param <ITEM>
     */
    public <T extends ComplexViewModel, ITEM> void move(T dstVM, IWriteModelCommand<ITEM> deleteItemCmd, IWriteModelCommand<ITEM> addItemCmd) {
        if (null == srcContainerVM) {
            srcContainerVM = getMVVM().createVM(pm.load(srcContainerId));
            registerChildVM(srcContainerVM);
        }
        registerChildVM(dstVM);
        for (int i=0; i<items.size(); i++) {
            ITEM item = pm.load(items.get(i));
            deleteItemCmd.execute(srcContainerVM, item);
            addItemCmd.execute(dstVM, item);
        }
        commit();
    }
}
