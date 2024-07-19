package com.privatesecuredata.arch.testdata.v1;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.privatesecuredata.arch.db.DbId;
import com.privatesecuredata.arch.db.IPersistable;
import com.privatesecuredata.arch.db.IPersistableFactory;
import com.privatesecuredata.arch.db.annotations.DbFactory;
import com.privatesecuredata.arch.db.annotations.DbField;
import com.privatesecuredata.arch.db.annotations.DbForeignKeyField;
import com.privatesecuredata.arch.db.annotations.DbMultipleForeignKeyFields;
import com.privatesecuredata.arch.db.annotations.DbThisToMany;
import com.privatesecuredata.arch.mvvm.annotations.ComplexVmMapping;
import com.privatesecuredata.arch.testdata.vm.PersonVM;

import java.util.ArrayList;
import java.util.Collection;

@DbFactory(factoryType = Person.DbFactory.class)
@DbMultipleForeignKeyFields({@DbForeignKeyField(foreignType=Company.class), @DbForeignKeyField(foreignType=Person.class)})
@ComplexVmMapping(vmType = PersonVM.class)
public class Person implements IPersistable {

    public static class DbFactory implements IPersistableFactory<Person> {

        @Override
        public Person create() {
            return new Person();
        }
    }

    public static final String QUERY_PERSONS_OF_LAST_NAME_ORDERED_BY_FIRST = "query_persons_of_lastname_ordered_by_first";
    public static final String QUERY_PERSONS_OF_LAST_NAME = "query_persons_of_lastname";
    public static final String QUERY_PERSONS_OF_NAME = "query_persons_of_name";
    public static final String FLD_FIRSTNAME = "firstName";
    public static final String FLD_LASTNAME = "lastName";

    private DbId<Person> dbId;
	@DbField(isMandatory=true)
	private String firstName;
	@DbField(isMandatory=true)
	private String lastName;
	@DbField
	private long age;
	@DbThisToMany(referencedType=Person.class)
	private Collection<Person> children = new ArrayList<Person>();
	
	public Person() {}
	
	public Person(String _firstName, String _lastName, int _age)
	{
		this.setFirstName(_firstName);
		this.setLastName(_lastName);
		this.setAge(_age);
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public long getAge() {
		return age;
	}

	public void setAge(long age) {
		this.age = age;
	}
	
	public void addChild(Person child)
	{
		this.children.add(child);	
	}
	
	public void killChild(Person child)
	{
		this.children.remove(child);
	}
	
	public Collection<Person> getChildren() { return this.children;}
	
	@Override
	public int hashCode() {
		return Objects.hashCode(firstName, lastName, age, children.size());
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o instanceof Person) {
			Person that = (Person) o;
			return Objects.equal(this.firstName, that.firstName) &&
				   Objects.equal(this.lastName, that.lastName) &&
				   Objects.equal(this.age, that.age);
		}
		else {
			return false;
		}
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("firstname", this.firstName)
				.add("lastname", this.lastName)
				.add("age", this.age)
				.toString();
	}

	@Override
	public DbId<Person> getDbId() {
		return this.dbId;
	}

	@Override
	public <T extends IPersistable>  void setDbId(DbId<T> dbId) {
		this.dbId = (DbId<Person>)dbId;
	}
}
