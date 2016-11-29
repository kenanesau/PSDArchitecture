package com.privatesecuredata.arch.mvvm.vm;

import android.util.SparseArray;
import android.widget.Filter;
import android.widget.Filterable;

import com.privatesecuredata.arch.db.DbId;
import com.privatesecuredata.arch.db.query.Query;
import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.mvvm.MVVM;
import com.privatesecuredata.arch.mvvm.android.MVVMActivity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * A Viewmodel which is capable of encapsulating a List of models. The List itself is encapsulated 
 * by a Class implementing IModelListCallback. This way the list could also be a DB-Cursor or whatever.
 * 
 * @author kenan
 *
 * @param <M> Type of Model
 * @param <VM> Type of ViewModel encapsulating a single Model-object instance
 * 
 * @see ViewModel
 * @see SimpleValueVM<>
 * @see IViewModel<>
 */
public class EncapsulatedListViewModel<M, VM extends IViewModel<M>> extends ComplexViewModel<List<M>>
                                                                    implements IListViewModel<M, VM>,
                                                                    IDbBackedListViewModel,
                                                                    Filterable
{

    /**
	 * Interface which encapsulates the list representation (eg. a DB-Cursor)
	 * 
	 * @author kenan
	 *
	 * @param <M> Model item which was removed/added/updated from/to the list
     * @see com.privatesecuredata.arch.db.CursorToListAdapter
	 */
	public interface IModelListCallback<M> extends Filterable {
		void init(Class<?> parentClazz, Object parent, Class<M> childClazz);
        void setSortOrder(OrderBy... order);
		M get(int pos);
        DbId getDbId(int pos);
		int size();
		
		void commitFinished();
		void remove(M item);
		void save(Collection<M> items);
        void save(SparseArray<M> items);
		
		List<M> getList();
        Filter getFilter();
        String getFilteredParamId();
        void setFilterParamId(String filteredColumn);

        void registerForDataChange(IDataChangedListener provider);

        void setQuery(String queryId);
        void setQuery(Query query);
        void where(String id, Object value);
        void where(String id, Class value);
        void runQuery();

        /**
         * Call this to get rid of still open resources (Cursor)
         */
        void dispose();
    }
	
	protected ArrayList<M> deletedItems = new ArrayList<M>();
	protected ArrayList<M> newItems = new ArrayList<M>();
    protected ArrayList<VM> newVMs = null;
    protected SparseArray<M> dirtyItems = new SparseArray<M>();
	private Class<?> referencingType;
    private Class<M> referencedType;
	private Class<VM> vmType;
	private Constructor<VM> vmConstructor;
	private IModelListCallback<M> listCB;
    private boolean dataLoaded = false;

	private HashMap<Integer, VM> positionToViewModel = new HashMap<Integer, VM>();

    public EncapsulatedListViewModel(MVVMActivity ctx,
                                     Class<M> referencedType,
                                     Class<VM> vmType,
                                     IModelListCallback<M> listCB,
                                     OrderBy... sortOrderTerms)
    {
        this(ctx.getDefaultPM().createMVVM(), null, referencedType, vmType, listCB);
        if (null != sortOrderTerms)
            setSortOrder(sortOrderTerms);
    }

    public EncapsulatedListViewModel(MVVMActivity ctx,
                                     Class<M> referencedType,
                                     Class<VM> vmType,
                                     IModelListCallback<M> listCB
                                     )
    {
        this(ctx, referencedType, vmType, listCB,  null);
    }

    /**
     *
     * @param referencingType Type of the class referencing the model (The foreign key in DB-terms)
     * @param referencedType
     * @param vmType
     * @param listCB
     * @param sortOrderTerms Immediately set the terms by which to sort the list
     */
    public EncapsulatedListViewModel(MVVM mvvm,
                                     Class<?> referencingType, Class<M> referencedType,
                                     Class<VM> vmType,
                                     IModelListCallback<M> listCB,
                                     OrderBy... sortOrderTerms)
    {
        this(mvvm, referencingType, referencedType, vmType, listCB);
        setSortOrder(sortOrderTerms);
    }

    /**
     *
     * @param referencingType Type of the class referencing the model (The foreign key in DB-terms)
     * @param referencedType
     * @param vmType
     * @param listCB
     */
	public EncapsulatedListViewModel(MVVM mvvm,
                                     Class<?> referencingType, Class<M> referencedType,
                                     Class<VM> vmType,
                                     IModelListCallback<M> listCB)
	{
		super(mvvm);
        this.referencingType = referencingType;
		this.listCB = listCB;
        this.listCB.registerForDataChange(new IDataChangedListener() {
            @Override
            public void notifyDataChanged() {
                /** Tell the ListAdapter to update the View **/
                EncapsulatedListViewModel.this.notifyModelChanged();
            }
        });
		this.referencedType = referencedType;
		this.vmType = vmType;
		
		if ( (null==referencedType) || (null==vmType) || (null==listCB))
			throw new ArgumentException("No parameter of this constructor is allowed to be null");
		
		try {
			this.vmConstructor = this.vmType.getConstructor(MVVM.class, this.referencedType);
		}
		catch (NoSuchMethodException ex)
		{
			throw new ArgumentException(String.format("Unable to find a valid constructor for the model of type \"%s\"", this.referencedType.getName()), ex);
		}
	}

    public void setQueryId(String queryId) { listCB.setQuery(queryId); }
    public void where(String id, Object val) { listCB.where(id, val);}
    public void where(String id, Class val) { listCB.where(id, val);}
    public void runQuery() { listCB.runQuery(); }

    @Override
    public void setSortOrder(OrderBy... sortOrderTerms) {
        this.listCB.setSortOrder(sortOrderTerms);

        if (dataLoaded)
        {
            ComplexViewModel vm = getParentViewModel();
            Field fld = getModelField();

            if ( (vm != null) && (fld != null) )
                init(getParentViewModel(), getModelField());
        }
    }

    public void init(ComplexViewModel<?> parentVM, Field modelField)
	{
		dataLoaded = false;
        setParentViewModel(parentVM);
        setModelAccess(parentVM, modelField);

        load();
	}

    public void init(ComplexViewModel<?> parentModel)
    {
        this.dataLoaded = false; //since we've got new data...
        setModelGetter(parentModel, null);
        load();
    }

    protected void setModelAccess(ComplexViewModel<?> model, Field modelField)
    {
        setModelGetter(model, modelField);
    }

    public IModelListCallback<M> getModelListCallback() {
        return this.listCB;
    }

    /**
     * get the cursor from the database (with CursorToListAdapter)
     */
    @Override
    public void load()
    {
        if (!dataLoaded) {
            listCB.init(referencingType, getParentModel(), referencedType);
            dataLoaded = true;
        }
    }

    public boolean add(VM vm)
    {
        if (null == newVMs)
            newVMs = new ArrayList<VM>();

        vm.addViewModelListener(this);
        this.newVMs.add(vm);
        notifyViewModelDirty();
        return true;
    }

	public boolean add(M object) {
		boolean ret = newItems.add(object);
        notifyViewModelDirty();
		return ret;
	}

	public void add(int location, M object) {
		newItems.add(location, object);
        notifyViewModelDirty();
	}

	public boolean addAll(Collection<? extends M> arg0) {
		boolean ret = newItems.addAll(arg0);
        notifyViewModelDirty();
		return ret;
	}

	public boolean addAll(int arg0, Collection<? extends M> arg1) {
		boolean ret = newItems.addAll(arg1);
        notifyViewModelDirty();
		return ret;
	}

	public M get(int pos) {
        load();
        M ret = dirtyItems.get(pos);
        if (null == ret)
		    ret = listCB.get(pos);

        return ret;
	}

    @Override
    public DbId getDbId(int pos) {
        return listCB.getDbId(pos);
    }

    public boolean isEmpty() {
		load();
        return ( (listCB.size()==0) && (this.newItems.size()==0) );
	}

	public boolean remove(M object) {
        load();
		boolean ret = deletedItems.add((M) object);
        notifyViewModelDirty();
		return ret;
	}

    public M remove(int location) {
        load();
        M item = get(location);
        if (remove(item)) {
            notifyViewModelDirty();
            return item;
        }
        else
        {
            return null;
        }
    }

    public void clear() {
        load();
        for (int i=0; i<listCB.size(); i++)
        {
            deletedItems.add(listCB.get(i));
        }
        newItems.clear();
        dirtyItems.clear();
        notifyViewModelDirty();
    }

	public boolean removeAll(Collection<?> arg0) {
        load();
		Iterator<?> it = arg0.iterator();
		boolean ret = false;
		
		while(it.hasNext())
		{
			M obj = (M)it.next();
			remove(obj);
		}
		
		return ret;
	}

	public int size() {
		load();
        return listCB.size();
	}

    public int dirtySize() {
        return listCB.size() + newItems.size() - deletedItems.size();
    }

	@Override
	public boolean isDirty() {
		return ( (newItems.size()>0) || (deletedItems.size()>0) || super.isDirty());
	};

    @Override
	public void commitData() {
		if (!this.isDirty())
			return;

        /**
         * It can happen that we have made changes without getting the cursor before
         * (eg. add without reading before)
         */
        load();

        /**
         * first commit() is called -- here we have to save all changed child-VMs
         * later the DBViewModelCommitListener calls save() on this List-VM -- then those
         * VMs saved in the changedChildren-list are also saved to the DB...
         */
		for(IViewModel<M> vm : positionToViewModel.values()) {
			if(vm.isDirty())
			{
                vm.commit();
                newItems.add(vm.getModel());
			}
		}

        /**
         * Commit all children
         */
        List<IListViewModel> listVMs = new ArrayList<IListViewModel>();
        List<IViewModel<?>> children = getChildrenOrdered();
        if (null != children) {
            for (IViewModel<?> vm : children) {
                if (vm instanceof IListViewModel) {
                    listVMs.add((IListViewModel) vm);
                    continue;
                }
            }
        }

        if (null != newVMs) {
            for (IViewModel<?> vm : newVMs) {
                if (vm instanceof ComplexViewModel) {
                    // Disable global notify since the children of a list are saved to DB when the
                    // whole list is saved
                    ((ComplexViewModel) vm).disableGlobalNotify();
                    vm.commit();
                    if (vmType.isInstance(vm))
                        newItems.add((M) vm.getModel());
                    ((ComplexViewModel) vm).enableGlobalNotify();
                }
            }
            newVMs.clear();
            newVMs = null;
        }

        /**
         * Commit the lists AFTER everything else since there could be a foreign key relation
         * in the DB -> Ensure that the rest of the parent-model is already clean.
         */
        for(IListViewModel listVM : listVMs) {
            listVM.commit();
        }

        this.setClean();
	}

    public void save()
    {
        load();
        for (Iterator<M> iterator = deletedItems.iterator(); iterator.hasNext();) {
            M item = iterator.next();
            listCB.remove(item);
        }
        deletedItems.clear();

        if (newItems.size() > 0) {
            listCB.save(newItems);
            newItems.clear();
        }
        if (dirtyItems.size() > 0) {
            listCB.save(dirtyItems);
            dirtyItems.clear();
        }

        listCB.commitFinished();
    }

	/**
	 * Return a ViewModel of a Model at the specified position.
	 * 
	 * @param pos
	 * @return ViewModel
	 */
	public VM getViewModel(int pos)
	{
		VM vm = null;
		
		try {
			if (hasViewModel(pos))
				return positionToViewModel.get(pos);
			else
			{
                if (pos < this.size() ) {
                    M model = get(pos);
                    if (null != model) {
                        vm = (VM)getMVVM().createVM(model);
                        registerChildVM(vm);
                        vm.addModelListener(this);
                        positionToViewModel.put(pos, vm);
                    } else
                        throw new ArgumentException("Could not find object at position");
                }
			}
		}
		catch (Exception ex)
		{
			throw new ArgumentException("Unable to create ViewModel-object", ex);			
		}

		return vm;
	}

    @Override
    public boolean hasViewModel(int pos) {
        return positionToViewModel.containsKey(pos);
    }

    @Override
    public IDbBackedListViewModel db() {
        return this;
    }

    @Override
    public Filter getFilter() {
        return listCB.getFilter();
    }

    public void setFilterParamId(String filterParamId)
    {
        listCB.setFilterParamId(filterParamId);
    }

    @Override
    public void notifyModelChanged(IViewModel<?> changedViewModel, IViewModel<?> originator) {
        Object model = changedViewModel.getModel();
        /**
         * If the originator is an EncapsulatedListViewModel this is the load()-operation
         */
        if ( (null != model) && !(originator instanceof EncapsulatedListViewModel) )
        {
            if (model.getClass().equals(referencedType))
            {
                /**
                 * childVM-was comitted -> reload the cursor...
                 */
                listCB.init(referencingType, getParentModel(), referencedType);

                /**
                 * Clear the list since the postition might have changed
                 */
                positionToViewModel.clear();
            }
        }
        super.notifyModelChanged(changedViewModel, originator);
    }

    public String getFilteredColumn()
    {
        return listCB.getFilteredParamId();
    }

}
