package com.privatesecuredata.arch.mvvm.vm;

import com.privatesecuredata.arch.db.DbId;
import com.privatesecuredata.arch.db.IPersistable;
import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.exceptions.MVVMException;
import com.privatesecuredata.arch.mvvm.MVVM;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * A Viewmodel which is capable of encapsulating Lists of models.
 * 
 * This Viewmodel encapsulates a whole list of Models. This means if you pass
 * in a List<M> no object in it is encapsulated in a ViewModel. This is only done
 * on demand (Call the getViewModel()).
 * 
 * 
 * @author kenan
 *
 * @param <M> Type of Model
 * @see ViewModel
 * @see com.privatesecuredata.arch.mvvm.vm.SimpleValueVM<>
 * @see IViewModel <>
 */
public class FastListViewModel<M> extends ComplexViewModel<List<M>> implements List<M>, IListViewModel<M>
{
    private boolean clearAll = false;

    public static <MODEL, VIEWMODEL extends IViewModel> boolean addAllListVM(IListViewModel<MODEL> src, IListViewModel<MODEL> dst)
	{
		for (int n=0; n<src.size(); n++) {
			MODEL m = src.get(n);

            ///HACK!!! Need a proper solution for items being transferred from on List-VM to another
			if (m instanceof IPersistable)
				((IPersistable) m).getDbId().setDirty();
			dst.add(m);
		}

		return true;
	}

	public interface ICommitItemCallback<M>
	{
		void removeItem(IViewModel<?> parent, M item);
		void addItem(IViewModel<?> parent, M item);
		void updateItem(IViewModel<?> parent, M item); 
	}
	
	protected ArrayList<M> deletedItems = new ArrayList<M>();
	protected ArrayList<M> newItems = new ArrayList<M>();
	private Class<M> modelClass;
	private Class viewModelClass;
	private Constructor vmConstructor;
	private ICommitItemCallback<M> itemCB;
	private boolean initialized = false;

	private HashMap<Integer, ComplexViewModel> positionToViewModel = new HashMap<Integer, ComplexViewModel>();
	private ComplexViewModel<?> parentVM;

	public FastListViewModel(MVVM mvvm, Class<M> modelClazz, Class vmClazz, IListViewModel<M> data)
	{
		this(mvvm, null, modelClazz, vmClazz);
		List<M> lst = new ArrayList<M>(data.size());
		for (int i=0; i<data.size(); i++)
			lst.add(data.get(i));

		init(lst);
	}

	public FastListViewModel(MVVM mvvm, Class<M> modelClazz, Class vmClazz, List<M> data)
	{
		this(mvvm, null, modelClazz, vmClazz);
		init(data);
	}

	public FastListViewModel(MVVM mvvm, ComplexViewModel<?> parentVM, Class<M> modelClazz, Class vmClazz, List<M> data)
	{
        this(mvvm, parentVM, modelClazz, vmClazz);
        init(data);
	}

	public FastListViewModel(MVVM mvvm, ComplexViewModel<?> parentVM, Class<M> modelClazz, Class vmClazz)
	{
		this(mvvm, modelClazz, vmClazz);
		this.parentVM = parentVM;
	}
	
	public FastListViewModel(MVVM mvvm, Class<M> modelClazz, Class vmClazz)
	{
        super(mvvm);
		this.modelClass = modelClazz;
		this.viewModelClass = vmClazz;
		
		if ( (null==modelClazz) || (null==vmClazz) )
			throw new ArgumentException("No parameter of this constructor is allowed to be null");
		
		try {
			this.vmConstructor = viewModelClass.getConstructor(MVVM.class, modelClass);
		}
		catch (NoSuchMethodException ex)
		{
			throw new ArgumentException(String.format("Unable to find a valid constructor for the model of type \"%s\"", viewModelClass.getName()), ex);
		}
	}

    public void init(ComplexViewModel<?> parentVM, Field modelField)
    {
        setModelGetter(parentVM, modelField);
        init(getModel());
    }

    public void init(Collection<M> modelItems)
	{
		init(new ArrayList<M>(modelItems));
	}
	
	/**
	 * Initialise the ViewModel. 
	 * 
	 * @param modelItems List of Model-instances
	 */
	public void init(List<M> modelItems)
	{
		setModel(modelItems);
		if (!Proxy.isProxyClass(modelItems.getClass()))
		{
			//this.items = new ArrayList<M>(modelItems);
			initialized = true;
		}
	}

    @Override
    public List<M> getModel() throws MVVMException
    {
        List<M> model = super.getModel();
        if ( (!initialized) && (null != model) ) {
            if (Proxy.isProxyClass(model.getClass()))
            {
                model = loadProxyData(model);
            }

            init(model);
        }

        return model;
    }

	@Override
	public boolean add(Object object) {
		newItems.add((M)object);
		
		boolean ret = false;
		if (!initialized)
			ret = getItems().add((M)object);
		
		this.notifyViewModelDirty();
		
		return ret;
	}

    public <VM extends IViewModel> boolean add(VM viewModel) {
        return this.add(viewModel.getModel());
    }

    @Override
	public boolean addAll(IListViewModel<M> list) {
		return addAllListVM(list, this);
	}

	@Override
	public void add(int location, M object) {
		newItems.add(location, object);
		this.notifyViewModelDirty();
	}

