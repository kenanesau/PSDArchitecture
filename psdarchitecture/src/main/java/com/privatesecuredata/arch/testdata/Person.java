package com.privatesecuredata.arch.testdata;

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
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

@DbFactory(factoryType = Person.DbFactory.class)
@DbMultipleForeignKeyFields({@DbForeignKeyField(foreignType=Company.class), @DbForeignKeyField(foreignType=Person.class)})
@ComplexVmMapping(vmType = PersonVM.class, vmFactoryType = PersonVM.VmFactory.class)
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
    public static final String FLD_BIRTHDATE = "birthDate";

    private DbId<Person> dbId;
	@DbField(isMandatory=true)
	private String firstName;
	@DbField(isMandatory=true)
	private String lastName;
	@DbField
	private Date birthDate;
	@DbThisToMany(referencedType=Person.class)
	private Collection<Person> children = new ArrayList<Person>();
	
	public Person() {}

    public Person(String _firstName, String _lastName, int  year, int month, int day)
    {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day);
        init(_firstName, _lastName, new Date(cal.getTimeInMillis()));
    }

	public Person(String _firstName, String _lastName, Date birthdate)
	{
        init(_firstName, _lastName, birthdate);
	}

    public Person (String _firstName, String _lastName, int age)
    {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR) - age);
        setBirthDate(new Date(cal.getTimeInMillis()));

        init(_firstName, _lastName, getBirthDate());
    }

    protected void init(String _firstName, String _lastName, Date birthdate) {
        this.setFirstName(_firstName);
        this.setLastName(_lastName);
        this.birthDate = birthdate;
    }

    public void setBirthDate(Date date) {
        this.birthDate = date;
    }

    public void setBirthDate(int year, int month, int day)
    {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day);
        setBirthDate(new Date(cal.getTimeInMillis()));
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
		return age();
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
		return Objects.hashCode(firstName, lastName, age(), children.size());
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
				   Objects.equal(this.age(), that.age());
		}
		else {
			return false;
		}
	}

    public long age() {
        Calendar cal = Calendar.getInstance();
        if (null != birthDate) {
            cal.setTimeInMillis(birthDate.getTime());
            return Calendar.getInstance().get(Calendar.YEAR) - cal.get(Calendar.YEAR);
        }
        else
            return 0;
    }

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("firstname", (null != this.firstName) ? this.firstName : "null")
				.add("lastname", (null != this.lastName) ? this.lastName : "null")
				.add("age", this.age())
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

    public Date getBirthDate() {
        return birthDate;
    }
}
