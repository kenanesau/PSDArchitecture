package com.privatesecuredata.arch.mvvm.vm;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.mvvm.IViewModel;
import com.privatesecuredata.arch.mvvm.ViewModelCommitHelper;

/**
 * A Viewmodel which is capable of encapsulating Lists of models.
 * 
 * This Viewmodel encapsulates a whole list of Viewmodels. This means if you pass
 * in a List<M> all objects in it are encapsulated in ViewModel<M>-objects.
 * 
 * This is relative expensiv: So only use it with very few objects.
 * 
 * @author kenan
 *
 * @param <M> Type of Model
 * @param <E> Type of ViewModel encapsulating a single Model-object instance
 * @see ViewModel
 * @see SimpleValueVM<>
 * @see IViewModel<>
 */
public class ListViewModel<M, E extends IViewModel<M>> extends ComplexViewModel<Collection<M>> implements List<E> {
	
	public interface ICommitItemCallback<M>
	{
		void removeItem(IViewModel<M> item);
		void addItem(IViewModel<M> item);
	}
	
	private ArrayList<E> items = new ArrayList<E>();
	protected ArrayList<E> deletedItems = new ArrayList<E>();
	protected ArrayList<E> newItems = new ArrayList<E>();
	private Class<M> modelClass;
	private Class<E> viewModelClass;
	private Constructor<E> vmConstructor;
	private ICommitItemCallback itemCB;

	public ListViewModel(Class<M> modelClazz, Class<E> vmClazz) 
	{
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
	 * Initialise the ViewModel. This creates a ViewModel-Wrapper around each Model, puts them 
	 * into a List and registers them as childs of this ViewModel.
	 * 
	 * @param modelItems List of Model-instances
	 */
	public void init(Collection<M> modelItems)
	{
		setModel(modelItems);
		
		try {
			Iterator<M> it = modelItems.iterator();
			while(it.hasNext())
			{
				M model = it.next();
				E vm = vmConstructor.newInstance(model);
				registerChildVM(vm);
				items.add(vm);
			}
		}
		catch (Exception ex)
		{
			throw new ArgumentException("Unable to create ViewModel-objects", ex);			
		}
	}
	
	@Override
	public boolean add(E object) {
		registerChildVM(object);
		newItems.add(object);
		boolean ret = items.add(object);
		
		this.notifyViewModelDirty();
		
		return ret;
	}

	@Override
	public void add(int location, E object) {
		registerChildVM(object);
		newItems.add(location, object);
		items.add(location, object);
		this.notifyViewModelDirty();
	}

	@Override
	public boolean addAll(Collection<? extends E> arg0) {
		newItems.addAll(arg0);
		boolean ret = items.addAll(arg0);
		for(IViewModel<?> vm : arg0)
			registerChildVM(vm);
		
		this.notifyViewModelDirty();
		
		return ret;
	}

	@Override
	public boolean addAll(int arg0, Collection<? extends E> arg1) {
		newItems.addAll(arg1);
		boolean ret = items.addAll(arg0, arg1);
		for(IViewModel<?> vm : arg1)
			registerChildVM(vm);
		
		this.notifyViewModelDirty();
		
		return ret;
	}

	@Override
	public void clear() {
		deletedItems.addAll(items);
		newItems.clear();
		for(IViewModel<?> vm : items)
			unregisterChildVM(vm);
		items.clear();
		
		this.notifyViewModelDirty();
	}

	@Override
	public boolean contains(Object object) {
		return items.contains(object);
	}

	@Override
	public boolean containsAll(Collection<?> arg0) {
		return items.containsAll(arg0);
	}

	@Override
	public E get(int location) {
		return items.get(location);
	}

	@Override
	public int indexOf(Object object) {
		return items.indexOf(object);
	}

	@Override
	public boolean isEmpty() {
		return items.isEmpty();
	}

	@Override
	public Iterator<E> iterator() {
		return items.iterator();
	}

	@Override
	public int lastIndexOf(Object object) {
		return items.lastIndexOf(object);
	}

	@Override
	public ListIterator<E> listIterator() {
		return items.listIterator();
	}

	@Override
	public ListIterator<E> listIterator(int location) {
		return items.listIterator(location);
	}

	@Override
	public E remove(int location) {
		E item = items.remove(location);
		deletedItems.add(item);
		unregisterChildVM(item);
		notifyViewModelDirty();
		return item;
	}

	@Override
	public boolean remove(Object object) {
		boolean ret = items.remove(object);
		if (ret == true)
		{
			deletedItems.add((E) object);
			unregisterChildVM((IViewModel<?>)object);
			notifyViewModelDirty();
		}
		return ret;
	}

	@Override
	public boolean removeAll(Collection<?> arg0) {
		boolean ret = items.removeAll(arg0);
		Iterator<?> it = arg0.iterator();
		
		while(it.hasNext())
		{
			Object obj = it.next();
			unregisterChildVM((IViewModel<?>)obj);
			deletedItems.add((E)obj);
		}
		
		notifyViewModelDirty();
		return ret;
	}

	@Override
	public boolean retainAll(Collection<?> arg0) {
		ArrayList<E> _deletedItems = new ArrayList<E>(items);
		boolean ret = items.retainAll(arg0);
		if (ret)
		{
			_deletedItems.removeAll(arg0);
			for(IViewModel<?> vm : _deletedItems)
				unregisterChildVM(vm);
			deletedItems.addAll(_deletedItems);
		}
		
		notifyViewModelDirty();
		return ret;
	}

	@Override
	public E set(int location, E object) {
		E item = items.set(location, object);
		unregisterChildVM(item);
		deletedItems.add(item);
		notifyViewModelDirty();
		return item;
	}

	@Override
	public int size() {
		return items.size();
	}

	@Override
	public List<E> subList(int start, int end) {
		return items.subList(start, end);
	}

	@Override
	public Object[] toArray() {
		return items.toArray();
	}

	@Override
	public <T> T[] toArray(T[] array) {
		return items.toArray(array);
	}
	
	@Override
	public void commit() {
		boolean ret = false;
		super.commitData();
		Collection<M> model = getModel();
		for (Iterator<E> iterator = deletedItems.iterator(); iterator.hasNext();) {
			E vm = iterator.next();
			ret = model.remove(vm.getModel());
			if (itemCB != null)
				itemCB.removeItem(vm);
		}
		deletedItems.clear();
		
		for (Iterator<E> iterator = newItems.iterator(); iterator.hasNext();) {
			E vm = iterator.next();
			ret = model.add(vm.getModel());
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
	public void registerItemCallback(ICommitItemCallback itemCB) {
		if (null != this.itemCB)
			throw new ArgumentException("There is already an ItemCallback registered. Unregister first!");
		this.itemCB = itemCB;
	}

}
