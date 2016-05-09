package com.privatesecuredata.arch.db;

import android.database.Cursor;

import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.exceptions.DBException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Helper-class to convert the db from an old version to a new one
 *
 * It reads from the old version and all write-operations go to the new version.
 */
public class ConversionManager {

    private Map<Class<?>, DefaultObjectConverter> _converterMap = new LinkedHashMap<>();
    /**
     * Map: old Type -> new Type
     */
    private Map<Class<?>, Class<?>> _oldToNewType = new LinkedHashMap<>();

    private PersistanceManager _oldPm;
    private PersistanceManager _newPm;

    public ConversionManager(PersistanceManager oldPm, PersistanceManager newPm, IConversionDescription convDesc)
    {
        this(oldPm, newPm);

        for (Class[] types : convDesc.getEntityMappings()) {
            Class newType = types[0];
            Class oldType = types[1];
            _oldToNewType.put(oldType, newType);
            if (newType.equals(oldType)) {
                throw new DBException(String.format("Error in '%s' type '%s' is defined as old and new!!",
                        convDesc.getClass().getName(), oldType.getName()));
            }

            PersisterDescription oldDesc = oldPm.getIPersister(oldType).getDescription();
            PersisterDescription newDesc = newPm.getIPersister(newType).getDescription();

            DefaultObjectConverter<?> converter = new DefaultObjectConverter<>(this, newDesc, oldDesc);
            registerDefaultConverter(newType, converter);
        }

        for (Class[] objConverter : convDesc.getObjectConverters()) {
            Class newType = objConverter[0];
            Class objConverterType = objConverter[1];
            try {
                Constructor constructor = objConverterType.getConstructor((Class[]) null);
                try {
                    BaseObjectConverter converter = (BaseObjectConverter) constructor.newInstance((Class[]) null);
                    converter.setConversionManager(this);
                    registerObjectConverter(newType, converter);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        Class[] peristentTypes = newPm.getDbDescription().getPersistentTypes();
        for( Class type : peristentTypes) {
            if (!hasConverter(type)) {
                IPersister persister = oldPm.getIPersister(type);
                if (null == persister)
                    continue;

                _oldToNewType.put(type, type);
                PersisterDescription oldDesc = persister.getDescription();
                PersisterDescription newDesc = newPm.getIPersister(type).getDescription();
                registerDefaultConverter(type, new DefaultObjectConverter(this, newDesc, oldDesc));
            }
        }
    }

    public ConversionManager(PersistanceManager oldPm, PersistanceManager newPm) {
        _oldPm = oldPm;
        _newPm = newPm;
    }

    public void registerDefaultConverter(Class<?> type, DefaultObjectConverter conv)
    {
        _converterMap.put(type, conv);
    }

    public boolean hasConverter(Class<?> type) {
        return _converterMap.containsKey(type);
    }

    public void registerObjectConverter(Class<?> type, BaseObjectConverter conv)
    {
        DefaultObjectConverter defaultConverter = _converterMap.get(type);

        defaultConverter.registerObjectConverter(conv);
    }

    protected IPersistable __convertNoSave(Class<?> newType, IPersistable oldData) {
        if (null == oldData)
            throw new ArgumentException("Parameter oldData must not be null");

        DefaultObjectConverter converter = _converterMap.get(newType);

        return converter.convert(oldData);
    }

    public <T extends IPersistable> DbId<T> convert(Class<?> newType, IPersistable oldData) {
        if (null == oldData)
            throw new ArgumentException("Parameter oldData must not be null");

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

    public PersistanceManager getOldPm() { return this._oldPm; }

    public <T> T createNew(Class type) {
        IPersister persister = _newPm.getPersister(type);
        return (T)persister.createPersistable();
    }

    public Class getMappedNewType(Class<? extends IPersistable> oldType) {
        return _oldToNewType.get(oldType);
    }
}
