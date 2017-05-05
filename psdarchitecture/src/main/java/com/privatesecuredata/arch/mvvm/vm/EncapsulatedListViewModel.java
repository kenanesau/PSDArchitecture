package com.privatesecuredata.arch.mvvm.vm;

import android.util.SparseArray;
import android.widget.Filter;
import android.widget.Filterable;

import com.privatesecuredata.arch.db.CursorToListAdapter;
import com.privatesecuredata.arch.db.DbId;
import com.privatesecuredata.arch.db.PersistanceManager;
import com.privatesecuredata.arch.db.query.Query;
import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.exceptions.MVVMException;
import com.privatesecuredata.arch.mvvm.MVVM;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * A Viewmodel which is capable of encapsulating a List of models. The List itself is encapsulated 
 * by a Class implementing IModelListCallback. This way the list could also be a DB-Cursor or whatever.
 * 
 * @author kenan
 *
 * @param <M> Type of Model
 *
 * @see ViewModel
 * @see SimpleValueVM<>
 * @see IViewModel<>
 */
public class EncapsulatedListViewModel<M> extends ComplexViewModel<List<M>>
                                                                    implements IListViewModel<M>,
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
		
		Observable<IModelListCallback<M>> loadModel();
        void commitFinished();
		void remove(M item);
		void save(Collection<M> items);
        void save(SparseArray<M> items);
		
		List<M> getList();
        Filter getFilter();
        String getFilteredParamId();
        void setFilterParamId(String filteredColumn);

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
    protected ArrayList<ComplexViewModel> newVMs = null;
    protected SparseArray<M> dirtyItems = new SparseArray<M>();
	private Class<?> referencingType;
    private Class<M> referencedType;
	private Class<?> vmType;
	private Constructor vmConstructor;
	private IModelListCallback<M> listCB;
    private boolean dataLoaded = false;
    private ReentrantLock mtx = new ReentrantLock();

	private HashMap<Integer, ComplexViewModel<M>> positionToViewModel = new HashMap<Integer, ComplexViewModel<M>>();

    protected EncapsulatedListViewModel(PersistanceManager pm,
                                        Class<M> referencedType,
                                        Class vmType,
                                        IModelListCallback<M> cb,
                                        Query q)
    {
        this(pm, referencedType, vmType, cb);
        cb.setQuery(q);
    }

    public EncapsulatedListViewModel(PersistanceManager pm,
                                     Query q,
                                     Class<M> referencedType,
                                     Class vmType)
    {
        this(pm, referencedType, vmType, new CursorToListAdapter(pm), q);
    }

    public EncapsulatedListViewModel(PersistanceManager pm,
                                     Class<M> referencedType,
                                     Class vmType,
                                     IModelListCallback<M> listCB,
                                     OrderBy... sortOrderTerms)
    {
        this(pm.createMVVM(), null, referencedType, vmType, listCB);
        if (null != sortOrderTerms)
            setSortOrder(sortOrderTerms);
    }

    public EncapsulatedListViewModel(PersistanceManager pm,
                                     Class<M> referencedType,
                                     Class vmType,
                                     IModelListCallback listCB
                                     )
    {
        this(pm, referencedType, vmType, listCB, (OrderBy[]) null);
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
                                     Class vmType,
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
                                     Class vmType,
                                     IModelListCallback<M> listCB)
	{
		super(mvvm);
        this.referencingType = referencingType;
		this.listCB = listCB;
		this.referencedType = referencedType;
		this.vmType = vmType;
		
		if ( (null==referencedType) || (null==vmType) || (null==listCB))
			throw new ArgumentException("No parameter of this constructor is allowed to be null");
		
		try {
			this.vmConstructor = this.vmType.getConstructor(MVVM.class, this.referencedType);
		}
		catch (NoSuchMethodException ex)
		{
			throw new ArgumentException(String.format("Unable to find a valid constructor for the viewmodel of type \"%s\"", this.vmType.getName()), ex);
		}
	}

    public void setQueryId(String queryId) { listCB.setQuery(queryId); }
    public void where(String id, Object val) { listCB.where(id, val);}
    public void where(String id, Class val) { listCB.where(id, val);}

    @Override
    public void setSortOrder(OrderBy... sortOrderTerms) {
        this.listCB.setSortOrder(sortOrderTerms);

        if (dataLoaded)
        {
            ComplexViewModel vm = getParentViewModel();
            Field fld = getModelField();

            /*if ( (vm != null) && (fld != null) )
                init(getParentViewModel(), getModelField());*/
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

    @Override
    public void reload() {
        super.reload();
        loadData();
    }

    protected void setModelAccess(ComplexViewModel<?> model, Field modelField)
    {
        setModelGetter(model, modelField);
    }

    public IModelListCallback<M> getModelListCallback() {
        return this.listCB;
    }

    /**
     * Inherited from ComplexVM: load the lazily loaded data...
     */
    @Override
    public void load()
    {
        if (!dataLoaded)
            loadData();
    }

    /**
     * load the cursor from the database (with CursorToListAdapter)
     *
     * When the data was not loaded yet, each operation on this List-VM will trigger
     * this function (e.g. calling size(), get(), ...)
     */
    @Override
    public void loadData()
    {
        listCB.init(referencingType, getParentModel(), referencedType);

        ///dataLoaded is set to true in notifyDataChanged()
        //dataLoaded = true;
    }

    /**
     * triggers loading of the DB-cursor on the io-thread and returns an observable of
     * IListViewModel if you subscribe to the observable
     *
     * @return
     */
    @Override
    public Observable<IListViewModel<M>> loadDataAsync() {
        return Observable.defer(new Callable<ObservableSource<? extends IListViewModel<M>>>() {
            @Override
            public ObservableSource<? extends IListViewModel<M>> call() throws Exception {
                return Observable.just((IListViewModel<M>) EncapsulatedListViewModel.this);
            }
        })
        .subscribeOn(Schedulers.io())
        .map(new Function<IListViewModel<M>, IListViewModel<M>>() {
            @Override
            public IListViewModel<M> apply(IListViewModel<M> list) throws Exception {
                list.db().loadData();
                return list;
            }
        });
    }

    public <VM extends IViewModel> boolean add(VM vm)
    {
        try {
            mtx.lock();
            if (null == newVMs)
                newVMs = new ArrayList<ComplexViewModel>();

            vm.addViewModelListener(this);
            this.newVMs.add((ComplexViewModel) vm);
            notifyViewModelDirty();
            return true;
        }
        finally {
            mtx.unlock();
        }
    }

	public boolean add(M object) {
        try {
            mtx.lock();
            boolean ret = newItems.add(object);
            notifyViewModelDirty();
            return ret;
        }
        finally {
            mtx.unlock();
        }
    }

    @Override
    public boolean addAll(IListViewModel<M> list) {
        return FastListViewModel.addAllListVM(list, this);
    }

    public void add(int location, M object) {
        try {
            mtx.lock();
            newItems.add(location, object);
            notifyViewModelDirty();
        }
        finally {
            mtx.unlock();
        }
    }

	public boolean addAll(Collection<? extends M> arg0) {
        try {
            mtx.lock();
            boolean ret = newItems.addAll(arg0);
            notifyViewModelDirty();
            return ret;
        }
        finally {
            mtx.unlock();
        }
    }

	public boolean addAll(int arg0, Collection<? extends M> arg1) {
        try {
            mtx.lock();
            boolean ret = newItems.addAll(arg1);
            notifyViewModelDirty();
            return ret;
        }
        finally {
            mtx.unlock();
        }
    }

	public M get(int pos) {
        try {
            mtx.lock();
            load();
            M ret = dirtyItems.get(pos);
            if (null == ret)
                ret = listCB.get(pos);

            return ret;
        }
        finally {
            mtx.unlock();
        }
    }

    @Override
    public DbId getDbId(int pos) {
        return listCB.getDbId(pos);
    }

    public boolean isEmpty() {
        try {
            mtx.lock();
            load();
            return ((listCB.size() == 0) && (this.newItems.size() == 0));
        }
        finally {
            mtx.unlock();
        }
    }

	public boolean remove(M object) {
        try {
            mtx.lock();
            load();
            boolean ret = deletedItems.add((M) object);
            notifyViewModelDirty();
            return ret;
        }
        finally {
            mtx.unlock();
        }
    }

    public M remove(int location) {
        try {
            mtx.lock();
            load();
            M item = get(location);
            if (remove(item)) {
                notifyViewModelDirty();
                return item;
            } else {
                return null;
            }
        }
        finally {
            mtx.unlock();
        }
    }

    public void clear() {
        try {
            mtx.lock();
            load();
            for (int i = 0; i < listCB.size(); i++) {
                deletedItems.add(listCB.get(i));
            }
            newItems.clear();
            dirtyItems.clear();
            notifyViewModelDirty();
        }
        finally {
            mtx.unlock();
        }
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
        try {
            mtx.lock();
            int ret = listCB.size() + newItems.size() - deletedItems.size();

            return (newVMs != null) ? ret + newVMs.size() : ret;
        }
        finally {
            mtx.unlock();
        }
    }

	@Override
	public boolean isDirty() {
        try {
            mtx.lock();
            return ( (newItems.size()>0) || (deletedItems.size()>0) || super.isDirty());
        }
        finally {
            mtx.unlock();
        }
    };

    @Override
	public void commitData() {
		if (!this.isDirty())
			return;

        try {
            mtx.lock();
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
            /*for(IViewModel<M> vm : positionToViewModel.values()) {
                if(vm.isDirty())
                {
                    vm.commit();
                    newItems.add(vm.getModel());
                }
            }*/

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
                    } else {
                        if (vm instanceof ComplexViewModel) {
                            vm.commit();
                            newItems.add((M) vm.getModel());
                        }
                    }
                }
            }

            if (null != newVMs) {
                for (IViewModel<?> vm : newVMs) {
                    if (vm instanceof ComplexViewModel) {
                        vm.commit();
                        if (vmType.isInstance(vm))
                            newItems.add((M) vm.getModel());
                    }
                }
                newVMs.clear();
                newVMs = null;
            }

            /**
             * Commit the lists AFTER everything else since there could be a foreign key relation
             * in the DB -> Ensure that the rest of the parent-model is already clean.
             */
            for (IListViewModel listVM : listVMs) {
                listVM.commit();
            }

            this.setClean();

            /**
             * Update of Adapter data-source and the appropriate notifyDataSetchanged have to be
             * called from the main-thread...
             */
            save();
            listCB.commitFinished();
            dataLoaded = true;

        }
        catch (Exception e) {
            throw new MVVMException("Error committing list!", e);
        }
        finally {
            mtx.unlock();
        }
    }

    private void save()
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

    }

	/**
	 * Return a ViewModel of a Model at the specified position.
	 * 
	 * @param pos
	 * @return ViewModel
	 */
	public <VM extends IViewModel> VM getViewModel(int pos)
	{
		VM vm = null;
		
		try {
            mtx.lock();
			if (hasViewModel(pos))
				return (VM)positionToViewModel.get(pos);
			else
			{
                if (pos < this.size() ) {
                    M model = get(pos);
                    if (null != model) {
                        vm = (VM)getMVVM().createVM(model);
                        registerChildVM(vm);
                        vm.addModelListener(this);
                        positionToViewModel.put(pos, (ComplexViewModel)vm);
                    } else
                        throw new ArgumentException("Could not find object at position");
                }
			}
		}
		catch (Exception ex)
		{
			throw new ArgumentException("Unable to create ViewModel-object", ex);			
		}
		finally {
            mtx.unlock();
        }

		return vm;
	}

    @Override
    public boolean hasViewModel(int pos) {
        try {
            mtx.lock();
            return positionToViewModel.containsKey(pos);
        }
        finally {
            mtx.unlock();
        }
    }

    @Override
    public IDbBackedListViewModel db() {
        return this;
    }

    @Override
    public void dispose() {
        this.getModelListCallback().dispose();
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

        try {
            mtx.lock();
            Object model = changedViewModel.getModel();
            /**
             * If the originator is an EncapsulatedListViewModel this is the load()-operation
             */
            if ((null != model) && !(originator instanceof EncapsulatedListViewModel)) {
                if (model.getClass().equals(referencedType)) {
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
        finally {
            mtx.unlock();
        }
    }

    public String getFilteredColumn()
    {
        return listCB.getFilteredParamId();
    }

}
