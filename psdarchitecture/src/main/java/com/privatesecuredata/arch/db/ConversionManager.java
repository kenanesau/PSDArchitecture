package com.privatesecuredata.arch.db;

import android.database.Cursor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by kenan on 3/18/16.
 */
public class ConversionManager {

    private Map<Class<?>, DefaultObjectConverter> _converterMap = new LinkedHashMap<>();
    private PersistanceManager _oldPm;
    private PersistanceManager _newPm;

    public ConversionManager(PersistanceManager oldPm, PersistanceManager newPm) {
        _oldPm = oldPm;
        _newPm = newPm;
    }

    public void registerConverter(Class<?> type, DefaultObjectConverter conv)
    {
        _converterMap.put(type, conv);
    }

    protected IPersistable __convertNoSave(Class<?> newType, IPersistable oldData) {
        DefaultObjectConverter converter = _converterMap.get(newType);

        return converter.convert(oldData);
    }

    public IPersistable convert(Class<?> newType, IPersistable oldData) {
        IPersistable obj = __convertNoSave(newType, oldData);
        _newPm.save((IPersistable)obj);

        return obj;
    }

    public Cursor getCursor(DbId<?> foreignKey, Class referencingType, Class referencedType)
    {
        return _oldPm.getCursor(foreignKey, referencingType, referencedType);
    }

    public void saveAndUpdateForeignKey(IPersistable object, DbId<?> foreignKey)
    {
        _newPm.saveAndUpdateForeignKey(object, foreignKey);
    }

    public IPersistable load(Class type, Cursor csr, int pos)
    {
        IPersister persister = _oldPm.getPersister(type);
        return persister.rowToObject(pos, csr);
    }

    public <T extends IPersistable> void save(T newObject) {
        _newPm.save(newObject);
    }
}
