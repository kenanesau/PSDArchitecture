package com.privatesecuredata.arch.db;

import android.database.Cursor;

import com.privatesecuredata.arch.exceptions.DBException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Set;

/**
 * Class for converting objects of "old" type to "new" ones
 */
public class DefaultObjectConverter<T extends IPersistable> extends BaseObjectConverter<T> {

    public interface IFieldConverter<T extends IPersistable> {
        void convertField (SqlDataField newSqlField, T newObject,
                           PersisterDescription oldDesc, Object oldObject);
    }

    public interface IObjectRelationConverter<U extends IPersistable> {
        IPersistable convertObjectRelation (ObjectRelation newRelation,
                                            PersisterDescription oldDesc,
                                            Object oldObject);
    }

    private PersisterDescription _oldDesc;
    private PersisterDescription _newDesc;
    private ConversionManager _convMan;

    public DefaultObjectConverter(ConversionManager convMan, PersisterDescription newDescription, PersisterDescription oldDescription)
    {
        _oldDesc = oldDescription;
        _newDesc = newDescription;
        _convMan = convMan;
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
        Field fld = newSqlField.getField();
        fld.setAccessible(true);

        SqlDataField oldSqlField = _oldDesc.getTableField(newSqlField.getSqlName());
        if (null != oldSqlField) {
            Object oldData = oldSqlField.getField().get(oldObject);
            fld.set(newObject, oldData);
        }
    }

    public void convertOneToOneRelation(ObjectRelation newRelation, IPersistable newObject, Object oldObject)
            throws IllegalAccessException {

        IObjectRelationConverter<T> converter = _oneToOneConverterMap.get(newRelation.getField().getName());
        IPersistable newData = null;
        if (null != converter)
        {
            newData = converter.convertObjectRelation(newRelation, _oldDesc, oldObject);
        }
        else {
            ObjectRelation oldRel = _oldDesc.getOneToOneRelation(newRelation.getField().getName());
            if (null != oldRel) {
                IPersistable oldData = (IPersistable) oldRel.getField().get(oldObject);
                newData = _convMan.__convertNoSave(newRelation.getField().getType(), oldData);
            }
            else {
                new DBException(String.format("Unable to convert object of type '%s', no converter found for field '%s'",
                        newRelation.getReferencingType().getName(),
                        newRelation.getField().getName()));
            }

        }

        if (null != newData)
            _convMan.save(newData);
        newRelation.getField().set(newObject, newData);
    }

    public void convertOneToManyRelation(ObjectRelation newRelation, IPersistable newObject, IPersistable oldObject)
            throws IllegalAccessException {
        ObjectRelation oldRel = _oldDesc.getOneToManyRelation(newRelation.getField().getName());
        IPersistable oldData;
        IObjectRelationConverter<T> converter = _oneToManyConverterMap.get(newRelation.getField().getName());

        Class typeOfData = oldRel.getReferencedListType();
        Cursor csr = _convMan.getCursor((oldObject).getDbId(), oldRel.getReferencingType(), typeOfData);
        IPersistable newData;
        for (int i=0; i<csr.getCount(); i++)
        {
            oldData = _convMan.load(typeOfData, csr, i);
            if (null == converter) {
                newData = _convMan.__convertNoSave(newRelation.getReferencedListType(), oldData);
                newData.getDbId().setDirty();
            }
            else {
                newData = converter.convertObjectRelation(newRelation, _oldDesc, oldObject);
            }

            DbId id = newObject.getDbId();
            //id.setDirty();
            _convMan.saveAndUpdateForeignKey(newData, id);
        }

        _convMan.updateCollectionProxySize(newObject.getDbId(), newRelation.getField(), csr.getCount());
    }

    public <T extends IPersistable> T convert(IPersistable oldObject) {
        T newObject;
        String currentFieldName = "";
        try {
            Class<T> type = _newDesc.getType();
            Constructor<T> _const = type.getConstructor((Class<?>[]) null);
            newObject = _const.newInstance();

            Collection<SqlDataField> fields = _newDesc.getTableFields();
            for (SqlDataField newSqlField : fields) {
                currentFieldName = newSqlField.getSqlName();
                SqlDataField.SqlFieldType fldType = newSqlField.getSqlType();

                /** Only convert "real" data-fields **/
                if (fldType == SqlDataField.SqlFieldType.COLLECTION_REFERENCE ||
                        fldType == SqlDataField.SqlFieldType.OBJECT_NAME ||
                        fldType == SqlDataField.SqlFieldType.OBJECT_REFERENCE)
                    continue;

                IFieldConverter<T> fldConverter = _fieldConverterMap.get(newSqlField.getField().getName());

                if (null == fldConverter)
                    convertField(newSqlField, newObject, oldObject);
                else
                    fldConverter.convertField(newSqlField, newObject, _oldDesc, oldObject);
            }
            _convMan.save(newObject);

            Collection<ObjectRelation> oneToOneRels = _newDesc.getOneToOneRelations();
            for (ObjectRelation newOneToOneRel : oneToOneRels)
            {
                convertOneToOneRelation(newOneToOneRel, newObject, oldObject);
            }

            newObject.getDbId().setDirty();
            _convMan.save(newObject);

            Collection<ObjectRelation> oneToManyRels = _newDesc.getOneToManyRelations();
            for (ObjectRelation newOneToManyRel : oneToManyRels)
            {
                convertOneToManyRelation(newOneToManyRel, newObject, oldObject);
            }
        }
        catch (Exception ex) {
            throw new DBException(String.format("Error converting object of type %s field of type %s",
                    _newDesc.getType().getName(), currentFieldName), ex);
        }

        return newObject;
    }
}
