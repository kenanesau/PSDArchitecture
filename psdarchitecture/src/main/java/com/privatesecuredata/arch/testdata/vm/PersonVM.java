package com.privatesecuredata.arch.testdata.vm;

import com.privatesecuredata.arch.mvvm.ICommitCommand;
import com.privatesecuredata.arch.mvvm.MVVM;
import com.privatesecuredata.arch.mvvm.vm.ComplexViewModel;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;
import com.privatesecuredata.arch.mvvm.vm.ListViewModel;
import com.privatesecuredata.arch.mvvm.vm.SimpleValueVM;
import com.privatesecuredata.arch.testdata.Person;

import java.util.HashMap;

public class PersonVM extends ComplexViewModel<Person> {

    public static class VmFactory implements ComplexViewModel.VmFactory<PersonVM, Person>
    {
        @Override
        public PersonVM create(MVVM mvvm, Person m) {
            return new PersonVM(mvvm, m);
        }
    }

    SimpleValueVM<String> firstNameVM;
	SimpleValueVM<String> lastNameVM;
	SimpleValueVM<Long> ageVM;
	ListViewModel<Person, PersonVM> childrenVM;

	public PersonVM(MVVM mvvm, Person person) {
        super(mvvm, person);
	}

    @Override
    protected void doMappings(HashMap<String, IViewModel<?>> childVMs) {
        Person person = getModel();
        firstNameVM = new SimpleValueVM<String>(String.class, person.getFirstName());
        firstNameVM.RegisterCommitCommand(new ICommitCommand() {
            @Override
            public void commit() {getModel().setFirstName(firstNameVM.get());}
        });
        this.registerChildVM(firstNameVM);

        lastNameVM = new SimpleValueVM<String>(String.class, person.getLastName());
        lastNameVM.RegisterCommitCommand(new ICommitCommand() {
            @Override
            public void commit() {getModel().setLastName(lastNameVM.get());}
        });
        this.registerChildVM(lastNameVM);

        ageVM = new SimpleValueVM<Long>(Long.class, person.age());
        this.registerChildVM(ageVM);

        childrenVM = new ListViewModel<Person, PersonVM>(getMVVM(), Person.class, PersonVM.class);
        childrenVM.init(person.getChildren());
        this.registerChildVM(childrenVM);
    }

    public String getFirstName() { return this.firstNameVM.get(); }
	public void setFirstName(String name) { this.firstNameVM.set(name); }
	
	public String getLastName() { return this.lastNameVM.get(); }
	public void setLastName(String name) { this.lastNameVM.set(name); }
	
	public long getAge() { return this.ageVM.get(); }

	public void addChild(PersonVM child) 
	{
		this.childrenVM.add(child);
	}
	
	public void killChild(PersonVM child) {
		this.childrenVM.remove(child);
		this.setDirty();
		this.notifyViewModelDirty(this, this);
	}
	
	public PersonVM getChild(String firstName, String lastName)
	{
		PersonVM ret = null;
		for(PersonVM child : this.childrenVM) {
			if (child.getFirstName().equals(firstName) && child.getLastName().equals(lastName))
			{
				ret = child;
			}
		}
		
		return ret;
	}
	
	public int childCount() { return this.childrenVM.size(); }

    public SimpleValueVM<String> getFirstNameVM() {
        return firstNameVM;
    }

    public SimpleValueVM<String> getLastNameVM() {
        return lastNameVM;
    }

    public ListViewModel<Person, PersonVM> getChildrenVM() {
        return childrenVM;
    }
}
