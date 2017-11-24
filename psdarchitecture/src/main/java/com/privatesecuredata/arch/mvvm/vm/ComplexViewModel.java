package com.privatesecuredata.arch.mvvm.vm;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.CallSuper;

import com.google.common.base.MoreObjects;
import com.privatesecuredata.arch.db.IPersistable;
import com.privatesecuredata.arch.db.LazyCollectionInvocationHandler;
import com.privatesecuredata.arch.db.annotations.DbFactory;
import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.exceptions.MVVMException;
import com.privatesecuredata.arch.mvvm.IViewModelChangedListener;
import com.privatesecuredata.arch.mvvm.MVVM;
import com.privatesecuredata.arch.mvvm.annotations.ComplexVmMapping;
import com.privatesecuredata.arch.mvvm.annotations.ListVmMapping;
import com.privatesecuredata.arch.mvvm.annotations.SimpleVmMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * @author kenan
 *
 * ViewModel-Class for all "complex" object-trees. This means it is a ViewModel which can have 
 * child-ViewModels.  
 */
public abstract class ComplexViewModel<MODEL> extends ViewModel<MODEL> {
    public interface VmFactory<T, MODEL> {
        T create(MVVM mvvm, MODEL m);
    }

	private HashMap<Integer, IViewModel<?>> children = new HashMap<Integer, IViewModel<?>>();
    private ArrayList<IViewModel<?>> childrenOrdered = new ArrayList<IViewModel<?>>();
	private SimpleValueVM<Boolean> selected = new SimpleValueVM<Boolean>(false);
    private ArrayList<IListViewModel> listVMs = new ArrayList<>();
    private ListViewModelFactory vmFactory = null;
	private Field modelField = null;
	private ComplexViewModel<?> parentViewModel = null;
    private HashMap<String, IViewModel<?>> nameViewModelMapping;
    private MVVM mvvm;
    private boolean globalNotify = true;
    private boolean isLazy = false;

    /**
     * Handle for locating the lower level configuration reference when communicating with
     * MVVM...
     */
    private int _cfgHandle = -1;

    public ComplexViewModel()
	{
		super();
	}

    public ComplexViewModel(MVVM mvvm)
    {
        super();
        this.mvvm = mvvm;
        this.setHandle(mvvm.getHandle());
    }

    /**
     * Standard-Constructor for creating a VM-Tree for a model
     *
     * @param mvvm MVVM-object
     * @param model Model
     */
    public ComplexViewModel(MVVM mvvm, MODEL model)
    {
        this(mvvm);

        //setModelAndRegisterChildren needs this.mvvm set...
        //if (null != model)
        //    model = mvvm.createModel(); //first provide factory for creating models...
        if (null != model)
            this.nameViewModelMapping = setModelAndRegisterChildren(model);
    }

    public ComplexViewModel(MVVM mvvm, MODEL model, Class<MODEL> type, VmFactory<ComplexViewModel, MODEL> provider) {
        this(mvvm, model);

    }

/*
    public ComplexViewModel(ListViewModelFactory fac)
    {
        this();
        this.vmFactory = fac;
    }

	public ComplexViewModel(ComplexViewModel<?> parentViewModel, Method modelGetter)
	{
        this();
		setModelGetter(parentViewModel, modelGetter);
	}

    public ComplexViewModel(ListViewModelFactory fac, ComplexViewModel<?> parentViewModel, Method modelGetter)
    {
        this(fac);
        setModelGetter(parentViewModel, modelGetter);
    }*/

    @Override
	public MODEL getModel() throws MVVMException 
	{
        if (!hasModel())
		    load();
		return super.getModel();
	}

   	protected MODEL loadProxyData(MODEL m)
	{
		InvocationHandler handler = Proxy.getInvocationHandler(m);
		if (handler instanceof LazyCollectionInvocationHandler)
			m = (MODEL) new ArrayList(((LazyCollectionInvocationHandler)handler).loadCollection());
		
		return m;
	}

    public MVVM getMVVM()
    {
        return this.mvvm;
    }

    /**
     * doMappings() is called whenever setModelAndRegisterChildren() is called
     * (getModel() -> load() -> setModelAndRegisterChildren() -> doMappings())
     *
     * This method has to be overwritten if you want to support lazy loading and you want
     * to set/update your Model <-> VM-mappings after loading.
     *
     * If you don't want to support lazy loading just set your mappings in your VM-constructor.
     *
     * @param childVMs
     */
    protected void doMappings(HashMap<String, IViewModel<?>> childVMs) {}

