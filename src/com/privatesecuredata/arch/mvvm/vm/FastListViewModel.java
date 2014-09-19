package com.privatesecuredata.arch.mvvm.vm;

import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.exceptions.MVVMException;
import com.privatesecuredata.arch.mvvm.IModel;
import com.privatesecuredata.arch.mvvm.ViewModelCommitHelper;

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
 * @param <E> Type of ViewModel encapsulating a single Model-object instance
 * @see ViewModel
 * @see SimpleViewModel<T>
 * @see IModel<T>
 */
public class FastListViewModel<M, E extends IModel<M>> extends ComplexViewModel<List<M>> implements List<M> {
	
	public interface ICommitItemCallback<M>
	{
		void removeItem(M item);
		void addItem(M item);
		void updateItem(M item); 
	}
	
	private ArrayList<M> items = new ArrayList<M>();
	protected ArrayList<M> deletedItems = new ArrayList<M>();
	protected ArrayList<M> newItems = new ArrayList<M>();
	private Class<M> modelClass;
	private Class<E> viewModelClass;
	private Constructor<E> vmConstructor;
	private ICommitItemCallback<M> itemCB;
	private boolean initialized = false;
	
	private HashMap<Integer, E> positionToViewModel = new HashMap<Integer, E>();

	public FastListViewModel(Class<M> modelClazz, Class<E> vmClazz)
	{
		super();
		this.modelClass = modelClazz;
		this.viewModelClass = vmClazz;
		
		if ( (null==modelClazz) || (null==vmClazz) )
			throw new ArgumentException("No parameter of this constructor is allowed to be null");
		
		try {
			this.vmConstructor = viewModelClass.getConstructor(modelClass);
		}
		catch (NoSuchMethodException ex)
		{
			throw new ArgumentException("Unable to find a valid constructor for the model", ex);
		}
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
			this.items = new ArrayList<M>(modelItems);
			initialized = true;
		}
	}

	@Override
	public boolean add(M object) {
		newItems.add(object);
		boolean ret = getItems().add(object);
		
		this.notifyChange();
		
		return ret;
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
	public void add(int location, M object) {
		newItems.add(location, object);
		getItems().add(location, object);
		this.notifyChange();
	}

	@Override
	public boolean addAll(Collection<? extends M> arg0) {
		newItems.addAll(arg0);
		boolean ret = getItems().addAll(arg0);
		this.notifyChange();
		
		return ret;
	}

	@Override
	public boolean addAll(int arg0, Collection<? extends M> arg1) {
		newItems.addAll(arg1);
		boolean ret = getItems().addAll(arg0, arg1);
		
		this.notifyChange();
		return ret;
	}

	@Override
	public void clear() {
		deletedItems.addAll(getItems());
		newItems.clear();
		getItems().clear();
		
		this.notifyChange();
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
		M item = getItems().remove(location);
		deletedItems.add(item);
		notifyChange();
		return item;
	}

	@Override
	public boolean remove(Object object) {
		boolean ret = getItems().remove(object);
		if (ret == true)
		{
			deletedItems.add((M) object);
			notifyChange();
		}
		return ret;
	}

	@Override
	public boolean removeAll(Collection<?> arg0) {
		boolean ret = getItems().removeAll(arg0);
		Iterator<?> it = arg0.iterator();
		
		while(it.hasNext())
		{
			Object obj = it.next();
			deletedItems.add((M)obj);
		}
		
		notifyChange();
		return ret;
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
		
		notifyChange();
		return ret;
	}

	@Override
	public M set(int location, M object) {
		M item = getItems().set(location, object);
		deletedItems.add(item);
		notifyChange();
		return item;
	}

	@Override
	public int size() {
		return getItems().size();
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
		
		super.commitData();
				
		List<M> model = getModel();
		for (Iterator<M> iterator = deletedItems.iterator(); iterator.hasNext();) {
			M item = iterator.next();
			ret = model.remove(item);
			if (itemCB != null)
				itemCB.removeItem(item);
		}
		deletedItems.clear();
		
		for (Iterator<M> iterator = newItems.iterator(); iterator.hasNext();) {
			M vm = iterator.next();
			ret = model.add(vm);
			if (itemCB != null)
				itemCB.addItem(vm);
		}
		newItems.clear();
		
		ViewModelCommitHelper.notifyCommit(this);
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
	public E getViewModel(int pos)
	{
		E vm = null;
		
		try {
			if (positionToViewModel.containsKey(pos))
				return positionToViewModel.get(pos);
			else
			{
				M model = get(pos);
				if (null != model) {
					vm = vmConstructor.newInstance(model);
					registerChildVM(vm);
					positionToViewModel.put(pos, vm);
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

	private ArrayList<M> getItems() {
		if (!initialized)
			getModel();
		
		return items;
	}

}
