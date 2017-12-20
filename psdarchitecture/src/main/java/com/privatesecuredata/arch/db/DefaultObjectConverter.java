package com.privatesecuredata.arch.db;

import android.database.Cursor;

import com.privatesecuredata.arch.exceptions.DBException;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Set;

/**
 * Class for converting whole objects of "old" type to "new" ones.
 *
 * For each type in the new DB one DefaultObjectConverter is registered at the ConversionManager.
 * For each change of a type a BaseObjectConverter containing the special rules (IFieldConverter
 * or IObjectRelationConverter) is registered with the DefaultObjectConverter of that type.
 *
 * To register these special rules create a new class deriving from BaseObjectConverter and register
 * all special rules in its constructor. Add this new class to your IConversionDescription
 */
public class DefaultObjectConverter<T extends IPersistable> extends BaseObjectConverter<T> {

    /**
     * Interface for converting single fields of an object
     * @param <T>
     */
    public interface IFieldConverter<T extends IPersistable> {
        void convertField (SqlDataField newSqlField, T newObject,
                           PersisterDescription oldDesc, Object oldObject);
    }

    /**
     * Interface for converting all kinds of references to other objects
     * @param <U>
     */
    public interface IObjectRelationConverter<U extends IPersistable> {
        IPersistable convertObjectRelation (ObjectRelation newRelation,
                                            PersisterDescription oldDesc,
                                            Object oldObject);
    }

    private PersisterDescription _oldDesc;
    private PersisterDescription _newDesc;

    public DefaultObjectConverter(ConversionManager convMan, PersisterDescription newDescription, PersisterDescription oldDescription)
    {
        _oldDesc = oldDescription;
        _newDesc = newDescription;
        setConversionManager(convMan);
    }

    public PersisterDescription getOldDesc() {
        return _oldDesc;
    }

    public PersisterDescription getNewDesc() {
        return _newDesc;
    }


    public void registerObjectConverter(BaseObjectConverter other) {

        Set<String> keys = other._fieldConverterMap.keySet();
        for (String key : keys)
        {
            _fieldConverterMap.put(key, (IFieldConverter)other._fieldConverterMap.get(key));
        }

        keys = other._oneToOneConverterMap.keySet();
        for (String key : keys)
        {
            _oneToOneConverterMap.put(key, (IObjectRelationConverter) other._oneToOneConverterMap.get(key));
        }

        keys = other._oneToManyConverterMap.keySet();
        for (String key : keys)
        {
            _oneToManyConverterMap.put(key, (IObjectRelationConverter) other._oneToManyConverterMap.get(key));
        }
    }

    public void convertField(SqlDataField newSqlField, IPersistable newObject, Object oldObject) throws IllegalAccessException {
        Field fld = newSqlField.getObjectField();
        fld.setAccessible(true);

        SqlDataField oldSqlField = _oldDesc.getTableField(newSqlField.getSqlName());
        if (null != oldSqlField) {
            Object oldData = oldSqlField.getObjectField().get(oldObject);
            fld.set(newObject, oldData);
        }
    }

    public void convertOneToOneRelation(ObjectRelation newRelation, IPersistable newObject, Object oldObject)
            throws IllegalAccessException {

        IObjectRelationConverter<T> converter = _oneToOneConverterMap.get(newRelation.getField().getName());
        IPersistable newData = null;
        IPersistable oldData = null;
        if (null != converter)
        {
            newData = converter.convertObjectRelation(newRelation, _oldDesc, oldObject);
        }
        else {
            ObjectRelation oldRel = _oldDesc.getOneToOneRelation(newRelation.getField().getName());
            if (null != oldRel) {
                oldData = (IPersistable) oldRel.getField().get(oldObject);
                if (null != oldData) {
                    Class newType = getConversionManager().getMappedNewType(oldData.getClass());
                    if (null == newType) {
                        throw new DBException(
                                String.format("Unable to find the new corresponding type for old type '%s'",
                                        oldData.getClass().getName()));
                    }
                    newData = getConversionManager().__convert(newType, oldData);
                }
            }
            else {
                new DBException(String.format("Unable to convert object of type '%s', no converter found for field '%s'",
                        newRelation.getReferencingType().getName(),
                        newRelation.getField().getName()));
            }

        }

        if ( (null != newData) && (!newRelation.isComposition()) )
            this.save(oldData, newData);
        newRelation.getField().set(newObject, newData);
    }