    protected HashMap<String, IViewModel<?>> getNameViewModelMapping()
    {
        return this.nameViewModelMapping;
    }

	/**
	 * Use this function to load a complex VM which is not yet fully loaded (lazy loading activated/default)
	 */
	public void load()
	{
        /**
         * If there is as modelGetter, this means the model was not loaded yet
         * -> Load it and delete the modelgetter
         */
		if (null != getModelField()) {
			try {
                Object parentModel = getParentModel();
                if (null != parentModel) {
                    Field fld = getModelField();
                    MODEL model = (MODEL) fld.get(parentModel); //(MODEL) getModelGetter().invoke(getParentModel(), (Object[]) null);
                    if ((null != model) && (Proxy.isProxyClass(model.getClass()))) {
                        model = loadProxyData(model);
                    }

                    if (null != model)
                        setModelAndRegisterChildren(model);
                }
			}
			catch(Exception ex)
			{
				throw new MVVMException("Unable to load model (see inner exception for details)", ex);
			}
		}
	}

	/**
	 * This function is used by the functions setComplexModelMapping() and  setListModelMapping()
	 * to enable lazy loading on the child model.
	 * 
	 * @param parentViewModel the parent view-model on which the modelGetter is invoked
	 * @param field the Field which accesses the model of THIS Viewmodel in the Parent-Model
	 */
	protected void setModelGetter(ComplexViewModel<?> parentViewModel, Field field)
	{
        this.modelField = field;
        if (null != this.modelField)
            this.modelField.setAccessible(true);
		this.parentViewModel = parentViewModel;
	}

    /**
     * Return the field within the parent-model which contains the model for this VM.
     *
     * @return Field
     */
    public Field getModelField() {
        return this.modelField;
    }

    protected Object getParentModel() {
        return ( (null != parentViewModel) ? getParentViewModel().getModel() : null);
    }

    public SimpleValueVM<Boolean> getSelectedVM() { return this.selected; }

	public boolean isSelected() { return this.selected.get(); }
	public void setSelected(boolean val) { this.selected.set(val); }

    /**
     * Add vm as a child, adding this as a parent to vm
     * @param vm child-vm
     */
    protected void addChild(IViewModel<?> vm) {
        addChild(vm, true);
    }

    /**
     * Add vm as a child,
     * @param vm child-vm child-vm
     * @param useThisAsParent if true (default) us this as parent to vm
     *                        otherwise not
     */
	protected void addChild(IViewModel<?> vm, boolean useThisAsParent)
	{
		this.children.put(System.identityHashCode(vm), vm);
		this.childrenOrdered.add(vm);
        if (vm instanceof ComplexViewModel)
        {
            ComplexViewModel complexVM=(ComplexViewModel)vm;

            complexVM.vmFactory = this.vmFactory;
            complexVM.setHandle(getHandle());
            if (useThisAsParent)
                complexVM.setParentViewModel(this);
        }
	}
	
	private void delChild(IViewModel<?> vm) 
	{
		this.children.remove(System.identityHashCode(vm));
		this.childrenOrdered.remove(vm);
	}
	
	protected List<IViewModel<?>> getChildrenOrdered()
	{
		return this.childrenOrdered;			
	}

    public List<ComplexViewModel> getComplexChildren()
    {
        List<ComplexViewModel> ret = new ArrayList<ComplexViewModel>();

        for(IViewModel vm : childrenOrdered)
        {
            if (vm instanceof ComplexViewModel)
            {
                ret.add((ComplexViewModel)vm);
            }
        }

        return ret;
    }
	
	/**
	 * Register a new Child-VM. Registered children propagate their "changed"-information 
	 * to the parent
	 * 
	 * @param vm Child-Viewmodel
	 */
	public void registerChildVM(IViewModel<?> vm) 
	{
		if (!this.children.containsKey(System.identityHashCode(vm)))
		{
			addChild(vm);
			vm.addViewModelListener(this);
        }
		else 
			throw new ArgumentException(String.format("You are trying to register a Viewmodel twice. ViewModel \"%s\" was already registered", vm.toString()));
	}
	
