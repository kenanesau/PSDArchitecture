package com.privatesecuredata.arch.testdata.db;

import com.privatesecuredata.arch.db.BaseObjectConverter;
import com.privatesecuredata.arch.db.DefaultObjectConverter;
import com.privatesecuredata.arch.db.IPersistable;
import com.privatesecuredata.arch.db.ObjectRelation;
import com.privatesecuredata.arch.db.PersisterDescription;
import com.privatesecuredata.arch.testdata.Company;
import com.privatesecuredata.arch.testdata.Person;


/**
 * Created by kenan on 3/21/16.
 */
public class CompanyConverter extends BaseObjectConverter<Company> {
    public CompanyConverter()
    {
        /**
         * Read old boss and convert to new CEO-field
         */
        registerOneToOneConverter(Company.FLD_CEO, new DefaultObjectConverter.IObjectRelationConverter<Company>() {
            @Override
            public IPersistable convertObjectRelation(ObjectRelation newRelation, PersisterDescription oldDesc, Object oldObject) {
                com.privatesecuredata.arch.testdata.v1.Company oldCompany = (com.privatesecuredata.arch.testdata.v1.Company)oldObject;

                return getConversionManager().convertAndLoad(newRelation.getField().getType(), oldCompany.getBoss());
            }
        });

        /**
         * new CTO-field introduced -> set default values to that "field" (I know, silly example...)
         */
        registerOneToOneConverter(Company.FLD_CTO, new DefaultObjectConverter.IObjectRelationConverter<Company>() {
            @Override
            public IPersistable convertObjectRelation(ObjectRelation newRelation, PersisterDescription oldDesc, Object oldObject) {
                com.privatesecuredata.arch.testdata.v1.Company oldCompany = (com.privatesecuredata.arch.testdata.v1.Company)oldObject;

                return new Person("John", "Doe", 1970, 5, 3);
            }
        });
    }
}