    /**
     * Convert one to many relation
     * @param newRelation
     * @param newObject
     * @param oldObject
     * @return Count of objects in the collection
     * @throws IllegalAccessException
     */
    public int convertOneToManyRelation(ObjectRelation newRelation, IPersistable newObject, IPersistable oldObject)
            throws IllegalAccessException {
        ObjectRelation oldRel = _oldDesc.getOneToManyRelation(newRelation.getField().getName());
        IPersistable oldData;
        IObjectRelationConverter<T> converter = _oneToManyConverterMap.get(newRelation.getField().getName());
        ConversionManager convMan = getConversionManager();

        Class typeOfData = oldRel.getReferencedListType();
        Cursor csr = convMan.getCursor((oldObject).getDbId(), oldRel.getReferencingType(), typeOfData);
        IPersistable newData;
        int cnt=0;
        DbId id = newObject.getDbId();
        for (cnt=0; cnt<csr.getCount(); cnt++)
        {
            oldData = convMan.load(typeOfData, csr, cnt);
            if (null == converter) {
                newData = convMan.__convert(newRelation.getReferencedListType(), oldData);
            }
            else {
                newData = converter.convertObjectRelation(newRelation, _oldDesc, oldObject);
            }

            convMan.updateForeignKey(newData, id);
        }

        return cnt;
    }

    public void save(IPersistable oldObject, IPersistable newObject) {
        if (_oldDesc.getReferenceCount() > 1)
            getConversionManager().saveAndTrack(oldObject, newObject);
        else
            getConversionManager().save(newObject);
    }

    /**
     * Unconditionally rewrite the whole object (needed for updating the Proxy-Count-fields
     * @param newObject
     */
    public void rewrite(IPersistable newObject)
    {
        newObject.getDbId().setDirty();
        getConversionManager().save(newObject);
    }

    public <T extends IPersistable> T convert(IPersistable oldObject) {
        T newObject;
        String currentFieldName = "";
        try {
            if (_oldDesc.getReferenceCount() > 1) {
                if (getConversionManager().isConverted(oldObject))
                {
                    newObject = getConversionManager().loadCorrespondingNewObject(oldObject);
                    return newObject;
                }
            }

            Class<T> type = _newDesc.getType();
            ConversionManager convMan = getConversionManager();
            newObject = convMan.createNew(type);

            Collection<SqlDataField> fields = _newDesc.getTableFields();
            for (SqlDataField newSqlField : fields) {
                currentFieldName = newSqlField.getSqlName();
                SqlDataField.SqlFieldType fldType = newSqlField.getSqlType();

                /** Only convert "real" data-fields **/
                if (fldType == SqlDataField.SqlFieldType.COLLECTION_REFERENCE ||
                        fldType == SqlDataField.SqlFieldType.OBJECT_NAME ||
                        fldType == SqlDataField.SqlFieldType.OBJECT_REFERENCE)
                    continue;

                if (newSqlField.isComposition()) {

                }
                else {
                    IFieldConverter<T> fldConverter = _fieldConverterMap.get(newSqlField.getObjectField().getName());

                    if (null == fldConverter)
                        convertField(newSqlField, newObject, oldObject);
                    else
                        fldConverter.convertField(newSqlField, newObject, _oldDesc, oldObject);
                }
            }

            Collection<ObjectRelation> oneToOneRels = _newDesc.getOneToOneRelations();
            for (ObjectRelation newOneToOneRel : oneToOneRels)
            {
                convertOneToOneRelation(newOneToOneRel, newObject, oldObject);
            }

            this.save(oldObject, newObject);

            Collection<ObjectRelation> oneToManyRels = _newDesc.getOneToManyRelations();
            for (ObjectRelation newOneToManyRel : oneToManyRels)
            {
                int cnt = convertOneToManyRelation(newOneToManyRel, newObject, oldObject);
                convMan.updateCollectionProxySize(newObject.getDbId(), newOneToManyRel.getField(), cnt);
            }
        }
        catch (Exception ex) {
            throw new DBException(String.format("Error converting object of type '%s', field '%s'",
                    _newDesc.getType().getName(), currentFieldName), ex);
        }

        return newObject;
    }
}
