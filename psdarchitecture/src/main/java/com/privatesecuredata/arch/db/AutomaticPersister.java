package com.privatesecuredata.arch.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.google.common.base.MoreObjects;
import com.privatesecuredata.arch.db.annotations.DbFactory;
import com.privatesecuredata.arch.db.annotations.DbField;
import com.privatesecuredata.arch.db.annotations.DbForeignKeyField;
import com.privatesecuredata.arch.db.annotations.DbMultipleForeignKeyFields;
import com.privatesecuredata.arch.db.annotations.DbThisToMany;
import com.privatesecuredata.arch.db.annotations.DbThisToOne;
import com.privatesecuredata.arch.db.query.Query;
import com.privatesecuredata.arch.exceptions.DBException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AutomaticPersister<T extends IPersistable> extends AbstractPersister<T> {
    protected SQLiteStatement insert;
    protected SQLiteStatement update;

    public static final String DATE_FORMAT = "yyyy-MM-dd";

    /**
     * Only used if no factory is declared via @DbFactory-annotation
     */
    private Constructor<T> _const;
    
    private IPersistableFactory<T> _factory;

    protected AutomaticPersister() {
    }

    public AutomaticPersister(PersistanceManager pm, Class<T> persistentType) throws Exception {
        setPM(pm);
        setTableName(DbNameHelper.getTableName(persistentType));
        setPersistentType(persistentType);
        Field[] fields = persistentType.getDeclaredFields();

        createDbFactory(persistentType);

        DbForeignKeyField anno = persistentType.getAnnotation(DbForeignKeyField.class);
        if (anno != null)
            getDescription().addForeignKeyField(anno);

        DbMultipleForeignKeyFields multipleAnnos = persistentType.getAnnotation(DbMultipleForeignKeyFields.class);
        if (null != multipleAnnos) {
            for (DbForeignKeyField dbAnno : multipleAnnos.value())
                getDescription().addForeignKeyField(dbAnno);
        }

        for (Field field : fields) {
            DbField fieldAnno = field.getAnnotation(DbField.class);
            if (null != fieldAnno)
                getDescription().addSqlField(field, fieldAnno);

            DbThisToOne thisToOneAnno = field.getAnnotation(DbThisToOne.class);
            if (null != thisToOneAnno) {
                ObjectRelation objRel = new ObjectRelation(field, getPersistentType(), thisToOneAnno);
                getDescription().addOneToOneRelation(objRel);

                if (thisToOneAnno.isComposition()) {
                    IPersister composedPersister = pm.getPersister((Class)field.getType());
                    if (null == composedPersister)
                        throw new DBException(String.format("Could not find composed persister for type '%s'",
                                field.getType().getCanonicalName()));

                    composePersister(composedPersister, field);
                }
                else {
                    field.setAccessible(true);

                    // At the moment DbThisToOne-Annotations are always saved in a long-field of the referencing
                    // object -> Maybe later: also make it possible to save as a foreign-key in the referenced object.
                    SqlDataField idField = new SqlDataField(field);
                    getDescription().addSqlField(idField);

                    SqlDataField fldTypeName = new SqlDataField(field, field.getType());
                    getDescription().addSqlField(fldTypeName);
                }
            }

            // At the moment DbThisToMany-Annotations are always saved as a foreign-key in the table of the referenced object
            DbThisToMany oneToManyAnno = field.getAnnotation(DbThisToMany.class);

            if (null != oneToManyAnno) {
                field.setAccessible(true);
                ObjectRelation objRel = new ObjectRelation(field, oneToManyAnno.referencedType(), getPersistentType(), oneToManyAnno.deleteChildren(), false, false, oneToManyAnno.queryId());
                getDescription().addOneToManyRelation(objRel);

                Class referencedType = oneToManyAnno.referencedType();
                ICursorLoaderFactory fac = new IdCursorLoaderFactory(getPM(), persistentType, referencedType);
                getPM().registerCursorLoaderFactory(fac);

				/**
                 * Add table-field for the collection-proxy-size
                 */
                SqlDataField collectionProxySizeFld = new SqlDataField(field, referencedType);
                getDescription().addProxyCntField(collectionProxySizeFld);
            }
        }
    }

    protected void createDbFactory(Class type) throws IllegalAccessException, InstantiationException, NoSuchMethodException {
        DbFactory factoryAnno = (DbFactory) type.getAnnotation(DbFactory.class);
        if (factoryAnno != null) {
            _factory = (IPersistableFactory<T>)factoryAnno.factoryType().newInstance();
        }
        else {
            setConstructor(type.getConstructor((Class<?>[]) null));
            _factory = new IPersistableFactory<T>() {
                @Override
                public T create() {
                    T ret = null;
                    try {
                        ret = (T)getConstructor().newInstance();
                    } catch (Exception e) {
                        new DBException(String.format("Error creating new Instance of type '%s'",
                                getPersistentType().getName()), e);
                    }

                    return ret;
                }
            };
        }
    }

    public void extendsPersister(AutomaticPersister<?> parentPersister) {
        getDescription().extend(parentPersister.getDescription());
        parentPersister.addExtendingPersister(this);
    }

    protected void composePersister(IPersister composedPersister, Field composeParentField) {
        getDescription().extend(composedPersister.getDescription(), composeParentField);
    }

    /**
     * Constructor of the Persistable for which this Persister is used for
     */
    protected Constructor<T> getConstructor() {
        return _const;
    }

    protected void setConstructor(Constructor<T> _const) {
        this._const = _const;
    }

    public List<SqlDataField> getSqlFields() {
        return new ArrayList<>(getDescription().getTableFields());
    }

    public Map<String, SqlDataField> getFieldMap() {
        return new LinkedHashMap<>(getDescription().getFieldMap());
    }

    /**
     * List of list-Fields to be filled in an persistable and the corresponding types
     */
    public Collection<ObjectRelation> getOneToManyRelations() {
        return getDescription().getOneToManyRelations();
    }

    public Collection<ObjectRelation> getOneToOneRelations() {
        return getDescription().getOneToOneRelations();
    }

    @Override
    public String getSelectAllStatement(OrderByTerm... terms) {
        if (getDescription().getTableFields().isEmpty())
            return null;

        StringBuilder sql = AbstractPersister.createSelectAllStatement(getTableName(), getSqlFields(), terms);

        return sql.toString();
    }


    /**
     * Creates the SQL-Statement for inserting a new row to the database
     *
     * @return SQL-Statement (insert)
     */
    protected String getInsertStatement() {
        if (getDescription().getTableFields().isEmpty())
            return null;

        StringBuilder sql = new StringBuilder("INSERT INTO ")
                .append(getTableName())
                .append(" ( ");

        int fieldCount = 0;
        for (SqlDataField fld : getDescription().getTableFields()) {
            if (fieldCount > 0)
                sql.append(", ");

            sql.append(fld.getSqlName());

            fieldCount++;
        }

        sql.append(" ) VALUES ( ");

        for (int i = 0; i < fieldCount; i++) {
            sql.append("?");
            if (i + 1 < fieldCount)
                sql.append(", ");
            else
                sql.append(" ");
        }

        sql.append(")");
        return sql.toString();
    }

    /**
     * Creates the SQL-Statement for updating a row in the database
     *
     * @return SQL-Statement (update)
     */
    protected String getUpdateStatement() {
        if (getDescription().getTableFields().isEmpty())
            return null;

        StringBuilder sql = new StringBuilder("UPDATE ")
                .append(getTableName())
                .append(" SET ");

        int i = 1;
        for (SqlDataField field : getDescription().getTableFields()) {
            sql.append(field.getSqlName());
            sql.append("=? ");
            if (i < getSqlFields().size())
                sql.append(", ");
            i++;
        }

        sql.append("WHERE _id=?");

        return sql.toString();
    }


    /**
     * Creates the SQL-Statement for creating the database
     *
     * @return SQL-Statement (create)
     */
    protected String getCreateStatement() {
        if (getDescription().getTableFields().isEmpty())
            return null;

        /**
         * Create tables
         */
        StringBuilder sql = new StringBuilder("CREATE TABLE ")
                .append(getTableName())
                .append(" ( ")
                .append("_id INTEGER PRIMARY KEY AUTOINCREMENT");

        //Handle normal DB-Fields
        for (SqlDataField field : getDescription().getTableFields()) {
            sql.append(", ")
                    .append(field.getSqlName()).append(" ")
                    .append(field.getSqlTypeString());

            if (field.isMandatory())
                sql.append(" NOT NULL");
        }

        //Add the foreign key-fields and constraints
        for (SqlDataField field : getDescription().getForeignKeyFields()) {
            sql.append(", ")
                    .append(field.getSqlName()).append(" ")
                    .append(field.getSqlTypeString());

            // Foreign-Key Fields do never have a NOT NULL constraint
            // since they are deferred
        }
        for (SqlForeignKeyField field : getDescription().getForeignKeyFields()) {
            Class<?> cls = field.getForeignKeyType();
            if ((field.isMandatory()) && (null != cls)) {
                sql.append(",\nFOREIGN KEY(")
                        .append(field.getSqlName()).append(") REFERENCES ")
                        .append(cls.getSimpleName()).append("(_id) DEFERRABLE INITIALLY DEFERRED");
            }
        }

        sql.append(" ) ");

        return sql.toString();
    }

    @Override
    public void init(Object obj) throws DBException {
        try {
            super.init(obj);

            String insertStatement = getInsertStatement();
            if (null != insertStatement)
                insert = getDb().compileStatement(insertStatement);
            String updateStatement = getUpdateStatement();
            if (null != updateStatement)
                update = getDb().compileStatement(getUpdateStatement());
            if (getDescription().hasForeignKeyFields()) {
                for (SqlForeignKeyField fld : getDescription().getForeignKeyFields()) {
                    fld.compileUpdateStatement(getDb());
                }
            }

            List<SqlDataField> proxyCntFields = getDescription().getAndResetProxyCntFields();
            if (null != proxyCntFields) {
                for (SqlDataField collectionProxySizeFld : proxyCntFields) {
                    // Create an update-Statement for the collection-proxy-size
                    SQLiteStatement updateListCountStatement = createUpdateListCountStatement(getTableName(), collectionProxySizeFld);
                    addUpdateProxyStatement(collectionProxySizeFld.getObjectField(), updateListCountStatement);
                }

                proxyCntFields.clear();
            }
        }
        catch (Exception ex)
        {
            Log.e(getClass().getName(), String.format("Error initializing AutomaticPersister for table '%s':", getTableName()));
            Log.e(getClass().getName(), ex.toString());
            throw ex;
        }
    }

    public void bind(SQLiteStatement sql, int idx, SqlDataField sqlField, T persistable) throws IllegalAccessException, IllegalArgumentException {
        Field fld = sqlField.getObjectField();
        if (null != fld)
            fld.setAccessible(true);

        switch (sqlField.getSqlType()) {
            case STRING:
                Object val = fld.get(persistable);
                String valStr = null;
                if (null != val)
                    valStr = val.toString();
                bind(sql, idx, valStr);
                break;
            case DATE:
                val = fld.get(persistable);
                bind(sql, idx, DbNameHelper.getDbDateString((Date)val));
                break;
            case BOOLEAN:
                bind(sql, idx, fld.getBoolean(persistable) == true ? 1L : 0L);
                break;
            case DOUBLE:
                bind(sql, idx, fld.getDouble(persistable));
                break;
            case FLOAT:
                bind(sql, idx, fld.getFloat(persistable));
                break;
            case INTEGER:
                bind(sql, idx, fld.getInt(persistable));
                break;
            case LONG:
                bind(sql, idx, fld.getLong(persistable));
                break;
            case OBJECT_REFERENCE:
                IPersistable referencedObj = (IPersistable) fld.get(persistable);
                if (null == referencedObj) {
                    bindNull(sql, idx);
                    break;
                }

                DbId<?> dbId = referencedObj.getDbId();
                if (null == dbId)
                    throw new DBException(String.format("Unable to safe reference from object of type \"%s\" to object of type \"%s\" since this object is not persistent yet!",
                            persistable.getClass().getName(), referencedObj.getClass().getName()));

                bind(sql, idx, referencedObj.getDbId().getId());
                break;
            case OBJECT_NAME:
                referencedObj = (IPersistable) fld.get(persistable);
                if (null == referencedObj) {
                    bindNull(sql, idx);
                    break;
                }

                PersisterDescription desc = getPM().getPersister(referencedObj.getClass()).getDescription();
                bind(sql, idx, desc.getDbTypeName());
                break;
            case COLLECTION_REFERENCE:
                Collection<?> referencedColl = (Collection<?>) fld.get(persistable);
                if (null == referencedColl)
                    break;

                bind(sql, idx, referencedColl.size());
                break;
            default:
                break;
        }
    }

    public void bind(SQLiteStatement sql, T persistable) {
        sql.clearBindings();

        int i = 1;
        try {
            for (SqlDataField field : getDescription().getTableFields()) {

                if (field.isComposition()) {
                    AutomaticPersister composedPersister = (AutomaticPersister)getPM().getPersister((Class)field.getComposeField().getType());
                    composedPersister.bind(sql, i++, field, (IPersistable)field.getComposeField().get(persistable));
                }
                else {
                    bind(sql, i++, field, persistable);
                }
            }

            onActionsSave(persistable);
        } catch (Exception e) {
            throw new DBException(
                    String.format("Error binding to SQL statement \"%s\"", sql.toString()), e);
        }
    }

    @Override
    public long insert(T persistable) throws DBException {
        // First save the referenced objects
        ObjectRelation errRel = null;
        try {

            for (ObjectRelation rel : getDescription().getOneToOneRelations()) {
                /* If it's a composition -- the data is saved with the referencing object */
                if (rel.isComposition())
                    continue;
                errRel = rel;
                IPersistable other = (IPersistable) rel.getField().get(persistable);
                if (null == other)
                    continue;

                if (other.getDbId() == null ? true : other.getDbId().getDirty())
                    getPM().save(other);
            }
        } catch (Exception ex) {
            throw new DBException(String.format("Error saving one-to-one relation from type \"%s\" to \"%s\"!",
                    persistable.getClass().getName(), errRel.getField().getType().getName()), ex);
        }

        bind(insert, persistable);

        long id = insert.executeInsert();
        DbId<?> dbId = null;

        if ((getOneToOneRelations().size() > 0) || (getOneToManyRelations().size() > 0))
            dbId = getPM().assignDbId(persistable, id);

        saveAndUpdateForeignKeyRelations(persistable, dbId);

        return id;
    }

    private void saveAndUpdateForeignKeyRelations(T persistable, DbId<?> dbId) {
        try {

            for (ObjectRelation rel : getOneToManyRelations()) {
                Collection<?> others = (Collection<?>) rel.getField().get(persistable);
                if (null == others)
                    continue;
                if (Proxy.isProxyClass(others.getClass())) {
                    LazyCollectionInvocationHandler handler = (LazyCollectionInvocationHandler) Proxy.getInvocationHandler(others);
                    /**
                     * If there was no change -> skip it
                     */
                    if (!handler.isLoaded())
                        continue;
                }

                for (Object other : others) {
                    getPM().saveAndUpdateForeignKey((IPersistable) other, dbId);
                }
            }
        } catch (Exception ex) {
            throw new DBException(String.format("Error saving one-to-many relation in type \"%s\"!",
                    persistable.getClass().getName()), ex);
        }
    }

    @Override
    public long update(T persistable) throws DBException {
        bind(update, persistable);
        bind(update, getDescription().getTableFields().size() + 1, persistable.getDbId().getId());

        int rowsAffected = update.executeUpdateDelete();

        DbId<?> dbId = persistable.getDbId();
        saveAndUpdateForeignKeyRelations(persistable, dbId);

        return rowsAffected;
    }

    @Override
    public void updateForeignKey(DbId<T> persistableId, DbId<?> foreignId)
            throws DBException {
        IPersistable foreignObj = foreignId.getObj();
        if (null != foreignObj) {
            Class<?> foreignType = foreignObj.getClass();
            SqlForeignKeyField fld = getDescription().getForeignKeyField(foreignType);

            if (fld != null) {
                SQLiteStatement updateForeignKey = fld.getUpdateForeingKey();
                updateForeignKey.clearBindings();
                updateForeignKey.bindLong(1, foreignId.getId());
                updateForeignKey.bindLong(2, persistableId.getId());

                int ret = updateForeignKey.executeUpdateDelete();
                /*if (ret == 0)
                    throw new DBException(String.format("Updating Foreign-key-relation to type \"%s\" in type \"%s\" failed!",
                            foreignType.getName(), getPersistentType().getName()));*/
            } else {
                throw new DBException(String.format("Could not find Foreign-key-relation to type \"%s\" in type \"%s\"",
                        foreignType.getName(), getPersistentType().getName()));
            }
        } else {
            throw new DBException(String.format("Could not find update Foreign-key-relation in type \"%s\" since there is no persistable object in the DbId!",
                    getPersistentType().getName()));
        }
    }

    @Override
    public Query getQuery(String queryId) {
        return null;
    }

    protected void deleteChildren(IPersistable father, Class referencedChild) {
        StringBuilder sql = new StringBuilder("DELETE FROM ").append(DbNameHelper.getTableName(referencedChild))
                .append(" WHERE ").append(DbNameHelper.getForeignKeyFieldName(father.getClass()))
                .append("=").append(father.getDbId().getId());
        SQLiteStatement del = getDb().compileStatement(sql.toString());
        del.executeUpdateDelete();
    }

    protected int copyDataRow(IPersistable obj, Cursor csr, SqlDataField[] coll, int colIndex) throws IllegalAccessException, ParseException {
        SqlDataField field = null;
        long referencedObjectId = -1;
        Field referencedObjectField = null;

        int i=0;
        try {
            for (; i < coll.length; ) {
                field = coll[i];

                if (field.isComposition()) {
                    Field composedField = field.getComposeField();
                    IPersister composePersister = getPM().getUnspecificPersister(composedField.getType());
                    IPersistable composedObj = composePersister.createPersistable();
                    Collection<SqlDataField> composeColl = composePersister.getDescription().getTableFields();
                    SqlDataField[] compseAr = new SqlDataField[composeColl.size()];
                    composeColl.toArray(compseAr);
                    int n = copyDataRow(composedObj, csr, compseAr, colIndex);
                    composedField.set(obj, composedObj);

                    colIndex += n;
                    i += n;
                    continue;
                }

                Field fld = field.getObjectField();
                fld.setAccessible(true);
                switch (field.getSqlType()) {
                    case DATE:
                        String dateStr = csr.getString(colIndex);
                        java.text.DateFormat df = new SimpleDateFormat(DATE_FORMAT);
                        Date val = null;
                        if (null != dateStr)
                            val = df.parse(dateStr);
                        fld.set(obj, val);
                        break;
                    case STRING:
                        fld.set(obj, csr.getString(colIndex));
                        break;
                    case BOOLEAN:
                        fld.set(obj, csr.getInt(colIndex) == 1 ? true : false);
                        break;
                    case DOUBLE:
                        fld.set(obj, csr.getDouble(colIndex));
                        break;
                    case FLOAT:
                        fld.set(obj, csr.getFloat(colIndex));
                        break;
                    case INTEGER:
                        fld.set(obj, csr.getInt(colIndex));
                        break;
                    case LONG:
                        fld.set(obj, csr.getLong(colIndex));
                        break;
                    case OBJECT_REFERENCE:
                        referencedObjectId = csr.getLong(colIndex);
                        referencedObjectField = fld;
                        break;
                    case OBJECT_NAME:
                        String objectName = csr.getString(colIndex);
                        IPersistable referencedObj = null;
                        boolean failedLoad = false;
                        if (null != objectName) {
                            if ((referencedObjectId > -1) && (referencedObjectField != null)) {
                                Class clazz = getPM().getPersistentType(objectName);
                                referencedObj = getPM().load(obj.getDbId(), clazz, referencedObjectId);
                                failedLoad = (referencedObj == null);

                                referencedObjectField.set(obj, referencedObj);
                            }
                        }


                        /**
                         * If the referenced object could not be loaded and the relation says it is mandatory
                         * -> This object cannot exist -> delete it and return null
                         */
                        if (failedLoad) {
                            ObjectRelation rel = getDescription().getOneToOneRelation(field.getObjectField().getName());
                            if (rel.isMandatory()) {
                                deleteObjectsWithObsoleteDataref(getPersistentType(), field, referencedObjectField);
                                obj = null;
                            } else {
                                /**
                                 * Mark this object for saving, so the non-existing referenced
                                 * object-reference is updated
                                 */
                                obj.getDbId().setDirty();
                            }
                        }

                        break;
                    case COLLECTION_REFERENCE:
                        int collSize = 0;
                        if (!csr.isNull(colIndex))
                            collSize = csr.getInt(colIndex);

                        //TODO: If object is not lazily loaded omit the proxy-stuff ...
                        //TODO: Get type of objects in collection...
                        ObjectRelation rel = getDescription().getOneToManyRelation(field.getObjectField().getName());
                        Query q = rel.getAndCacheQuery(getPM());
                        ICursorLoader loader;
                        if (null != q) {
                            loader = new QueryCursorLoader(q);
                        } else {
                            loader = getPM().getLoader(getPersistentType(), field.getReferencedType());
                        }
                        Collection lstItems = (Collection) CollectionProxyFactory.getCollectionProxy(getPM(), (Class) field.getReferencedType(), obj, collSize, loader);

                        fld.set(obj, lstItems);
                        break;

                    default:
                        throw new DBException("Unknow data-type");
                }

                colIndex++;
                i++;

                if (colIndex == getDescription().getTableFields().size() + 1)
                    break;

                if (obj == null)
                    break;
            }
        }
        catch (Exception ex) {
            throw new DBException(String.format("Error copying data to type '%s' field '%s'", obj.getClass().getName(), field.getObjectField().getName()), ex);
        }

        return i;
    }

    @Override
	public T rowToObject(int pos, Cursor csr) throws DBException {
		T obj = null;
		SqlDataField currentField = null;

		try {
			obj = createPersistable();

            csr.moveToPosition(pos);
            getPM().assignDbId(obj, csr.getLong(0));

            //Iterate over the first _tableFields.size() columns -> All further columns are foreign-key-fields
            int colIndex = 1;
            SqlDataField[] coll = new SqlDataField[getDescription().getTableFields().size()];
            getDescription().getTableFields().toArray(coll);
            copyDataRow(obj, csr, coll, 1);

            if (null != obj) {
                /**
                 * Save Object if we changed it while loading (non-existing referenced object / not mandatory)
                 */
                if (obj.getDbId().getDirty())
                    getPM().save(obj);

                onActionsLoad(obj);
            }
        } catch (Exception e) {
			if (currentField != null) {
				throw new DBException(
					String.format("Error converting value for Field \"%s\" in object of type \"%s\"",
							currentField.getSqlName(),
							getPersistentType().getName()), e);
			}
			else {
				throw new DBException(
						String.format("Error converting Cursor-row to object of type \"%s\"",
								getPersistentType().getName()), e);
			}
		}

		return obj;
	}

    /**
     * Delete all data-rows in table for persistentType which are not in the table for
     * type field.getReferencedListType.
     *
     * This is for example in a List of items where each item has a one-to-one-reference
     * to another type. And some of those object which were referenced are deleted...
     *
     * @param persistentType
     * @param field
     */
    private void deleteObjectsWithObsoleteDataref(Class<T> persistentType, SqlDataField field, Field refObjField) {
        String tblOther = DbNameHelper.getTableName(field.getObjectField().getType());
        String tblName = DbNameHelper.getTableName(persistentType);
        String fldName = String.format("%s.%s", tblName,
                DbNameHelper.getFieldName(refObjField, SqlDataField.SqlFieldType.OBJECT_REFERENCE));

        StringBuilder sql = new StringBuilder("DELETE FROM ");
        sql.append(tblName)
                .append(" WHERE ")
                .append(fldName)
                .append(" IN (SELECT ")
                .append(fldName).append(" FROM ")
                .append(tblName)
                .append(" LEFT JOIN ")
                .append(tblOther).append(" ON ").append(tblOther)
                .append("._id = ").append(fldName).append(" WHERE ")
                .append(tblOther).append("._id IS NULL)");


        SQLiteStatement del = getDb().compileStatement(sql.toString());
        del.executeUpdateDelete();

        /**
         DELETE FROM tbl_expireditem WHERE tbl_expireditem.fld_expiredstockitem_id IN
         (SELECT tbl_expireditem.fld_expiredstockitem_id FROM tbl_expireditem
         LEFT JOIN tbl_stockitem ON tbl_stockitem._id = tbl_expireditem.fld_expiredstockitem_id
         WHERE tbl_stockitem._id IS NULL)
         */
    }

    @Override
    public T createPersistable() {
        return _factory.create();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("persisted type", getPersistentType().getSimpleName())
                .add("persisted fields", getDescription().getTableFields())
                .toString();
    }

}
