package com.privatesecuredata.arch.mvvm.binder;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.exceptions.MVVMException;
import com.privatesecuredata.arch.mvvm.IGetVMCommand;
import com.privatesecuredata.arch.mvvm.IReadViewCommand;
import com.privatesecuredata.arch.mvvm.IViewModelChangedListener;
import com.privatesecuredata.arch.mvvm.IWriteViewCommand;
import com.privatesecuredata.arch.mvvm.vm.IModelChangedListener;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;
import com.privatesecuredata.arch.mvvm.vm.IWidgetValueAccessor;
import com.privatesecuredata.arch.mvvm.vm.IWidgetValueReceiver;
import com.privatesecuredata.arch.mvvm.vm.SimpleValueVM;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;

/**
 * Objects of this type can be used to establish a binding between a View and a corresponding
 * SimpleViewModel wich is usually part of a ComplexViewModel.
 *
 * @param <T> Datatype of the underlying SimpleValueVM
 */
public class ViewToVmBinder<T> extends TransientViewToVmBinder<T>
									implements IModelChangedListener, IViewModelChangedListener

{
	private boolean viewChanged;
	private View view;
	private TextWatcher txtWatch;
	private OnFocusChangeListener focusChangeListener;
	private OnAttachStateChangeListener attachStateChangedListener;
	private View.OnClickListener chkButtonListener;
    private IWidgetValueReceiver widgetValueReceiver;
	private SimpleValueVM<T> vm;
	private boolean vmUpdatesView = false;

    public ViewToVmBinder(Class<T> type, IGetVMCommand<T> getVMCommand, boolean readOnly)
    {
        this(type, getVMCommand);
        this.setReadOnly(readOnly);
    }

	public ViewToVmBinder(Class<T> type, IGetVMCommand<T> getVMCommand)
	{
		super(type);
		setGetVMCommand(getVMCommand);
	}
	
	public ViewToVmBinder(TransientViewToVmBinder<T> other)
	{
		super(other.dataType);
		
		this.setGetVMCommand(other.getGetVMCommand());
		this.setReadViewCommand(other.getReadViewCommand());
		this.setWriteViewCommand(other.getWriteViewCommand());
        this.setReadOnly(other.isReadOnly());
	}

	protected void setVMUpdatesView() { this.vmUpdatesView = true; }
	protected void resetVMUpdatesView() { this.vmUpdatesView = false; }
	protected boolean isVMUpdatesView() { return this.vmUpdatesView; }
	
	/**
	 * 
	 */
	protected void detectViewCommands(View view) 
	{
		if (view instanceof TextView) {
				
			if (needsWriteViewCommand())
			{
				if ((view instanceof CompoundButton) && (this.dataType.equals(Boolean.class)))
				{
					IWriteViewCommand<Boolean> writeCmpBtnCmd = new IWriteViewCommand<Boolean>() {
						@Override
						public void set(View view, Boolean val) {
							if (!isVMUpdatesView())
								((CompoundButton)view).setChecked(val);					
						}
					};
					
					setWriteViewCommand((IWriteViewCommand<T>) writeCmpBtnCmd);
				} 
				else
				{
					IWriteViewCommand<T> writeCmd = new IWriteViewCommand<T>() {
							@Override
							public void set(View view, T val) {
								if ( null!=val ) {
									TextView txtView = ((TextView)view);
									txtView.setText(val.toString());
								}
							}
						};
				
					setWriteViewCommand(writeCmd);
				}
			}
			
			if (needsReadViewCommand())
			{
				if ((view instanceof CompoundButton) && (this.dataType.equals(Boolean.class)))
				{
					IReadViewCommand<Boolean> readCmpBtnCmd = new IReadViewCommand<Boolean>() {
						@Override
						public Boolean get(View view) {
							return ((CompoundButton)view).isChecked();					
						}
					};
					
					setReadViewCommand((IReadViewCommand<T>) readCmpBtnCmd);
				}
				else
				{
					IReadViewCommand<T> readViewCmd = new IReadViewCommand<T>() {
						@Override
						public T get(View view) {
                            String strVal = ((TextView) view).getText().toString();

							try {
                                if (strVal!= null) {
                                    if (dataType == String.class) {
                                        return dataType.cast(strVal);
                                    }
                                    if (!strVal.isEmpty()) {
                                        if (dataType == Integer.class) {
                                            return dataType.cast(Integer.parseInt(strVal));
                                        }
                                        if (dataType == Long.class) {
                                            return dataType.cast(Long.parseLong(strVal));
                                        }
                                        if (dataType == Float.class) {
                                            return dataType.cast(Float.parseFloat(strVal));
                                        }
                                        if (dataType == Double.class) {
                                            return dataType.cast(Double.parseDouble(strVal));
                                        }
                                    }

                                    throw new MVVMException("Unable to convert view-value for the viewmodel! Datatype not supported!");
                                }
							}
							catch (Exception ex)
							{
								throw new MVVMException(String.format("Error converting string '%s'-value!", dataType.getSimpleName()), ex);
							}

							return null;
						}
					};
					
					setReadViewCommand(readViewCmd);
				}
			}
		} else {
            if (needsWriteViewCommand()) {

                if (view instanceof IWidgetValueAccessor) {
                    IWriteViewCommand<Object> writeCmd = new IWriteViewCommand<Object>() {
                        @Override
                        public void set(View view, Object val) {
                            if (null != val)
                                ((IWidgetValueAccessor) view).setValue(val);
                        }
                    };

                    setWriteViewCommand((IWriteViewCommand<T>) writeCmd);
                }
            }

            if (needsReadViewCommand())
            {
                if (view instanceof IWidgetValueAccessor)
                {
                    IReadViewCommand<Object> readCmd = new IReadViewCommand<Object>() {
                        @Override
                        public Object get(View view) {
                            return ((IWidgetValueAccessor)view).getValue();
                        }
                    };

                    setReadViewCommand((IReadViewCommand<T>) readCmd);

                    IWidgetValueAccessor accessor = (IWidgetValueAccessor)view;
                    IWidgetValueReceiver widgetValueReceiver =
                            new IWidgetValueReceiver() {
                                @Override
                                public void notifyWidgetChanged(IWidgetValueAccessor accessor) {
                                    if (!ViewToVmBinder.this.isVMUpdatesView())
									    viewValueToViewModel(ViewToVmBinder.this.view, vm);
                                }
                            };

                    accessor.registerValueChanged(widgetValueReceiver);
                }
            }
        }
		
		txtWatch = new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if ( !isVMUpdatesView() && canWriteToModel() )
				{
					viewValueToViewModel(ViewToVmBinder.this.view, vm);
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,	int after) {}
			
			@Override
			public void afterTextChanged(Editable s) {

			}
		};
		
		chkButtonListener = new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				ViewToVmBinder.this.viewChanged = true;
				if (canWriteToModel())
					viewValueToViewModel(v, vm);
			}
		};
		
		if (this.view instanceof EditText)
		{
			EditText txtView = (EditText)this.view; 
			txtView.addTextChangedListener(txtWatch);
		}
		
		if ( (this.view instanceof Checkable) && (this.view instanceof Button) ) 
		{
			Button chkView = (Button)this.view;
			chkView.setOnClickListener(chkButtonListener);
		}
	}

	private void viewValueToViewModel(View view, SimpleValueVM<T> vm) {
		try {
            T val = getReadViewCommand().get(view);
            if (null != val)
			    vm.set(val, this);
		}
		catch(MVVMException ex)
		{
			Log.e(getClass().getSimpleName(), "Unreadable view!!!!", ex);
			//Unreadable value -- for the moment do nothing
		}
		catch(ArgumentException ex)
		{
			/** Reset the View to the value of the VM **/
			writeViewCmd.set(view, vm.get());
		}
	}

	protected void unregisterListeners()
	{
		if (this.view instanceof EditText)
		{
			EditText txtView = (EditText)this.view; 
			txtView.removeTextChangedListener(txtWatch);
		} else if ( (this.view instanceof Checkable) && (this.view instanceof Button) ) {
			Button chkView = (Button)this.view;
			chkView.setOnClickListener(null);
		} else if (this.view instanceof IWidgetValueAccessor) {
            ((IWidgetValueAccessor)view).unregisterValueChanged(widgetValueReceiver);
        }
	}

    /**
     * Initialize the ViewToVmBinder.
     *
     * Here all the wiring for transfering the data beween VM and view is done.
     * @param view The view we want to bind to
     * @param complexVM The complexVM which contains the SimpleValueVM we want to bind
     */
	public void init(View view, IViewModel<?> complexVM)
	{
		if (null != this.view)
			unregisterListeners();
		this.view = view;
		if (needsViewCommands())
			detectViewCommands(view);
		
		//Most important two lines in the whole code: Register for VM-changes->View is updated automagically
		if (null != this.getGetVMCommand())
			this.setVM( this.getGetVMCommand().getVM(complexVM) );
	}

    /**
     * Reinitialize adapter when a new VM is set
     *
     * @param complexVM
     */
    public void reinit(IViewModel<?> complexVM)
    {
    	this.init(this.view, complexVM);
    }
	
	public boolean isViewChanged() {
		return viewChanged;
	}
	public void setViewChanged() {
		this.viewChanged = true;
	}
	public void resetViewChanged() {
		this.viewChanged = false;
	}
	
	public SimpleValueVM<T> getVM() { return vm; }
	public void setVM(SimpleValueVM<T> _vm) {
		if (null != this.vm)
			this.vm.delListeners(this, this);

		this.vm = _vm;

        if (null != this.vm)
		    this.vm.addListeners(this, this);
	}

	/**
	 * Fills the view with new data
	 * 
	 * @param complexVM The complex ViewModel which contains the data to transfer to the view
	 */
	public void updateView(IViewModel<?> complexVM)
	{
        if (complexVM.getModel() != null) {
            SimpleValueVM<T> simpleVM = this.getSimpleVMCmd.getVM(complexVM);
            if (simpleVM != this.vm)
                this.setVM(simpleVM);
            SimpleValueVM vm = this.getVM();

            if (null != vm)
                writeDataToView();
        }
	}

	private void writeDataToView() {
		setVMUpdatesView();
        Observable.just(this)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<ViewToVmBinder<T>>() {
                    @Override
                    public void accept(ViewToVmBinder<T> binder) throws Exception {
                        ViewToVmBinder.this.writeViewCmd.set(binder.view, binder.getVM().get());
                        resetVMUpdatesView();
                    }
                });
    }

    public void updateVM()
    {
        this.getVM().set(this.getReadViewCommand().get(this.view));
    }

	@Override
	public void notifyViewModelDirty(IViewModel<?> vm, IViewModelChangedListener originator)
	{
		writeDataToView();
	}

	protected Activity getActivity(View view) {
		Context context = view.getContext();
		while (context instanceof ContextWrapper) {
			if (context instanceof Activity) {
				return (Activity)context;
			}
			context = ((ContextWrapper)context).getBaseContext();
		}

		return null;
	}

	/**
     * A Model changed and the corresponding View has to be updated...
     */
    @Override
    public void notifyModelChanged(IViewModel<?> vm, IViewModel<?> originator) {
        SimpleValueVM<T> simpleVM = getVM();

		Activity activity = getActivity(view);

		if (null != activity) {
			activity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					setVMUpdatesView();
					writeViewCmd.set(ViewToVmBinder.this.view, simpleVM.get());
					resetVMUpdatesView();
				}
			});
		}
    }

    public void dispose()
    {
        unregisterListeners();
        if (null != this.vm)
            this.vm.delListeners(this, this);
    }
}