	/**
	 * Unregister Child-VM again.
	 * 
	 * @param vm Child-Viewmodel
	 */
	public void unregisterChildVM(IViewModel<?> vm) 
	{
		delChild(vm);
		vm.delViewModelListener(this);
	}

	private Field[] getAllFields(MODEL m) {
        Class cls = m.getClass();
        Field[] fields = cls.getDeclaredFields();

        ArrayList<Field> tmpFields = new ArrayList<Field>();
        for (Field fld : fields)
            tmpFields.add(fld);

        while( !cls.getSuperclass().equals(Object.class))
        {
            cls = cls.getSuperclass();
            fields = cls.getDeclaredFields();
            for (Field fld : fields)
                tmpFields.add(fld);
        }

        fields = new Field[tmpFields.size()];
        tmpFields.toArray(fields);

        return fields;
    }

    /**
     * This method analyses all the mapping-annotations and returns a Hashmap of all
     * Modelnames to their VMs. Overwrite doMappings to get the Hashmap with name->VM-mappings.
     *
     * @param m The model
     * @return Hashmap containing all mappings from model-name to view-model
     *
     * @see {@link #doMappings(HashMap<String, IViewModel>)}
     */
    private HashMap<String, IViewModel<?>> setModelAndRegisterChildren(MODEL m)
	{
		HashMap<String, IViewModel<?>> childViewModels = new HashMap<String, IViewModel<?>>();
        if (this.hasModel())
            throw new ArgumentException("This Viewmodel already has a Model, don't do this twice!!!");
        /* if (null == m)
            throw new ArgumentException("Your model is null -- this does not make any sense!!!"); */

        setModel(m);

        for (IListViewModel listVM : this.listVMs)
            unregisterChildVM((ComplexViewModel)listVM);
        if (this.children.size() > 0) {
            ArrayList<IViewModel<?>> vms = new ArrayList<>(this.children.values());
            for (IViewModel vm : vms) {
                unregisterChildVM(vm);
            }
            vms.clear();
        }
        Field[] fields = getAllFields(m);

        for (Field field : fields)
        {
            field.setAccessible(true);
            try {
                SimpleVmMapping simpleAnno = field.getAnnotation(SimpleVmMapping.class);
                if (null != simpleAnno)
                    setSimpleModelMapping(childViewModels, field, simpleAnno);
                else
                {
                    ComplexVmMapping complexAnno = field.getAnnotation(ComplexVmMapping.class);
                    if (null != complexAnno) {
                        setComplexModelMapping(childViewModels, field, complexAnno);
                    }
                    else {
                        ListVmMapping listAnno = field.getAnnotation(ListVmMapping.class);
                        if (null != listAnno)
                            setListModelMapping(childViewModels, field, listAnno);
                    }
                }
            }
            catch (IllegalArgumentException ex) {
                throw new ArgumentException("Wrong argument!", ex);
            }
            catch (IllegalAccessException ex) {
                throw new ArgumentException("Error accessing method!", ex);
            }
            catch (InvocationTargetException ex) {
                throw new ArgumentException("Error invoking method!", ex);
            }
            catch (NoSuchMethodException ex) {
                throw new ArgumentException(String.format("Could not find field with name \"%s\"", field.getName()), ex);
            }
            catch (InstantiationException ex) {
                throw new ArgumentException(String.format("Error instantiating complex viewmodel of field \"%s\"", field.getName()), ex);
            }
        }

        if (m instanceof IPersistable)
        {
            if (((IPersistable) m).getDbId() == null)
                setDirty();
        }
        try {
            doMappings(childViewModels);
        }
        catch (Exception ex) {
            throw new MVVMException(String.format("Error doing the mappings for VM of type '%s'", getClass().getName()), ex);
        }
		return childViewModels;
	}

