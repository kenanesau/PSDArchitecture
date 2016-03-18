package com.privatesecuredata.arch.db;

import android.database.Cursor;

import com.privatesecuredata.arch.exceptions.DBException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class for converting objects of "old" type to "new" ones
 */
public class DefaultObjectConverter<T extends IPersistable> {

    public interface IFieldConverter<T extends IPersistable> {
        void convertField (SqlDataField newSqlField, T newObject,
                           PersisterDescription oldDesc, Object oldObject);
    }

    public interface IObjectRelationConverter<U extends IPersistable> {
        IPersistable convertObjectRelation (ObjectRelation newRelation, IPersistable newObject,
                                    PersisterDescription oldDesc, Object oldObject);
    }

    private PersisterDescription _oldDesc;
    private PersisterDescription _newDesc;
    private Map<String, IFieldConverter> _fieldConverterMap = new LinkedHashMap<>();
    private Map<String, IObjectRelationConverter> _oneToOneConverterMap = new LinkedHashMap<>();
    private Map<String, IObjectRelationConverter> _oneToManyConverterMap = new LinkedHashMap<>();

    private ConversionManager _convMan;

    public DefaultObjectConverter(ConversionManager convMan, PersisterDescription newDescription, PersisterDescription oldDescription)
    {
        _oldDesc = oldDescription;
        _newDesc = newDescription;
        _convMan = convMan;
    }

    public void registerFieldConverter(String key, IFieldConverter<T> converter)
    {
        _fieldConverterMap.put(key, converter);
    }

    public void registerOneToOneConverter(String key, IObjectRelationConverter<T> converter)
    {
        _oneToOneConverterMap.put(key, converter);
    }

    public void registerOneToManyConverter(String key, IObjectRelationConverter<T> converter)
    {
        _oneToManyConverterMap.put(key, converter);
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

        ObjectRelation oldRel = _oldDesc.getOneToOneRelation(newRelation.getField().getName());
        if (null != oldRel) {
            IPersistable oldData = (IPersistable)oldRel.getField().get(oldObject);
            IObjectRelationConverter<T> converter = _oneToOneConverterMap.get(newRelation.getField().getName());

            Object newData;
            if (null == converter) {
                newData = _convMan.__convertNoSave(newRelation.getField().getType(), oldData);
            }
            else {
                newData = converter.convertObjectRelation(newRelation, newObject, _oldDesc, oldObject);
            }
            newRelation.getField().set(newObject, newData);
            newObject.getDbId().setDirty();
            _convMan.save(newObject);

        }
    }

    public void convertOneToManyRelation(ObjectRelation newRelation, IPersistable newObject, IPersistable oldObject)
            throws IllegalAccessException {
        ObjectRelation oldRel = _oldDesc.getOneToManyRelation(newRelation.getField().getName());
        if (null != oldRel) {
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
                    newData = converter.convertObjectRelation(newRelation, newObject, _oldDesc, oldObject);
                }

                DbId id = newObject.getDbId();
                //id.setDirty();
                _convMan.saveAndUpdateForeignKey(newData, id);
            }

        }
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
