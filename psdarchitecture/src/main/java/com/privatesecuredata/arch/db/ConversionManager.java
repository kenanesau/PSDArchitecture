package com.privatesecuredata.arch.db;

import android.database.Cursor;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Helper-class to convert the db from an old version to a new one
 *
 * It reads from the old version and all write-operations go to the new version.
 */
public class ConversionManager {

    private Map<Class<?>, DefaultObjectConverter> _converterMap = new LinkedHashMap<>();
    private PersistanceManager _oldPm;
    private PersistanceManager _newPm;

    public ConversionManager(PersistanceManager oldPm, PersistanceManager newPm) {
        _oldPm = oldPm;
        _newPm = newPm;
    }

    public void registerDefaultConverter(Class<?> type, DefaultObjectConverter conv)
    {
        _converterMap.put(type, conv);
    }

    public void registerObjectConverter(Class<?> type, BaseObjectConverter conv)
    {
        DefaultObjectConverter defaultConverter = _converterMap.get(type);

        defaultConverter.registerObjectConverter(conv);
    }

    protected IPersistable __convertNoSave(Class<?> newType, IPersistable oldData) {
        DefaultObjectConverter converter = _converterMap.get(newType);

        return converter.convert(oldData);
    }

    public <T extends IPersistable> DbId<T> convert(Class<?> newType, IPersistable oldData) {
        IPersistable obj = __convertNoSave(newType, oldData);
        _newPm.save((IPersistable)obj);

        return obj.getDbId();
    }

    public <T extends IPersistable> T convertAndLoad(Class<?> newType, IPersistable oldData) {
        DbId<T> dbId = convert(newType, oldData);
        return _newPm.load(dbId);
    }

    public Cursor getCursor(DbId<?> foreignKey, Class referencingType, Class referencedType)
    {
        return _oldPm.getCursor(foreignKey, referencingType, referencedType);
    }

    public void saveAndUpdateForeignKey(IPersistable object, DbId<?> foreignKey)
    {
        _newPm.saveAndUpdateForeignKey(object, foreignKey);
    }

    public <T extends IPersistable> T load(Class type, Cursor csr, int pos)
    {
        IPersister persister = _oldPm.getPersister(type);
        return (T)persister.rowToObject(pos, csr);
    }

    public void updateCollectionProxySize(DbId persistableId, Field field, long newCollSize) {
        _newPm.updateCollectionProxySize(persistableId, field, newCollSize);
    }

    public <T extends IPersistable> void save(T newObject) {
        _newPm.save(newObject);
    }
}