    private void replaceChildModels(Object m) {
        setModel((MODEL)m);
        Field[] fields = getAllFields((MODEL)m);

        for (Field field : fields) {
            field.setAccessible(true);
            try {
                SimpleVmMapping simpleAnno = field.getAnnotation(SimpleVmMapping.class);
                if (null != simpleAnno)
                    setNewModelSimple(field, simpleAnno);
                else {
                    ComplexVmMapping complexAnno = field.getAnnotation(ComplexVmMapping.class);
                    if (null != complexAnno) {
                        setNewModelComplex(field, complexAnno);
                    } else {
                        ListVmMapping listAnno = field.getAnnotation(ListVmMapping.class);
                        if (null != listAnno)
                            setNewModelList(field, listAnno);
                    }
                }
            } catch (IllegalArgumentException ex) {
                throw new ArgumentException("Wrong argument!", ex);
            } catch (IllegalAccessException ex) {
                throw new ArgumentException("Error accessing method!", ex);
            } catch (InvocationTargetException ex) {
                throw new ArgumentException("Error invoking method!", ex);
            } catch (NoSuchMethodException ex) {
                throw new ArgumentException(String.format("Could not find field with name \"%s\"", field.getName()), ex);
            } catch (InstantiationException ex) {
                throw new ArgumentException(String.format("Error instantiating complex viewmodel of field \"%s\"", field.getName()), ex);
            }
        }
    }
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void setListModelMapping(HashMap<String, IViewModel<?>> childViewModels,
			Field field, ListVmMapping listAnno) throws IllegalArgumentException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException
	{

        MVVM mvvm = getMVVM();
        if (mvvm==null)
            throw new ArgumentException(String.format("ComplexViewModel of type \"%s\" without MVVM!!! Please create VMs only via MVVM.createVM().", this.getClass().getName()));
        IListViewModelFactory fac = mvvm.getListViewModelFactory();
        IListViewModel<?> vmList = fac.createListVM(this, field, listAnno);
        this.listVMs.add(vmList);
        ComplexViewModel<?> vm = (ComplexViewModel<?>)vmList;

		if (listAnno.loadLazy()==false) {
			vmList.init(this, field);
		}
		else
			vm.setModelGetter(this, field);
		registerChildVM(vm);
		
		childViewModels.put(field.getName(), vm);
	}

    protected void setNewModelList(Field field, ListVmMapping listAnno) throws IllegalArgumentException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException
    {
        IListViewModel<?> vmList = (IListViewModel<?>)nameViewModelMapping.get(field.getName());

        if (listAnno.loadLazy()==false) {
            vmList.init(this, field);
        }
        else
            ((ComplexViewModel<?>)vmList).setModelGetter(this, field);
    }


