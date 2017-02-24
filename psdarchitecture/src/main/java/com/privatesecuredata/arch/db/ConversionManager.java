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

    /**
     * New type -> DefaultObjectConverter
     */
    private Map<Class<?>, DefaultObjectConverter> _converterMap = new LinkedHashMap<>();
    /**
     * Map: old Type -> new Type
     */
    private Map<Class<?>, Class<?>> _oldToNewType = new LinkedHashMap<>();

    private Map<DbId, DbId> _oldIdToNewId = new LinkedHashMap();

    private PersistanceManager _oldPm;
    private PersistanceManager _newPm;
    private long convertedObjects = 0;

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
                    BaseObjectConverter converter = (BaseObjectConverter) constructor.newInstance();
                    converter.setConversionManager(this);
                    registerObjectConverter(newType, converter);
                }
                catch (IllegalArgumentException e) {
                    throw new DBException(String.format("Error instantiating object of type '%s', incorrect parameters!",
                            objConverterType.getName()), e);
                }
                catch (InstantiationException e) {
                    throw new DBException(String.format("Error instantiating object of type '%s'!",
                            objConverterType.getName()), e);
                } catch (IllegalAccessException e) {
                    throw new DBException(String.format("Error instantiating object of type '%s', you do not have no access rights!",
                            objConverterType.getName()), e);
                } catch (InvocationTargetException e) {
                    throw new DBException(String.format("Error instantiating object of type '%s', InvocationTargetException!",
                            objConverterType.getName()), e);
                }
                catch (Exception e) {
                    throw new DBException(String.format("Error using '%s' to convert to new type '%s'",
                            objConverterType.getName(), newType.getName()), e);
                }

            } catch (NoSuchMethodException e) {
                throw new DBException(String.format("Cannot find constructor with no parameters in type '%s'!",
                        objConverterType.getName(), newType.getName()), e);
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

    protected IPersistable __convert(Class<?> newType, IPersistable oldData) {
        if (null == oldData)
            throw new ArgumentException("Parameter oldData must not be null");

        DefaultObjectConverter converter = _converterMap.get(newType);

        IPersistable obj = converter.convert(oldData);
        convertedObjects++;
        _newPm.publishStatus(new StatusMessage(PersistanceManager.Status.INFO, new Long(convertedObjects).toString()));

        return obj;
    }

    public <T extends IPersistable> DbId<T> convert(Class<?> newType, IPersistable oldData) {
        if (null == oldData)
            throw new ArgumentException("Parameter oldData must not be null");

        IPersistable obj = __convert(newType, oldData);

        return obj.getDbId();
    }

    public void convert(Class<?> newType, Cursor csr) {
        if (null == csr)
            throw new ArgumentException("Parameter csr must not be null");

        for (int i=0; i<csr.getCount(); i++) {
            DefaultObjectConverter converter = _converterMap.get(newType);

            IPersistable oldObj = load(converter.getOldDesc().getType(), csr, i);
            convert(newType, oldObj);
        }
    }

    public <T extends IPersistable> T convertAndLoad(Class<?> newType, IPersistable oldData) {
        DbId<T> dbId = convert(newType, oldData);
        return _newPm.load(dbId);
    }

    public Cursor getCursor(DbId<?> foreignKey, Class referencingType, Class referencedType)
    {
        return _oldPm.getCursor(foreignKey, referencingType, referencedType);
    }

    public void updateForeignKey(IPersistable object, DbId<?> foreignKey)
    {
        _newPm.updateForeignKey(object, foreignKey);
    }

    /**
     * Load object from cursor from the old DB
     * @param oldType oldType to load
     * @param csr Cursor
     * @param pos Position in cursor
     * @param <T> generic parameter
     * @return new object
     */
    public <T extends IPersistable> T load(Class oldType, Cursor csr, int pos)
    {
        IPersister persister = _oldPm.getPersister(oldType);
        T res = (T)persister.rowToObject(pos, csr);

        return res;
    }

    public void updateCollectionProxySize(DbId persistableId, Field field, long newCollSize) {
        _newPm.updateCollectionProxySize(persistableId, field, newCollSize);
    }

    /**
     * Save object in the new DB
     *
     * @param newObject
     * @param <T>
     */
    public <T extends IPersistable> void save(T newObject) {
        _newPm.save(newObject);
    }

    /**
     * Save object to the new DB and track its old an new DB-Id
     *
     * @param oldObj reference to the old object
     * @param newObject reference to the new object
     * @param <T>
     */
    public <T extends IPersistable> void saveAndTrack(IPersistable oldObj, T newObject) {
        _newPm.save(newObject);

        if (!_oldIdToNewId.containsKey(oldObj.getDbId())) {
            _oldIdToNewId.put(oldObj.getDbId(), newObject.getDbId());
        }
    }

    /**
     * Retrun the Persistancemanager of the old DB
     * @return PM of old DB
     */
    public PersistanceManager getOldPm() { return this._oldPm; }

    /**
     * Create a new object of type
     * @param type type to create object from
     * @param <T> generic parameter
     * @return new object
     */
    public <T> T createNew(Class type) {
        IPersister persister = _newPm.getPersister(type);
        return (T)persister.createPersistable();
    }

    /**
     * Return the corresponding new type of an old type
     * @param oldType the old type
     * @return the new type
     */
    public Class getMappedNewType(Class<? extends IPersistable> oldType) {
        return _oldToNewType.get(oldType);
    }

    /**
     * Check if the object wich is passed as parameter was already
     * converted to the new db
     *
     * @param oldObject
     * @return true if it was already converted, false otherwise
     */
    public boolean isConverted(IPersistable oldObject) {
        return _oldIdToNewId.containsKey(oldObject.getDbId());
    }

    /**
     * Load an object from the new DB
     *
     * @param newDbId
     * @param <T>
     * @return The new object
     */
    public <T extends IPersistable> T loadNewObject(DbId<T> newDbId) {
        return _newPm.load(newDbId);
    }

    /**
     * Load a new object from the new DB by providing its old object
     *
     * @param oldObject Reference to the old object
     * @param <T>
     * @return the new object
     */
    public <T extends IPersistable> T loadCorrespondingNewObject(IPersistable oldObject) {
        return _newPm.load(_oldIdToNewId.get(oldObject.getDbId()));
    }

    public PersistanceManager getNewPm() {
        return _newPm;
    }
}
