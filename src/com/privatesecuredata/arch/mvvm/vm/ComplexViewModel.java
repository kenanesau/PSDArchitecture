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
import com.privatesecuredata.arch.db.ILoadCollection;
import com.privatesecuredata.arch.db.LazyCollectionInvocationHandler;
import com.privatesecuredata.arch.db.LazyLoadCollectionProxy;
import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.exceptions.MVVMException;
import com.privatesecuredata.arch.mvvm.IViewModel;
import com.privatesecuredata.arch.mvvm.ViewModelCommitHelper;
import com.privatesecuredata.arch.mvvm.annotations.ComplexVmMapping;
import com.privatesecuredata.arch.mvvm.annotations.ListVmMapping;
import com.privatesecuredata.arch.mvvm.annotations.SimpleVmMapping;

/**
 * @author kenan
 *
 * ViewModel-Class for all "complex" object-trees. This means it is a ViewModel which can have 
 * child-ViewModels.  
 */
public class ComplexViewModel<MODEL> extends ViewModel<MODEL> {
	private HashMap<Integer, IViewModel<?>> children = new HashMap<Integer, IViewModel<?>>(); 
	private ArrayList<IViewModel<?>> childrenOrdered = new ArrayList<IViewModel<?>>();
	private SimpleValueVM<Boolean> selected = new SimpleValueVM<Boolean>(false);
	protected Method modelGetter = null;
	protected Object parentModel = null;
	
	public ComplexViewModel()
	{
		super();
	}
	
	public ComplexViewModel(Object parentModel, Method modelGetter)
	{
		setModelGetter(parentModel, modelGetter);
	}
	
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
	
	/**
	 * Use this function to load a complex VM which is not yet fully loaded (lazy loading activated/default)
	 */
	public void load()
	{
		if (null != modelGetter) {
			try {
				MODEL model = (MODEL)modelGetter.invoke(parentModel, (Object[])null);
				if ( ( null != model ) && ( Proxy.isProxyClass(model.getClass()) ) ) {
					model  = loadProxyData(model);
				}
				setModel(model);
				setModelGetter(null, null);
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
	 * @param parentModel the parent model on which the modelGetter is invoked
	 * @param modelGetter the getter-Method which return the (child)model (of the parentModel) 
	 */
	protected void setModelGetter(Object parentModel, Method modelGetter)
	{
		this.modelGetter = modelGetter;
		this.parentModel = parentModel;
	}
	
	public SimpleValueVM<Boolean> getSelectedVM() { return this.selected; }

	public boolean isSelected() { return this.selected.get(); }
	public void setSelected(boolean val) { this.selected.set(val); }

	private void addChild(IViewModel<?> vm) 
	{
		this.children.put(System.identityHashCode(vm), vm);
		this.childrenOrdered.add(vm);
	}
	
	private void delChild(IViewModel<?> vm) 
	{
		this.children.remove(System.identityHashCode(vm));
		this.childrenOrdered.remove(vm);
	}
	
	private List<IViewModel<?>> getChildrenOrdered()
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
			vm.addChangedListener(this);
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
		vm.delChangedListener(this);
	}
	
	public HashMap<String, IViewModel<?>> setModelAndRegisterChildren(MODEL model)
	{
		HashMap<String, IViewModel<?>> childModels = new HashMap<String, IViewModel<?>>();
		setModel(model);

		if (null != model)
		{
			Field[] fields = model.getClass().getDeclaredFields();
			
			for (Field field : fields)
			{
				try {
					SimpleVmMapping simpleAnno = field.getAnnotation(SimpleVmMapping.class);
					if (null != simpleAnno)
						setSimpleModelMapping(childModels, field, simpleAnno);
					else 
					{
						ComplexVmMapping complexAnno = field.getAnnotation(ComplexVmMapping.class);
						if (null != complexAnno)
							setComplexModelMapping(childModels, field, complexAnno);
						else
						{
							ListVmMapping listAnno = field.getAnnotation(ListVmMapping.class);
							if (null != listAnno)
								setListModelMapping(childModels, field, listAnno);
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
		}
				
		return childModels;
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
	protected void setListModelMapping(HashMap<String, IViewModel<?>> childModels,
			Field field, ListVmMapping listAnno) throws IllegalArgumentException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException 
	{
		Class<?> viewModelType = listAnno.vmClass();
		Class<?> modelType = listAnno.modelClass();
		Method childModelGetter = createGetter(field); 
		FastListViewModel vm = new FastListViewModel(modelType, viewModelType);
		if (listAnno.loadLazy()==false) {
			List<?> childModel = (List<?>) childModelGetter.invoke(getModel(), (Object[])null);
			vm.init(childModel);
		}
		else
			vm.setModelGetter(getModel(), childModelGetter);
		registerChildVM(vm);
		
		childModels.put(field.getName(), vm);
	}
	
	protected void setComplexModelMapping(HashMap<String, IViewModel<?>> childModels,
			Field field, ComplexVmMapping complexAnno) throws IllegalArgumentException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException 
	{
		if (complexAnno.loadLazy()==false) {
			Class<?> viewModelType = complexAnno.viewModelClass();
			Method childModelGetter = createGetter(field); 
			Object childModel = childModelGetter.invoke(getModel(), (Object[])null);
			Class<?> modelType = field.getType();
			Constructor<?> complexVMConstructor = viewModelType.getConstructor(modelType);
			ComplexViewModel<?> vm = (ComplexViewModel<?>) complexVMConstructor.newInstance(childModel);
			registerChildVM(vm);
			
			childModels.put(field.getName(), vm);
		}
		else {
			try {
				Class<?> viewModelType = complexAnno.viewModelClass();
				Method childModelGetter = createGetter(field); 
				
				Constructor<?> complexVMConstructor = viewModelType.getConstructor();
				ComplexViewModel<?> vm = (ComplexViewModel<?>) complexVMConstructor.newInstance();
				vm.setModelGetter(getModel(), childModelGetter);
				
				childModels.put(field.getName(), vm);
			}
			catch (NoSuchMethodException ex)
			{
				throw new MVVMException("Could not find default-constructor. When you are using lazy initialization you have to provide a default constructor!", ex);
			}
			catch (Exception ex)
			{
				throw new MVVMException("Error setting up the model-getter", ex);
			}
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
			vm.commit();
		}
		
		this.setClean();
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

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("isDirty", this.isDirty())
				.add("children", this.children.size())
				.toString();
	}
}
 