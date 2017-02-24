package com.privatesecuredata.arch.db;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by kenan on 3/21/16.
 */
public class BaseObjectConverter<T extends IPersistable> {

    protected Map<String, DefaultObjectConverter.IFieldConverter> _fieldConverterMap = new LinkedHashMap<>();
    protected Map<String, DefaultObjectConverter.IObjectRelationConverter> _oneToOneConverterMap = new LinkedHashMap<>();
    protected Map<String, DefaultObjectConverter.IObjectRelationConverter> _oneToManyConverterMap = new LinkedHashMap<>();
    private ConversionManager _convMan;

    public BaseObjectConverter() {}

    public void registerFieldConverter(String key, DefaultObjectConverter.IFieldConverter<T> converter) {
        _fieldConverterMap.put(key, converter);
    }

    public void registerOneToOneConverter(String key, DefaultObjectConverter.IObjectRelationConverter<T> converter) {
        _oneToOneConverterMap.put(key, converter);
    }

    public void registerOneToManyConverter(String key, DefaultObjectConverter.IObjectRelationConverter<T> converter) {
        _oneToManyConverterMap.put(key, converter);
    }

    public void setConversionManager(ConversionManager convMan)
    {
        _convMan = convMan;
    }

    public ConversionManager getConversionManager()
    {
        return _convMan;
    }
}
