package com.privatesecuredata.arch.testdata;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.privatesecuredata.arch.db.DbId;
import com.privatesecuredata.arch.db.IPersistable;
import com.privatesecuredata.arch.db.IPersistableFactory;
import com.privatesecuredata.arch.db.annotations.DbFactory;
import com.privatesecuredata.arch.db.annotations.DbField;
import com.privatesecuredata.arch.db.annotations.DbThisToMany;
import com.privatesecuredata.arch.db.annotations.DbThisToOne;
import com.privatesecuredata.arch.mvvm.annotations.ComplexVmMapping;
import com.privatesecuredata.arch.mvvm.annotations.ListVmMapping;
import com.privatesecuredata.arch.mvvm.annotations.SimpleVmMapping;
import com.privatesecuredata.arch.testdata.vm.CompanyVM;
import com.privatesecuredata.arch.testdata.vm.PersonVM;

import java.util.ArrayList;
import java.util.List;

@DbFactory(factoryType = Company.DbFactory.class)
@ComplexVmMapping(vmType = CompanyVM.class, vmFactoryType = CompanyVM.VmFactory.class)
public class Company implements IPersistable {

    public static class DbFactory implements IPersistableFactory<Company> {

        @Override
        public Company create() {
            return new Company();
        }
    }

    public static final String FLD_CEO = "ceo";
    public static final String FLD_CTO = "cto";

	private DbId<Company> dbId;
	@SimpleVmMapping
	@DbField
	private String name;
	@ListVmMapping(parentType=Company.class, vmType=PersonVM.class, modelType=Person.class)
	@DbThisToMany(referencedType=Person.class)
	private List<Person> employees = new ArrayList<Person>();
	@ComplexVmMapping(vmType = PersonVM.class)
	@DbThisToOne
	private Person ceo;
    @ComplexVmMapping(vmType = PersonVM.class)
    @DbThisToOne
    private Person cto;

	public Company() {}
	
	public Company(String name, Person ceo)
	{
		setName(name);
		setCeo(ceo);
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public List<Person> getEmployees() {
		return employees;
	}
	public void setEmployees(List<Person> employees) {
		this.employees = employees;
	}
	
	public Person getCeo() {
		return ceo;
	}
	public void setCeo(Person ceo) {
		this.ceo = ceo;
	}

    public Person getCto() { return cto; }
    public void setCto(Person cto) { this.cto = cto; }
	
	@Override
	public int hashCode() {
		return Objects.hashCode(name, employees.size(), ceo);
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o instanceof Company) {
			Company that = (Company) o;
			return Objects.equal(this.name, that.name) &&
				   Objects.equal(this.employees, that.employees) &&
				   Objects.equal(this.ceo, that.ceo);
		}
		else {
			return false;
		}
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("name", this.name)
				.add("ceo", this.ceo)
				.add("employees", (this.employees != null) ? this.employees.size() : 0)
				.toString();
	}

	@Override
	public DbId<Company> getDbId() {
		return this.dbId;
	}
	@Override
	public <T extends IPersistable> void setDbId(DbId<T> dbId) {
		this.dbId = (DbId<Company>) dbId;
	}

	public void addEmployee(Person employee) {
		this.employees.add(employee);
	}
}
