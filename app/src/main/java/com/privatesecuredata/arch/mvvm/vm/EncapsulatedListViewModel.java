package com.privatesecuredata.arch.mvvm.vm;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.exceptions.MVVMException;
import com.privatesecuredata.arch.mvvm.IViewModel;
import com.privatesecuredata.arch.mvvm.ViewModelCommitHelper;

/**
 * A Viewmodel which is capable of encapsulating a List of models. The List itself is encapsulated 
 * by a Class implementing IModelListCallback. This way the list could also be a DB-Cursor or whatever.
 * 
 * @author kenan
 *
 * @param <M> Type of Model
 * @param <E> Type of ViewModel encapsulating a single Model-object instance
 * 
 * @see ViewModel
 * @see SimpleViewModel<T>
 * @see IModel<T>
 */
public class EncapsulatedListViewModel<M, E extends IViewModel<M>> extends ComplexViewModel<List<M>> {
	
	/**
	 * Interface encapsulte the list representation (eg. a DB-Cursor)
	 * 
	 * @author kenan
	 *
	 * @param <M> Model item which was removed/added/updated from/to the list
	 */
	public interface IModelListCallback<M>
	{
		void init(Class<?> parentClazz, Object parent, Class<M> childClazz);
		M get(int pos);
		int size();
		
		void commitFinished();
		void remove(M item);
		void save(Collection<M> items);
		
		List<M> getList();
	}
	
	protected ArrayList<M> deletedItems = new ArrayList<M>();
	protected ArrayList<M> newItems = new ArrayList<M>();
	private Class<M> modelClass;
	private Class<E> viewModelClass;
	private Constructor<E> vmConstructor;
	private IModelListCallback<M> listCB;
	
	private HashMap<Integer, E> positionToViewModel = new HashMap<Integer, E>();
	
	public EncapsulatedListViewModel(Class<M> modelClazz, Class<E> vmClazz, IModelListCallback<M> listCB)
	{
		super();
		this.listCB = listCB;
		this.modelClass = modelClazz;
		this.viewModelClass = vmClazz;
		
		if ( (null==modelClazz) || (null==vmClazz) || (null==listCB))
			throw new ArgumentException("No parameter of this constructor is allowed to be null");
		
		try {
			this.vmConstructor = viewModelClass.getConstructor(modelClass);
		}
		catch (NoSuchMethodException ex)
		{
			throw new ArgumentException("Unable to find a valid constructor for the model", ex);
		}
	}
	
	public void init(Object parent)
	{
		listCB.init(parent.getClass(), parent, this.modelClass);
	}

	public boolean add(M object) {
		boolean ret = newItems.add(object);
		this.notifyChange();
		
		return ret;
	}
	
	@Override
	public List<M> getModel() throws MVVMException 
	{
		return listCB.getList();
	}

	public void add(int location, M object) {
		newItems.add(location, object);
		this.notifyChange();
	}

	public boolean addAll(Collection<? extends M> arg0) {
		boolean ret = newItems.addAll(arg0);
		this.notifyChange();
		
		return ret;
	}

	public boolean addAll(int arg0, Collection<? extends M> arg1) {
		boolean ret = newItems.addAll(arg1);
		this.notifyChange();

		return ret;
	}

	public M get(int location) {
		return listCB.get(location);
	}

	public boolean isEmpty() {
		return ( (listCB.size()==0) && (this.newItems.size()==0) ); 
	}

	public boolean remove(M object) {
		boolean ret = deletedItems.add((M) object);
		notifyChange();
		
		return ret;
	}

	public boolean removeAll(Collection<M> arg0) {
		Iterator<M> it = arg0.iterator();
		boolean ret = false;
		
		while(it.hasNext())
		{
			M obj = it.next();
			if (!ret)
				ret = true;
			remove(obj);
		}
		
		notifyChange();
		return ret;
	}

	public int size() {
		return listCB.size();
	}

	@Override
	public boolean isDirty() {
		return ( (newItems.size()>0) || (deletedItems.size()>0) || super.isDirty());
	};
	
	@Override
	public void commit() {
		if (!this.isDirty())
			return;
		
		List<M> changedChildren = new ArrayList<M>();
		for(IViewModel<M> vm : positionToViewModel.values()) {
			if(vm.isDirty())
			{
				changedChildren.add(vm.getModel());
			}
		}
		super.commit();
				
		for (Iterator<M> iterator = deletedItems.iterator(); iterator.hasNext();) {
			M item = iterator.next();
			listCB.remove(item);
		}
		deletedItems.clear();

		newItems.addAll(changedChildren);
		listCB.save(newItems);
		newItems.clear();
		
		ViewModelCommitHelper.notifyCommit(this);
		listCB.commitFinished();
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

}