	@Override
	public boolean addAll(Collection<? extends M> arg0) {
		boolean ret = newItems.addAll(arg0);
		this.notifyViewModelDirty();
		return ret;
	}

	@Override
	public boolean addAll(int arg0, Collection<? extends M> arg1) {
		boolean ret = newItems.addAll(arg1);
		this.notifyViewModelDirty();
		return ret;
	}

	@Override
	public void clear() {
		deletedItems.addAll(getItems());
		newItems.clear();
		clearAll = true;
		this.notifyViewModelDirty();
	}

	@Override
	public boolean contains(Object object) {
		return getItems().contains(object);
	}

	@Override
	public boolean containsAll(Collection<?> arg0) {
		return getItems().containsAll(arg0);
	}

	@Override
	public M get(int location) {
		return getItems().get(location);
	}

    @Override
    public DbId getDbId(int pos) {
        return ((IPersistable)get(pos)).getDbId();
    }

    @Override
	public int indexOf(Object object) {
		return getItems().indexOf(object);
	}

	@Override
	public boolean isEmpty() {
		return getItems().isEmpty();
	}

	@Override
	public Iterator<M> iterator() {
		return getItems().iterator();
	}

	@Override
	public int lastIndexOf(Object object) {
		return getItems().lastIndexOf(object);
	}

	@Override
	public ListIterator<M> listIterator() {
		return getItems().listIterator();
	}

	@Override
	public ListIterator<M> listIterator(int location) {
		return getItems().listIterator(location);
	}

	@Override
	public M remove(int location) {
		M item = get(location);
		deletedItems.add(item);
		notifyViewModelDirty();
		return item;
	}

    @Override
	public boolean remove(Object object) {
		boolean ret = true;
		if (ret == true)
		{
			deletedItems.add((M) object);
			notifyViewModelDirty();
		}
		return ret;
	}

	@Override
	public boolean removeAll(Collection<?> arg0) {
		Iterator<?> it = arg0.iterator();
		
		while(it.hasNext())
		{
			M obj = (M)it.next();
			deletedItems.add(obj);
		}
		
		notifyViewModelDirty();
		return true;
	}

	@Override
	public boolean retainAll(Collection<?> arg0) {
		ArrayList<M> _deletedItems = new ArrayList<M>(getItems());
		boolean ret = getItems().retainAll(arg0);
		if (ret)
		{
			_deletedItems.removeAll(arg0);
			deletedItems.addAll(_deletedItems);
		}
		
		notifyViewModelDirty();
		return ret;
	}

	@Override
	public M set(int location, M object) {
		M item = getItems().set(location, object);
		deletedItems.add(item);
		notifyViewModelDirty();
		return item;
	}

	@Override
	public int size() {
        List<M> items = getItems();
		return items != null ? items.size() : 0;
	}

	@Override
	public int dirtySize() {
		return size();
	}

	@Override
	public List<M> subList(int start, int end) {
		return getItems().subList(start, end);
	}

	@Override
	public Object[] toArray() {
		return getItems().toArray();
	}

	@Override
	public <T> T[] toArray(T[] array) {
		return getItems().toArray(array);
	}
	
	@Override
	public void commit() {
		boolean ret = false;
		boolean wasDirty = this.isDirty();

		if (wasDirty)
			super.commitData();
				
		List<M> model = getModel();
        if (clearAll)
        {
            getItems().clear();
            clearAll = true;
        }
		for (Iterator<M> iterator = deletedItems.iterator(); iterator.hasNext();) {
			M item = iterator.next();
			ret = model.remove(item);
			if (itemCB != null)
				itemCB.removeItem(parentVM, item);
		}
		deletedItems.clear();
		
		for (Iterator<M> iterator = newItems.iterator(); iterator.hasNext();) {
			M item = iterator.next();
			ret = model.add(item);
			if (itemCB != null)
				itemCB.addItem(parentVM, item);
		}
		newItems.clear();

		if ( (wasDirty) && (isGlobalNotifyEnabled()) )
			getMVVM().notifyCommit(this);

		if (wasDirty)
			notifyModelChanged();
	}

	public void unregisterItemCallback() {
		this.itemCB = null;
	}

	/**
	 * Register a callback which is called during commit. For each VM which is deleted from the list
	 * removeItem is called. For each VM which was added to the list addItem is called.
	 * 
	 * @param itemCB 
	 */
	public void registerItemCallback(ICommitItemCallback<M> itemCB) {
		if (null != this.itemCB)
			throw new ArgumentException("There is already an ItemCallback registered. Unregister first!");
		this.itemCB = itemCB;
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
			if (hasViewModel(pos))
				return (VM)positionToViewModel.get(pos);
			else
			{
				M model = get(pos);
				if (null != model) {
					vm = (VM)vmConstructor.newInstance(getMVVM(), model);
					registerChildVM(vm);
					positionToViewModel.put(pos, (ComplexViewModel)vm);
				}
				else
					throw new ArgumentException("Could not find object at position");
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
		return null;
	}

	@Override
	public void dispose() {
	}

	@Override
    public ComplexViewModel<?> getParentViewModel() {
        return this.parentVM;
    }

  	private List<M> getItems() {
		return getModel();
	}

}
