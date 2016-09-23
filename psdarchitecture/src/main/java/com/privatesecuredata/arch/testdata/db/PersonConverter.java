package com.privatesecuredata.arch.testdata.db;

import com.privatesecuredata.arch.db.BaseObjectConverter;
import com.privatesecuredata.arch.db.DefaultObjectConverter.IFieldConverter;
import com.privatesecuredata.arch.db.PersisterDescription;
import com.privatesecuredata.arch.db.SqlDataField;
import com.privatesecuredata.arch.testdata.Person;

import java.util.Calendar;

/**
 * Created by kenan on 3/21/16.
 */
public class PersonConverter extends BaseObjectConverter<Person> {
    public PersonConverter()
    {
        /**
         * Convert old integer-age-field to birthdate
         */
        registerFieldConverter(Person.FLD_BIRTHDATE, new IFieldConverter<Person>() {
            @Override
            public void convertField(SqlDataField newSqlField, Person newObject, PersisterDescription oldDesc, Object oldObject) {
                com.privatesecuredata.arch.testdata.v1.Person oldPerson = (com.privatesecuredata.arch.testdata.v1.Person)oldObject;
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) - (int)oldPerson.getAge());
                newObject.setBirthDate(cal.getTime());
            }
        });
    }
}
