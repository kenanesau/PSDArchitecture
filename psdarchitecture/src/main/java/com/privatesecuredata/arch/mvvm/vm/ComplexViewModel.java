package com.privatesecuredata.arch.mvvm.vm;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Objects;
import com.privatesecuredata.arch.db.LazyCollectionInvocationHandler;
import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.exceptions.MVVMException;
import com.privatesecuredata.arch.mvvm.MVVM;
import com.privatesecuredata.arch.mvvm.annotations.ComplexVmMapping;
import com.privatesecuredata.arch.mvvm.annotations.ListVmMapping;
import com.privatesecuredata.arch.mvvm.annotations.SimpleVmMapping;

/**
 * @author kenan
 *
 * ViewModel-Class for all "complex" object-trees. This means it is a ViewModel which can have 
 * child-ViewModels.  
 */
public abstract class ComplexViewModel<MODEL> extends ViewModel<MODEL> {
	private HashMap<Integer, IViewModel<?>> children = new HashMap<Integer, IViewModel<?>>(); 
	private ArrayList<IViewModel<?>> childrenOrdered = new ArrayList<IViewModel<?>>();
	private SimpleValueVM<Boolean> selected = new SimpleValueVM<Boolean>(false);
    private ListViewModelFactory vmFactory = null;
	private Field modelField = null;
	private ComplexViewModel<?> parentViewModel = null;
    private List<IListViewModel> listVMs = new ArrayList<IListViewModel>();
    private HashMap<String, IViewModel<?>> nameViewModelMapping;
    private MVVM mvvm;
    private boolean globalNotify = true;

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
    }

    public ComplexViewModel(MVVM mvvm, MODEL model)
    {
        this(mvvm);

        //setModelAndRegisterChildren needs this.mvvm set...
        if (null != model)
            this.nameViewModelMapping = setModelAndRegisterChildren(model);
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
		load();
		return super.getModel();
	}

   	protected MODEL loadProxyData(MODEL model)
	{
		InvocationHandler handler = Proxy.getInvocationHandler(model);
		if (handler instanceof LazyCollectionInvocationHandler)
			model = (MODEL) new ArrayList(((LazyCollectionInvocationHandler)handler).loadCollection());
		
		return model;
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
                    /**
                     * delete _modelGetter -> all future calls of getModel() go directly to the
                     * real Model. DO THIS BEFORE SETMODELANDREGISTER -> StackOverflow otherwise
                     */
                    setModelGetter(null, null);

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
	 * This function is used by the functions set setComplexModelMapping() and  setListModelMapping()
	 * to enable lazy loading on the child model.
	 * 
	 * @param parentViewModel the parent view-model on which the modelGetter is invoked
	 * @param field the Field which accesses the (child)model (of the parentModel)
	 */
	protected void setModelGetter(ComplexViewModel<?> parentViewModel, Field field)
	{
        this.modelField = field;
        if (null != this.modelField)
            this.modelField.setAccessible(true);
		this.parentViewModel = parentViewModel;
	}

    protected Field getModelField() {
        return this.modelField;
    }

    protected Object getParentModel() {
        return ( (null != parentViewModel) ? getParentViewModel().getModel() : null);
    }

    public SimpleValueVM<Boolean> getSelectedVM() { return this.selected; }

	public boolean isSelected() { return this.selected.get(); }
	public void setSelected(boolean val) { this.selected.set(val); }

	private void addChild(IViewModel<?> vm) 
	{
		this.children.put(System.identityHashCode(vm), vm);
		this.childrenOrdered.add(vm);
        if (vm instanceof ComplexViewModel)
        {
            ComplexViewModel complexVM=(ComplexViewModel)vm;

            complexVM.vmFactory = this.vmFactory;
            complexVM.setHandle(getHandle());
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

    /**
     * This method analyses all the mapping-annotations and returns a Hashmap of all
     * Modelnames to their VMs. Overwrite doMappings to get the Hashmap with name->VM-mappings.
     *
     * @param model The model
     * @return Hashmap containing all mappings from model-name to view-model
     *
     * @see {@link #doMappings(HashMap<String, IViewModel>)}
     */
	private HashMap<String, IViewModel<?>> setModelAndRegisterChildren(MODEL model)
	{
		HashMap<String, IViewModel<?>> childViewModels = new HashMap<String, IViewModel<?>>();
        if (null != getModel())
            throw new ArgumentException("This Viewmodel already has a Model, don't do this twice!!!");
        if (null == model)
            throw new ArgumentException("Your model is null -- this does not make any sense!!!");

        setModel(model);

        for (IListViewModel listVM : this.listVMs)
            unregisterChildVM((ComplexViewModel)listVM);
        this.listVMs.clear();
        Field[] fields = model.getClass().getDeclaredFields();

        for (Field field : fields)
        {
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
                throw new ArgumentException(String.format("Could not find property with base-name \"%s\"", field.getName()), ex);
            }
            catch (InstantiationException ex) {
                throw new ArgumentException(String.format("Error instantiating complex viewmodel of field \"%s\"", field.getName()), ex);
            }
        }

        doMappings(childViewModels);
		return childViewModels;
	}
	
	protected Method createGetter(Field field) throws NoSuchMethodException
	{
		String propName = field.getName();
		propName = Character.toUpperCase(propName.charAt(0)) + propName.substring(1);
		String name = String.format("get%s", propName); 
		return getModel().getClass().getDeclaredMethod(name, (Class[])null);
	}
	
	protected Method createSetter(Field field, Class<?> valType) throws NoSuchMethodException
	{
		String propName = field.getName();
		propName = Character.toUpperCase(propName.charAt(0)) + propName.substring(1);
		String name = String.format("set%s", propName); 
		return getModel().getClass().getDeclaredMethod(name, new Class[]{valType});
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void setListModelMapping(HashMap<String, IViewModel<?>> childViewModels,
			Field field, ListVmMapping listAnno) throws IllegalArgumentException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException
	{

        MVVM mvvm = getMVVM();
        if (mvvm==null)
            throw new ArgumentException(String.format("ComplexViewModel of type \"%s\" without MVVM!!! Please create VMs only via MVVM.createVM().", this.getClass().getName()));
        IListViewModelFactory fac = mvvm.getListViewModelFactory();
        IListViewModel<?, ?> vmList = fac.createListVM(this, field, listAnno);
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
	
	protected void setComplexModelMapping(HashMap<String, IViewModel<?>> childModels,
			Field field, ComplexVmMapping complexAnno) throws IllegalArgumentException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        try {
            Class<?> modelType = field.getType();

            if (complexAnno.loadLazy() == false) {
                Class<?> viewModelType;
                Method childModelGetter = createGetter(field);
                Object childModel = childModelGetter.invoke(getModel(), (Object[]) null);

                if (childModel != null) {
                    modelType = childModel.getClass();
                    ComplexVmMapping anno = modelType.getAnnotation(ComplexVmMapping.class);
                    if (null == anno)
                        throw new ArgumentException(
                                String.format("Type \"%s\" does not have a ComplexVmMapping-Annotation. Please provide one!", modelType.getName()));
                    viewModelType = anno.viewModelClass();
                } else {
                    viewModelType = complexAnno.viewModelClass();
                }

                Constructor<?> complexVMConstructor = viewModelType.getConstructor(MVVM.class, modelType);
                ComplexViewModel<?> vm = (ComplexViewModel<?>) complexVMConstructor.newInstance(getMVVM(), childModel);
                registerChildVM(vm);

                childModels.put(field.getName(), vm);
            } else {
                // Do we need to register a VM here?? create a VM when it is needed!!!

                Class<?> viewModelType = complexAnno.viewModelClass();

                Constructor<?> complexVMConstructor = viewModelType.getConstructor(MVVM.class, modelType);
                ComplexViewModel<?> vm = (ComplexViewModel<?>) complexVMConstructor.newInstance(getMVVM(), null);
                vm.setModelGetter(this, field);

                childModels.put(field.getName(), vm);
                registerChildVM(vm); //This was missing!?!?!
            }
        }
        catch (NoSuchMethodException ex)
        {
            throw new MVVMException("Could not find default-constructor.", ex);
        }
        catch (Exception ex)
        {
            throw new MVVMException("Error setting up the model-getter", ex);
        }
	}

	protected void setSimpleModelMapping(HashMap<String, IViewModel<?>> childModels, 
			Field field, SimpleVmMapping simpleAnno) throws NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException 
	{
		Method getter = createGetter(field);
		Class<?> valType = getter.getReturnType();
		Method setter = createSetter(field, valType);
		
		SimpleValueVM<?> vm = new SimpleValueVM(valType, getter.invoke(getModel(), (Object[])null));
		vm.RegisterCommitCommand(getModel(), setter);
		registerChildVM(vm);

		childModels.put(field.getName(), vm);
	}

	/**
	 * Write all data of the complete object tree back to the model.
	 */
	@Override
	protected void commitData()
	{
		if (!this.isDirty())
			return;

        for(IViewModel<?> vm : children.values()) {
            if (vm instanceof IListViewModel)
                continue;

            vm.commit();
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

    @Override
    public void commit() {
        boolean wasDirty = this.isDirty();
        super.commit();
        if ( (wasDirty) && (isGlobalNotifyEnabled()) )
                getMVVM().notifyCommit(this);
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
		return Objects.toStringHelper(this)
				.add("isDirty", this.isDirty())
				.add("children", this.children.size())
				.toString();
	}

    public ComplexViewModel<?> getParentViewModel() { return this.parentViewModel; }
    protected void setParentViewModel(ComplexViewModel<?> parentViewModel) { this.parentViewModel = parentViewModel; }

    public int getHandle() {
        return _cfgHandle;
    }
    public void setHandle(int handle) { _cfgHandle = handle; }

    public void disableGlobalNotify() { this.globalNotify = false; }

    public void enableGlobalNotify() { this.globalNotify = true; }

    public boolean isGlobalNotifyEnabled() { return this.globalNotify; }
}
 