    protected void setComplexModelMapping(HashMap<String, IViewModel<?>> childModels,
			Field field, ComplexVmMapping complexAnno) throws IllegalArgumentException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        try {
            Class<?> modelType = field.getType();
            /**
             * This VM has a model otherwise this method would not be called
             **/
            Object childModel = field.get(getModel());
            if (null == childModel) {
                //TODO: childModel = MVVM.createMVVM().createModel(modelType);
                throw new ArgumentException(String.format("The childmodel in type.field '%s.%s' is null!!",
                        modelType.getName(), field.getName()));
            }
            /**
             * If the Childmodel is != null -> it is in memory -> create the VM
             */
            if (!complexAnno.loadLazy() || childModel != null)
            {
                unsetLazy();
                if (childModel != null) {
                    modelType = childModel.getClass();
                    ComplexVmMapping anno = modelType.getAnnotation(ComplexVmMapping.class);
                    if (null == anno)
                        throw new ArgumentException(
                                String.format("Type \"%s\" does not have a ComplexVmMapping-Annotation. Please provide one!", modelType.getName()));
                }
                ComplexViewModel<?> vm = mvvm.createVM(modelType, childModel);

                //We might need this in case the model is replaced later by a new model
                vm.setModelGetter(this, field);
                registerChildVM(vm);

                childModels.put(field.getName(), vm);
            } else {
                // Do we need to register a VM here?? create a VM when it is needed!!!
                ComplexViewModel<?> vm = mvvm.createVM(modelType, null);
                vm.setLazy();
                vm.setModelGetter(this, field);

                childModels.put(field.getName(), vm);
                registerChildVM(vm); //This was missing!?!?!
            }
        }
        catch (Exception ex)
        {
            throw new MVVMException("Error setting up the model-getter", ex);
        }
	}

	protected void setNewModelComplex(Field field, ComplexVmMapping complexAnno) throws IllegalArgumentException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        Class<?> modelType = field.getType();
        Object childModel = field.get(getModel());
        if (null == childModel) {
            //TODO: childModel = MVVM.createMVVM().createModel(modelType);
            throw new ArgumentException(String.format("The childmodel in type.field '%s.%s' is null!!",
                    modelType.getName(), field.getName()));
        }
        /**
         * If the Childmodel is != null -> it is in memory -> create the VM
         */
        ComplexViewModel<?> vm = (ComplexViewModel<?>)nameViewModelMapping.get(field.getName());
        if (!complexAnno.loadLazy() || childModel != null) {
            unsetLazy();
            if (childModel != null) {
                modelType = childModel.getClass();
                ComplexVmMapping anno = modelType.getAnnotation(ComplexVmMapping.class);
                if (null == anno)
                    throw new ArgumentException(
                            String.format("Type \"%s\" does not have a ComplexVmMapping-Annotation. Please provide one!", modelType.getName()));
            }

            vm.replaceChildModels(childModel);
            vm.setModelGetter(this, field);
        } else {
            // Do we need to register a VM here?? create a VM when it is needed!!!
            vm.setModel(null);
            vm.setLazy();
            vm.setModelGetter(this, field);
        }
    }

    protected void setLazy() {
        this.isLazy = true;
    }

    protected void unsetLazy() {
        this.isLazy = false;
    }

    protected boolean isLazy() {
        return  isLazy;
    }


    protected void setSimpleModelMapping(HashMap<String, IViewModel<?>> childModels,
			Field field, SimpleVmMapping simpleAnno) throws NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException 
	{
		//Method getter = createGetter(field);
		Class<?> valType = field.getType(); //getter.getReturnType();
		//Method setter = createSetter(field, valType);
		
		SimpleValueVM<?> vm = new SimpleValueVM(valType, field.get(getModel()));
		vm.RegisterCommitCommand(getModel(), field);
		registerChildVM(vm);

		childModels.put(field.getName(), vm);
	}

    protected void setNewModelSimple(Field field, SimpleVmMapping simpleAnno) throws NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException
    {
        SimpleValueVM<?> vm = (SimpleValueVM<?>)nameViewModelMapping.get(field.getName());
        vm.setObject(field.get(getModel()));
    }

	/**
	 * Write all data of the complete object tree back to the model.
	 */
	@Override
	protected void commitData()
	{
		if (!this.isDirty())
			return;

        List<IListViewModel> localListVMs = new ArrayList<IListViewModel>();
        for(IViewModel<?> vm : children.values()) {
            if (vm instanceof IListViewModel) {
                localListVMs.add((IListViewModel) vm);
                continue;
            }

            vm.commit();
		}

        /**
         * Commit the lists AFTER everything else since there could be a foreign key relation
         * in the DB -> Ensure that the rest of the parent-model is already clean.
         */
        for(IListViewModel listVM : localListVMs) {
            listVM.commit();
        }

        Field fld = this.getModelField();
        try {
            if (null != fld)
                fld.set(getParentModel(), getModel());
        }
        catch (Exception ex)
        {}

		this.setClean();
	}

	@CallSuper
	protected void startCommit() {}

    @Override
    public void commit() {

        startCommit();
        boolean wasDirty = this.isDirty();
        if (wasDirty) {
            this.commitData();

            if (isGlobalNotifyEnabled()) {
                MVVM mvvm = getMVVM();
                /** MVVM can be null if we have a VM which created otherwise
                 * (e.g. CalculateReach-Dialog).
                 */
                if (null != mvvm)
                    mvvm.notifyCommit(this);
            }

            finishCommit();
        }
    }

	/**
	 * (re)load all data from the Model to the ViewModel
	 */
	public void reload() 
	{
		for(IViewModel<?> vm : children.values()) {
			vm.reload();
		}
		
		this.setClean();
	}

	@Override
	public int hashCode() {
		int hashCode = 1; 

		Iterator<IViewModel<?>> it = children.values().iterator();
		while (it.hasNext()) {
		    Object obj = it.next();
		    hashCode = 31*hashCode + (obj==null ? 0 : obj.hashCode());
		}
		
		return hashCode; 
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o instanceof ComplexViewModel<?>)
		{
			ComplexViewModel<?> complexVM = (ComplexViewModel<?>)o;
			if (this.children.size() != complexVM.children.size())
				return false;
			
			//-> We know both collections are of same size...
			if ( (this.children.size() >= 0) && (complexVM.children.size()>=0))
			{
				Iterator<IViewModel<?>> thisChildrenIt = getChildrenOrdered().iterator();
				Iterator<IViewModel<?>> thatChildrenIt = complexVM.getChildrenOrdered().iterator();
				
				while (thisChildrenIt.hasNext())
				{
					IViewModel<?> thisChild = thisChildrenIt.next();
					IViewModel<?> thatChild = thatChildrenIt.next();
					if (!thisChild.equals(thatChild))
						return false;
				}
				return true;
			}
			else
				return false;
			
		}
		else {
			return false;
		}
	}

    public List<IListViewModel> getListVMs() {
        if (this.listVMs.size()==0)
            return null;
        else
            return new ArrayList<IListViewModel>(this.listVMs);
    }

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("isDirty", this.isDirty())
				.add("children", this.children != null ? this.children.size() : "0")
				.toString();
	}

    public <T extends ComplexViewModel> T getParentViewModel() { return (T)this.parentViewModel; }
    protected void setParentViewModel(ComplexViewModel parentViewModel) { this.parentViewModel = parentViewModel; }

    public int getHandle() {
        return _cfgHandle;
    }
    public void setHandle(int handle) { _cfgHandle = handle; }

    public void disableGlobalNotify() {
        this.globalNotify = false;
        for (IViewModel childVM : children.values()) {
            if (childVM instanceof ComplexViewModel) {
                ((ComplexViewModel) childVM).disableGlobalNotify();
            }
        }
    }

    public void enableGlobalNotify() {
        this.globalNotify = true;
        for (IViewModel childVM : children.values()) {
            if (childVM instanceof ComplexViewModel) {
                ((ComplexViewModel) childVM).enableGlobalNotify();
            }
        }
    }

    /**
     * Write a new Model to the VM and replace all models of the subsequent VM-tree
     *
     * @param m
     */
    public void replaceModel(MODEL m)
    {
        setModel(null);
        unsetLazy();
        replaceChildModels(m);
        notifyComplexChildModels();
    }

    protected void notifyComplexChildModels() {
        List<ComplexViewModel> lst = getComplexChildren();

        for(ComplexViewModel vm : lst) {
            vm.notifyModelChanged();
            vm.notifyComplexChildModels();
        }
    }

    /**
     * Call this function to Replace a child-VM in a Complex VM
     *
     * Usually you use this function within a setter of a parent VM to replace one of its
     * child VMs
     *
     * @param oldChildVM old child VM -- which needs to be replaced
     * @param newChildViewModel new "value" for child-VM
     * @param <T> Model-Type of the child VMs
     * @return Returns the new child-VM
     */
    public <T extends ComplexViewModel> T replaceVM(T oldChildVM, T newChildViewModel) {
        if (null == newChildViewModel)
            throw new ArgumentException("Parameter 'newChildViewModel' must not be null!");
        if (null != oldChildVM) {
            unregisterChildVM(oldChildVM);

            Field fld = oldChildVM.getModelField();
            Collection<IModelChangedListener> modelListeners = oldChildVM.getModelListeners();
            for (IModelChangedListener listener : modelListeners) {
                oldChildVM.delModelListener(listener);
                newChildViewModel.addModelListener(listener);
            }

            Collection<IViewModelChangedListener> vmListeners = oldChildVM.getViewModelListeners();
            for (IViewModelChangedListener listener : vmListeners) {
                oldChildVM.delViewModelListener(listener);
                newChildViewModel.addViewModelListener(listener);
            }

            if (null == fld)
                throw new ArgumentException("Field for setting own value in parent is null!!");

            /**
             * Without this it would not be possible for the child-VM to write it's new value
             * to the parent-VM.
             */
            newChildViewModel.setModelGetter(this, oldChildVM.getModelField());
        }
        else throw new ArgumentException("Parameter 'oldChildVM' must not be null!");

        registerChildVM(newChildViewModel);
        newChildViewModel.notifyViewModelDirty();

        return newChildViewModel;
    }


    public boolean isGlobalNotifyEnabled() { return this.globalNotify; }

    public Resources getResources() { return this.mvvm.getResources();}
    public Context getContext() { return this.mvvm.getContext();}

    public void dispose() {
        List<IViewModel<?>> lst = getChildrenOrdered();
        for (IViewModel vm : lst)
        {
            vm.dispose();
        }
    };
}
 