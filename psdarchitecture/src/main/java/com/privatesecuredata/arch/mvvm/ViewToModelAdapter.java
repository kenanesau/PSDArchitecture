package com.privatesecuredata.arch.mvvm;

import com.privatesecuredata.arch.exceptions.MVVMException;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;
import com.privatesecuredata.arch.mvvm.vm.IWidgetValueAccessor;
import com.privatesecuredata.arch.mvvm.vm.IWidgetValueReceiver;
import com.privatesecuredata.arch.mvvm.vm.SimpleValueVM;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.Checkable;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

public class ViewToModelAdapter<T> extends TransientViewToModelAdapter<T> 
									implements IViewModelChangedListener
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
	
	public ViewToModelAdapter(Class<T> type, IGetVMCommand<T> getVMCommand)
	{
		super(type);
		setGetVMCommand(getVMCommand);
	}
	
	public ViewToModelAdapter(TransientViewToModelAdapter<T> other)
	{
		super(other.dataType);
		
		this.setGetVMCommand(other.getGetVMCommand());
		this.setReadViewCommand(other.getReadViewCommand());
		this.setWriteViewCommand(other.getWriteViewCommand());
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
								if ( !isVMUpdatesView() && (null!=val) )
									((TextView)view).setText(val.toString());						
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
							if ((null == strVal)  || strVal.isEmpty())
								throw new MVVMException("String value is null or empty");

							try {
								if (dataType==String.class)
								{
									return dataType.cast(strVal);
								}
								if (dataType==Integer.class)
								{
									return dataType.cast(Integer.parseInt(strVal));
								}
								if (dataType==Float.class)
								{
									return dataType.cast(Float.parseFloat(strVal));
								}
								if (dataType==Double.class)
								{
									return dataType.cast(Double.parseDouble(strVal));
								}

								return null;
							}
							catch (Exception ex)
							{
								throw new MVVMException(String.format("Error converting string '%s'-value!", dataType.getSimpleName()), ex);
							}
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
                            if (!isVMUpdatesView() && (null != val))
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
                                    vm.set(ViewToModelAdapter.this.getReadViewCommand().get(ViewToModelAdapter.this.view));
                                }
                            };

                    accessor.registerValueChanged(widgetValueReceiver);
                }
            }
        }
		
		txtWatch = new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,	int after) {}
			
			@Override
			public void afterTextChanged(Editable s) {
				if ( !isVMUpdatesView() && canWriteToModel() ) 
				{
					try {
						vm.set(ViewToModelAdapter.this.getReadViewCommand().get(ViewToModelAdapter.this.view));
					}
					catch(MVVMException ex)
					{
						//Unreadable value -- for the moment do nothing
					}
				}
			}
		};
		
//		focusChangeListener = new OnFocusChangeListener() 
//		{
//			
//			@Override
//			public void onFocusChange(View view, boolean hasFocus) {
//				SimpleValueVM<T> vm = ViewToModelAdapter.this.getVM();
//				
//				if ( (!hasFocus) && (isViewChanged()) )
//				{
//					if ( (canWriteToModel()) && (!isVMUpdatesView()) ) {
//						vm.set(ViewToModelAdapter.this.getReadViewCommand().get(view));
//						resetViewChanged();
//					}
//				}
//			}
//		};
		
		
		
//		attachStateChangedListener = new OnAttachStateChangeListener() {
//			
//			@Override
//			public void onViewDetachedFromWindow(View v) {
//				if (isViewChanged())
//				{
//					vm.set(ViewToModelAdapter.this.getReadViewCommand().get(v));
//					resetViewChanged();
//				}
//				if (v instanceof EditText)
//				{
//					((EditText) v).removeTextChangedListener(ViewToModelAdapter.this.txtWatch);
//				}
//				v.removeOnAttachStateChangeListener(ViewToModelAdapter.this.attachStateChangedListener);
//			}
//			
//			@Override
//			public void onViewAttachedToWindow(View v) {}
//		};
		
		chkButtonListener = new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				ViewToModelAdapter.this.viewChanged = true;
				if (canWriteToModel())
					vm.set(ViewToModelAdapter.this.getReadViewCommand().get(v));
			}
		};
		
		//this.view.setOnFocusChangeListener(focusChangeListener);
		//this.view.addOnAttachStateChangeListener(attachStateChangedListener);
		
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
	
	protected void unregisterListeners()
	{
		this.view.setOnFocusChangeListener(null);
		this.view.removeOnAttachStateChangeListener(attachStateChangedListener);
		
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
			this.vm.delViewModelListener(this);

		this.vm = _vm;

        if (null != this.vm)
		    this.vm.addViewModelListener(this);
	}

	/**
	 * Fills the view with new data
	 * 
	 * @param v View to write data to 
	 * @param complexVM The complex ViewModel which contains the data to transfer to the view
	 */
	@Override
	public void updateView(View v, IViewModel<?> complexVM) 
	{
		this.setVM(this.getSimpleVMCmd.getVM(complexVM));
        SimpleValueVM vm = this.getVM();

        if (null != vm)
		    this.writeViewCmd.set(v, this.getVM().get());
	}

	@Override
	public void notifyViewModelDirty(IViewModel<?> vm, IViewModel<?> originator)
	{
        this.writeViewCmd.set(this.view, this.getVM().get());
	}

    /**
     * A Model changed and the corresponding View has to be updatet...
     */
    @Override
    public void notifyModelChanged(IViewModel<?> vm, IViewModel<?> originator) {
        SimpleValueVM<T> simpleVM = getVM();
        setVMUpdatesView();
        writeViewCmd.set(this.view, simpleVM.get());
        resetVMUpdatesView();
    }
}
