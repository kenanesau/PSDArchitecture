package com.privatesecuredata.arch.testdata.db;

import com.privatesecuredata.arch.db.ConversionManager;
import com.privatesecuredata.arch.db.DbId;
import com.privatesecuredata.arch.db.IConversionDescription;
import com.privatesecuredata.arch.testdata.Company;
import com.privatesecuredata.arch.testdata.Person;

/**
 * Created by kenan on 3/22/16.
 */
public class V2CompanyConversionDescription implements IConversionDescription {
    public interface IConverter {
        void convert(ConversionManager convMan);
    }

    IConverter converterStrategy;

    public void setConverterStrategy(IConverter conv)
    {
        this.converterStrategy = conv;
    }

    @Override
    public Class[][] getEntityMappings() {
        return new Class[][] {
                {Person.class, com.privatesecuredata.arch.testdata.v1.Person.class},
                {Company.class, com.privatesecuredata.arch.testdata.v1.Company.class},
        };
    }

    @Override
    public Class[][] getObjectConverters() {
        return new Class[][] {
                {Person.class, PersonConverter.class},
                {Company.class, CompanyConverter.class}
        };
    }

    @Override
    public void convert(ConversionManager convMan) {
        com.privatesecuredata.arch.testdata.v1.Company oldCompany =
                convMan.getOldPm().loadFirst(com.privatesecuredata.arch.testdata.v1.Company.class);
        DbId<?> newCompanyId = convMan.convert(Company.class, oldCompany);
    }
}
