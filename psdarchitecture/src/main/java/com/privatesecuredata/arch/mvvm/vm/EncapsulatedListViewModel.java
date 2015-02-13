package com.privatesecuredata.arch.mvvm.vm;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.mvvm.MVVM;

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
                                                                    implements IListViewModel<M, VM>
{
    /**
	 * Interface which encapsulates the list representation (eg. a DB-Cursor)
	 * 
	 * @author kenan
	 *
	 * @param <M> Model item which was removed/added/updated from/to the list
     * @see com.privatesecuredata.arch.db.CursorToListAdapter
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
	private Class<?> referencingType;
    private Class<M> referencedType;
	private Class<VM> vmType;
	private Constructor<VM> vmConstructor;
	private IModelListCallback<M> listCB;
    private boolean dataLoaded = false;
    private Method modelSetter;

	private HashMap<Integer, VM> positionToViewModel = new HashMap<Integer, VM>();

    /**
     *
     * @param referencingType Type of the class referencing the model (The foreign key in DB-terms)
     * @param referencedType
     * @param vmType
     * @param listCB
     */
	public EncapsulatedListViewModel(MVVM mvvm, Class<?> referencingType, Class<M> referencedType, Class<VM> vmType, IModelListCallback<M> listCB)
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
			throw new ArgumentException(String.format("Unable to find a valid constructor for the model of type \"%s\"", this.referencedType.getName()), ex);
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
        this.dataLoaded = false; //since we've go new data...
        setModelGetter(parentModel, null);
        load();
    }

    protected void setModelAccess(ComplexViewModel<?> model, Field modelField)
    {
        setModelGetter(model, modelField);
    }

    protected Method getModelSetter() { return this.modelSetter; }

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
            listCB.init(referencingType, getParentModel(), this.referencedType);
            dataLoaded = true;
            notifyModelChanged();
        }
    }

    public boolean add(VM vm)
    {
        this.registerChildVM(vm); //only registers if vm was not yet part of the parent-VM
        return this.add(vm.getModel());
    }

	public boolean add(M object) {
		boolean ret = newItems.add(object);
        notifyViewModelDirty();
		return ret;
	}

    /*@Override
	public IModelListCallback<M> getModel() throws MVVMException
	{
        load();
		return super.getModel();
	}*/

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

	public M get(int location) {
        load();
		return listCB.get(location);
	}

	public boolean isEmpty() {
		load();
        return ( (listCB.size()==0) && (this.newItems.size()==0) );
	}

	public boolean remove(M object) {
		boolean ret = deletedItems.add((M) object);
        notifyViewModelDirty();
		return ret;
	}

    public M remove(int location) {
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

	public boolean removeAll(Collection<?> arg0) {
		Iterator<?> it = arg0.iterator();
		boolean ret = false;
		
		while(it.hasNext())
		{
			M obj = (M)it.next();
			if (!ret)
				ret = true;
			remove(obj);
		}
		
		return ret;
	}

	public int size() {
		load();
        return listCB.size();
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
         * first commit() ist called -- here we have to save all changed child-VMs
         * later the DBViewModelCommitListener calls save() on this List-VM -- then those
         * VMs saved in the changedChildren-list are also saved to the DB...
         */
		for(IViewModel<M> vm : positionToViewModel.values()) {
			if(vm.isDirty())
			{
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

                if (vm instanceof ComplexViewModel) {
                    // Disable global notify since the children of a list are saved to DB when the
                    // whole list is saved
                    ((ComplexViewModel) vm).disableGlobalNotify();
                    vm.commit();
                    ((ComplexViewModel) vm).enableGlobalNotify();
                }
            }
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
			if (positionToViewModel.containsKey(pos))
				return positionToViewModel.get(pos);
			else
			{
				M model = get(pos);
				if (null != model) {
					vm = vmConstructor.newInstance(getMVVM(), model);
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
