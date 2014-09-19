package com.privatesecuredata.arch.mvvm;

import android.view.View;
import android.widget.TextView;

/**
 * 
 * @author kenan
 * 
 * Objects of this class contain the commands for transferring data between the View and the 
 * ViewModel. This is the Base class for the ViewToModeladapters. This class only saves the 
 * Commands. Neither View nor ViewModel are saved within this class. 
 * 
 * Usually this class is only used by the user to define the mappings between the View-Ids and
 * the ViewModels. When updateView is called with a View and a ViewModel as its parameters all 
 * data is transfered from ViewModel to View.1   
 *
 * @param <T> (primitive) Datatype which is transfered between the View and the ViewModel
 * 
 * @see ViewToModelAdapter
 */
public class TransientViewToModelAdapter<T> {
	public TransientViewToModelAdapter(Class<T> _dataType)
	{
		this.dataType=_dataType;
	}
	
	protected Class<T> dataType;
	protected IReadViewCommand<T> readViewCmd;
	protected IWriteViewCommand<T> writeViewCmd;
	
	protected IGetModelCommand<T> getModelCmd;
	
	public void setGetModelCommand(IGetModelCommand<T> cmd) { this.getModelCmd = cmd; }
	public IGetModelCommand<T> getGetModelCommand() { return this.getModelCmd; }
	public void setReadViewCommand(IReadViewCommand<T> cmd) { this.readViewCmd = cmd; }
	public void setWriteViewCommand(IWriteViewCommand<T> cmd) { this.writeViewCmd = cmd; }
	public IReadViewCommand<T> getReadViewCommand() { return this.readViewCmd; }
	public IWriteViewCommand<T> getWriteViewCommand() { return this.writeViewCmd; }
	
	/**
	 * Set the commands for reading and writing the View
	 * 
	 * @param _readViewCmd
	 * @param _writeViewCmd
	 */
	public void setViewCommands(IReadViewCommand<T> _readViewCmd, IWriteViewCommand<T> _writeViewCmd)
	{
		setReadViewCommand(_readViewCmd);
		setWriteViewCommand(_writeViewCmd);
	}
	
	public void setCommands(IReadViewCommand<T> rdViewCmd, IWriteViewCommand<T> wrViewCmd,
							IGetModelCommand<T> getModelCmd)
	{
		this.setViewCommands(rdViewCmd, wrViewCmd);
		this.setGetModelCommand(getModelCmd);
	}
	
	/**
	 * Fills the view with new data provided by the model (vm)
	 * 
	 * @param v View to write data to 
	 * @param vm The complex ViewModel which contains the data to transfer to the view
	 */
	public void updateView(View v, IModel<?> vm)
	{
		this.writeViewCmd.set(v, this.getModelCmd.getVM(vm).get());
	}
	
	/**
	 * @return true if the writeViewCmd is NOT set
	 */
	protected boolean needsWriteViewCommand()
	{
		if ( (null != getModelCmd) && (null == writeViewCmd) )
			return true;
		else
			return false; 
	}
	
	/**
	 * @return true if the readViewCmd is NOT set
	 */
	protected boolean needsReadViewCommand()
	{
		if ( (null != getModelCmd) && (null == readViewCmd))
			return true;
		else
			return false;
	}
	
	protected boolean canWriteToModel() 
	{
		if ( null != getGetModelCommand() )
			return true;
		else
			return false;
	}
	
	/**
	 * @return true if a model is set but no commands for reading and writing the view 
	 */
	protected boolean needsViewCommands() {
		if (needsWriteViewCommand())
			return true;
		if (needsReadViewCommand())
			return true;
		return false;
	}
	
	/**
	 * Get a non-transient copy of this TransientViewToModelAdapter (Remember to set the View and the ViewModel in that copy)
	 */
	public ViewToModelAdapter<T> clone()
	{
		return new ViewToModelAdapter<T>(this);
	}
}
