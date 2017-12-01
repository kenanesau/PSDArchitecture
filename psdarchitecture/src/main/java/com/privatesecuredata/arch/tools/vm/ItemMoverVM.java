package com.privatesecuredata.arch.tools.vm;

import android.util.Pair;
import android.util.SparseArray;

import com.privatesecuredata.arch.R;
import com.privatesecuredata.arch.db.DbId;
import com.privatesecuredata.arch.db.IPersistable;
import com.privatesecuredata.arch.db.PersistanceManager;
import com.privatesecuredata.arch.mvvm.IGetListVMCommand;
import com.privatesecuredata.arch.mvvm.vm.ComplexViewModel;
import com.privatesecuredata.arch.mvvm.vm.IListViewModel;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;
import com.privatesecuredata.arch.mvvm.vm.SimpleValueVM;
import com.privatesecuredata.arch.mvvm.vm.StringFormatVM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * VM which can be used to checkAndMove items (container-content) from one
 * container to another
 *
 * The containers have to have a ViewModel which is derived from
 * ComplexViewModel
 */
public class ItemMoverVM<SRC extends IPersistable, DST extends IPersistable> extends ComplexViewModel<SRC> {

    @Override
    protected void doMappings(HashMap<String, IViewModel<?>> childVMs) {}

    /**
     * As a user of the ItemMoverVM-class you have to implement this abstract
     * class and overwrite the method checkMove(). Within this method return true
     * if the imminent checkAndMove-operation is allowed, false otherwise
     */
    public static abstract class CheckMoveCommand {
        private ItemMoverVM _mover;
        public void setMover(ItemMoverVM mover) {_mover = mover;}
        public ItemMoverVM getMover() { return _mover; }

        /**
         * Checks if the checkMove-operation to dst would be legal and returns
         * the true destination if this is the case.
         *
         * @param dst Potential destinatio of checkAndMove-operation
         * @return destination if the checkMove-operation would be legal, null otherwise
         */
        public abstract ComplexViewModel checkMove(ComplexViewModel dst);
        public abstract ComplexViewModel moveCommit(ComplexViewModel dst);
    }

    public static final String TAG_PSDARCH_ITEMMOVER = "psdarch_itemmover";
    private ComplexViewModel srcContainerVM;
    private ComplexViewModel dstConainerVM;
    private SparseArray<DbId> items = new SparseArray<>();
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
        public ComplexViewModel checkMove(ComplexViewModel dst) {
            if ( (srcContainerVM == null) || (dst.getModel().getClass().isAssignableFrom(srcContainerVM.getModel().getClass())) )
                return dst;
            else
                return null;
        }

        @Override
        public ComplexViewModel moveCommit(ComplexViewModel dst) {
            return dst;
        }
    };

    /**
     * Create a new ItemMover-Object
     *
     * @param pm The PersistanceManager
     * @param srcContainer The source of the checkAndMove-operation (items are removed from here)
     * @param getListCommand A command which returns the list of items of the src/dst-Container
     *                       where the items should be removed / added.
     */
    public ItemMoverVM(PersistanceManager pm, ComplexViewModel<SRC> srcContainer, IGetListVMCommand getListCommand)
    {
        super(pm.createMVVM());
        registerChildVM(itemCount);
        registerChildVM(actionValid);
        this.setModel(srcContainer != null ? srcContainer.getModel() : null);
        this.registerChildVM(srcContainer);
        itemsCountTxt = new StringFormatVM("%d %s", itemCount,
                new SimpleValueVM(getResources().getString(R.string.psdarch_fragment_action_move_number_of_items)));
        registerChildVM(itemsCountTxt);
        this.pm = pm;
        if (null != srcContainer) {
            srcContainerVM = srcContainer;
        }
        this.getListCommand = getListCommand;
    }

    /**
     * Set a new command-object which checks the validity of the impending checkAndMove-operation
     * @param cmd the command-object
     */
    public void setCheckMoveCommand(CheckMoveCommand cmd)
    {
        cmd.setMover(this);
        this.checkMoveCommand = cmd;
    }

    /**
     * Add a new Item for the checkAndMove-operation
     *
     * @param dbIds List of pairs of position in the list to checkAndMove from and references
     *              to DbId-object of the items to checkAndMove
     */
    public void addItems(List<Pair<Integer, DbId>> dbIds)
    {
        for(Pair<Integer, DbId> item : dbIds)
            items.append(item.first, item.second);
        itemCount.set(items.size());
    }

    public SparseArray<DbId> getItems() {
        return items;
    }

    public boolean hasItem(SRC model) {
        DbId needle = model.getDbId();

        if (null == needle)
            return false;

        SparseArray<DbId> items = getItems();

        for(int i=0; i<items.size(); i++)
        {
            DbId dbid = items.valueAt(i);
            if (dbid.equals(needle))
                return true;
        }

        return false;
    }

    /**
     * remove an item from the checkAndMove-operation
     *
     * @param pos
     */
    public void removeItem(int pos)
    {
        items.remove(pos);
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

        ComplexViewModel realDestination = checkMove(dstVM);
        if (realDestination != null) {
            realDestination = this.checkMoveCommand.moveCommit(dstVM);

            IListViewModel listVM = getListCommand.getVM(srcContainerVM);
            IListViewModel dstListVM = getListCommand.getVM(realDestination);
            ArrayList<DbId> dbIds = new ArrayList<>();

            for (int i = 0; i < items.size(); i++) {
                DbId dbId = items.valueAt(i);
                dbIds.add(dbId);
            }

            DbId dstId = ((IPersistable) realDestination.getModel()).getDbId();
            pm.move(((IPersistable)getSource().getModel()).getDbId(),
                    ((IPersistable) realDestination.getModel()).getDbId(),
                    ((ComplexViewModel) listVM).getModelField(),
                    listVM.size(),
                    dstListVM.size(),
                    dbIds);
            Object dstModel = pm.load(dstId);
            realDestination.replaceModel(dstModel);

            dstVM.notifyModelChanged();
            dstVM.notifyViewModelDirty();
            return true;
        }

        return false;
    }

    public <T extends ComplexViewModel> boolean move(T srcVM, T dstVM)
    {
        if (null != srcVM) {
            srcContainerVM = srcVM;
        }

        return move(dstVM);
    }

    public <T extends ComplexViewModel> boolean move() {
        return move(dstConainerVM);
    }


    /**
     * Triggers the ceck of the checkAndMove-operation an updates the Flag for the validity of the operation
     *
     * @param dstVM potential target for the checkAndMove operation
     * @return The real destination if the operation is allowed, null otherwise
     */
    public ComplexViewModel checkMove(ComplexViewModel dstVM) {
        ComplexViewModel ret = this.checkMoveCommand.checkMove(dstVM);
        this.actionValid.set(ret != null);
        return ret;
    }

    public ComplexViewModel checkMove() {
        return checkMove(dstConainerVM);
    }

    /**
     * returns a reference to the source-container
     * @return the source-container
     */
    public ComplexViewModel getSource() {
        return srcContainerVM;
    }
    protected void setSource(ComplexViewModel newSrc) {
        srcContainerVM = newSrc;
    }

    public void setDestination(ComplexViewModel dst) {
        dstConainerVM = dst;
    }

    public void clear() {
        this.getItems().clear();
        itemCount.set(items.size());
    }

    public PersistanceManager getPm() { return this.pm; }
}
