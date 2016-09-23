package com.privatesecuredata.arch.testdata.vm;

import com.privatesecuredata.arch.mvvm.MVVM;
import com.privatesecuredata.arch.mvvm.annotations.VmProvider;
import com.privatesecuredata.arch.mvvm.vm.ComplexViewModel;
import com.privatesecuredata.arch.mvvm.vm.IListViewModel;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;
import com.privatesecuredata.arch.mvvm.vm.SimpleValueVM;
import com.privatesecuredata.arch.testdata.Company;
import com.privatesecuredata.arch.testdata.Person;

import java.util.HashMap;

public class CompanyVM extends ComplexViewModel<Company>
{
    @VmProvider(type = CompanyVM.class)
    public static class VmFactory implements ComplexViewModel.VmFactory<CompanyVM, Company> {
        @Override
        public CompanyVM create(MVVM mvvm, Company m) {
            return new CompanyVM(mvvm, m);
        }
    };
    
	private SimpleValueVM<String> nameVM;
	IListViewModel<Person, PersonVM> employeesVM;
	private ComplexViewModel<Person> bossVM;

	public CompanyVM(MVVM mvvm, Company model)
	{
        super(mvvm, model);
	}

    @Override
    protected void doMappings(HashMap<String, IViewModel<?>> childVMs) {
        nameVM = ((SimpleValueVM<String>) childVMs.get("name"));
        employeesVM = (IListViewModel<Person, PersonVM>) childVMs.get("employees");
        bossVM = ((ComplexViewModel<Person>) childVMs.get("boss"));
    }

    public String getName() { return nameVM.get(); }
	public void setName(String name) { this.nameVM.set(name); }
	
	public IListViewModel<Person, PersonVM> getEmployees() { return employeesVM; }

	public ComplexViewModel<Person> getBossVM() {
		return bossVM;
	}

}
