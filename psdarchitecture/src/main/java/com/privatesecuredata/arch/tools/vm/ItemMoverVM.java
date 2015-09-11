package com.privatesecuredata.arch.tools.vm;

import com.privatesecuredata.arch.R;
import com.privatesecuredata.arch.db.DbId;
import com.privatesecuredata.arch.db.IPersistable;
import com.privatesecuredata.arch.db.PersistanceManager;
import com.privatesecuredata.arch.mvvm.IGetListVMCommand;
import com.privatesecuredata.arch.mvvm.vm.ComplexViewModel;
import com.privatesecuredata.arch.mvvm.vm.IListViewModel;
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

    /**
     * As a user of the ItemMoverVM-class you have to implement this abstract
     * class and overwrite the method checkMove(). Within this method return true
     * if the imminent move-operation is allowed, false otherwise
     */
    public static abstract class CheckMoveCommand {
        private ItemMoverVM _mover;
        public void setMover(ItemMoverVM mover) {_mover = mover;}
        public ItemMoverVM getMover() { return _mover; }

        /**
         * Checks if the move-operation to dst would be legal and returns
         * true if this is the case.
         *
         * @param dst Potential destinatio of move-operation
         * @return true if the move-operation would be legal, false otherwise
         */
        public abstract boolean checkMove(ComplexViewModel dst);
    }

    public static final String TAG_PSDARCH_ITEMMOVER = "psdarch_itemmover";
    private DbId srcContainerId;
    private ComplexViewModel srcContainerVM;
    private ArrayList<DbId> items = new ArrayList<DbId>();
    private SimpleValueVM<Integer> itemCount = new SimpleValueVM<Integer>(Integer.class, 0);
    private SimpleValueVM<Boolean> actionValid = new SimpleValueVM<Boolean>(false);
    private StringFormatVM itemsCountTxt;
    private PersistanceManager pm;
    private IGetListVMCommand getListCommand;

    /**
     * The default-implementation of the CheckMoveCommand just checks if the dst-vm is
     * assignable from the source-vm.
     */
    private CheckMoveCommand checkMoveCommand = new CheckMoveCommand() {
        @Override
        public boolean checkMove(ComplexViewModel dst) {
            if (dst.getModel().getClass().isAssignableFrom(srcContainerVM.getModel().getClass()))
                return true;
            else
                return false;
        }
    };

    /**
     * Create a new ItemMover-Object
     *
     * @param pm The PersistanceManager
     * @param srcContainer The source of the move-operation (items are removed from here)
     * @param getListCommand A command which returns the list of items of the src/dst-Container
     *                       where the items should be removed / added.
     */
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

    /**
     * Set a new command-object which checks the validity of the impending move-operation
     * @param cmd the command-object
     */
    public void setCheckMoveCommand(CheckMoveCommand cmd)
    {
        cmd.setMover(this);
        this.checkMoveCommand = cmd;
    }

    /**
     * Add a new Item for the move-operation
     *
     * @param dbId dbId of the item
     */
    public void addItem(DbId dbId)
    {
        items.add(dbId);
        itemCount.set(items.size());
    }

    /**
     * remove an item from the move-operation
     *
     * @param dbId dbId of the item
     */
    public void removeItem(DbId dbId)
    {
        items.remove(dbId);
        itemCount.set(items.size());
    }

    public SimpleValueVM<String> getNumberOfItemsText() { return itemsCountTxt; }
    public SimpleValueVM<Boolean> getActionValid() { return actionValid; }

    /**
     * Move everything to destination container
     *
     * @param dstVM destination container
     * @param <T> Type parameter (of the container)
     */
    public <T extends ComplexViewModel> boolean move(T dstVM) {
        if (this.checkMoveCommand.checkMove(dstVM)) {
            DbId dstId = ((IPersistable) dstVM.getModel()).getDbId();
            IListViewModel listVM = getListCommand.getVM(srcContainerVM);
            IListViewModel dstListVM = getListCommand.getVM(dstVM);
            pm.move(srcContainerId, ((IPersistable) dstVM.getModel()).getDbId(),
                    ((ComplexViewModel) listVM).getModelField(),
                    listVM.size(),
                    dstListVM.size(),
                    items);
            Object dstModel = pm.load(dstId);
            dstVM.updateModel(dstModel);
            return true;
        }

        return false;
    }

    /**
     * Triggers the ceck of the move-operation an updates the Flag for the validity of the operation
     *
     * @param dstVM potential target for the move operation
     * @return true if the operation is allowed,false otherwise
     */
    public boolean checkMove(ComplexViewModel dstVM) {
        boolean ret = this.checkMoveCommand.checkMove(dstVM);
        this.actionValid.set(ret);
        return ret;
    }

    /**
     * returns a reference to the source-containr
     * @return the source-container
     */
    public ComplexViewModel getSource() {
        return srcContainerVM;
    }
}
