package com.privatesecuredata.arch.mvvm;

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
	private SimpleValueVM<T> vm;
	private boolean vmUpdatesView = false;
	
	public ViewToModelAdapter(Class<T> _dataType, IGetModelCommand<T> _getModelCmd) 
	{
		super(_dataType);
		setGetModelCommand(_getModelCmd);
	}
	
	public ViewToModelAdapter(TransientViewToModelAdapter<T> other)
	{
		super(other.dataType);
		
		this.setGetModelCommand(other.getGetModelCommand());
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
							return dataType.cast(((TextView) view).getText().toString());
						}
					};
					
					setReadViewCommand(readViewCmd);
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
					vm.set(ViewToModelAdapter.this.getReadViewCommand().get(ViewToModelAdapter.this.view));
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
		}
		if ( (this.view instanceof Checkable) && (this.view instanceof Button) ) 
		{
			Button chkView = (Button)this.view;
			chkView.setOnClickListener(null);
		}
	}
	
	public void init(View _view, IViewModel<?> complexVM)
	{
		if (null != this.view)
			unregisterListeners();
		this.view = _view;
		if (needsViewCommands())
			detectViewCommands(_view);
		
		//Most important two lines in the whole code: Register for VM-changes->View is updated automagically
		if (null != this.getGetModelCommand())
			this.setVM( this.getGetModelCommand().getVM(complexVM) );
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
			this.vm.delChangedListener(this);

		this.vm = _vm;
		this.vm.addChangedListener(this);
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
		this.setVM(this.getModelCmd.getVM(complexVM));
		this.writeViewCmd.set(v, this.getVM().get());		
	}

	/**
	 * A ViewModel changed and the corresponding View has to be updatet...
	 */
	@Override
	public void notifyChange(IViewModel<?> vm, IViewModel<?> originator) 
	{
		SimpleValueVM<T> simpleVM = getVM(); 
		setVMUpdatesView();
		writeViewCmd.set(this.view, simpleVM.get());
		resetVMUpdatesView();
	}	
}
