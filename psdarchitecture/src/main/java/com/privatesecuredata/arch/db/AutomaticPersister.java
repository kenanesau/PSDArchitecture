package com.privatesecuredata.arch.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;
import android.util.Pair;

import com.google.common.base.MoreObjects;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AutomaticPersister<T extends IPersistable> extends AbstractPersister<T> {
    protected SQLiteStatement insert;
    protected SQLiteStatement update;

    public static final String DATE_FORMAT = "yyyy-MM-dd";

    private Constructor<T> _const;
    private PersisterDescription<T> _persisterDesc;

    protected AutomaticPersister() {
    }

    public AutomaticPersister(PersistanceManager pm, Class<T> persistentType) throws Exception {
        setPM(pm);
        setTableName(DbNameHelper.getTableName(persistentType));
        setPersistentType(persistentType);
        Field[] fields = persistentType.getDeclaredFields();
        setConstructor(persistentType.getConstructor((Class<?>[]) null));
        _persisterDesc = new PersisterDescription(getPersistentType());

        DbForeignKeyField anno = persistentType.getAnnotation(DbForeignKeyField.class);
        if (anno != null)
            _persisterDesc.addForeignKeyField(anno);

        DbMultipleForeignKeyFields multipleAnnos = persistentType.getAnnotation(DbMultipleForeignKeyFields.class);
        if (null != multipleAnnos) {
            for (DbForeignKeyField dbAnno : multipleAnnos.value())
                _persisterDesc.addForeignKeyField(dbAnno);
        }

        for (Field field : fields) {
            DbField fieldAnno = field.getAnnotation(DbField.class);
            if (null != fieldAnno)
                _persisterDesc.addSqlField(field, fieldAnno);

            DbThisToOne thisToOneAnno = field.getAnnotation(DbThisToOne.class);
            if (null != thisToOneAnno) {
                field.setAccessible(true);
                ObjectRelation objRel = new ObjectRelation(field, getPersistentType(), thisToOneAnno.deleteChildren());
                _persisterDesc.addOneToOneRelation(objRel);

                // At the moment DbThisToOne-Annotations are always saved in a long-field of the referencing
                // object -> Maybe later: also make it possible to save as a foreign-key in the referenced object.
                SqlDataField idField = new SqlDataField(field);
                _persisterDesc.addSqlField(idField);

                SqlDataField fldTypeName = new SqlDataField(field, field.getType());
                _persisterDesc.addSqlField(fldTypeName);
            }

            // At the moment DbThisToMany-Annotations are always saved as a foreign-key in the table of the referenced object
            DbThisToMany oneToManyAnno = field.getAnnotation(DbThisToMany.class);

            if (null != oneToManyAnno) {
                field.setAccessible(true);
                ObjectRelation objRel = new ObjectRelation(field, oneToManyAnno.referencedType(), getPersistentType(), oneToManyAnno.deleteChildren());
                _persisterDesc.addOneToManyRelation(objRel);

                Class referencedType = oneToManyAnno.referencedType();
                ICursorLoaderFactory fac = new IdCursorLoaderFactory(getPM(), persistentType, referencedType);
                getPM().registerCursorLoaderFactory(fac);

				/**
                 * Add table-field for the collection-proxy-size
                 */
                SqlDataField collectionProxySizeFld = new SqlDataField(field, referencedType);
                _persisterDesc.addProxyCntField(collectionProxySizeFld);
            }
        }
    }

    public PersisterDescription<T> getDesc() { return _persisterDesc; }
    protected void setDesc(PersisterDescription<T> desc) { _persisterDesc = desc; }

    public void extendsPersister(AutomaticPersister<?> parentPersister) {
        _persisterDesc.extend(parentPersister._persisterDesc);
        parentPersister.addExtendingPersister(this);
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
        return new ArrayList<>(_persisterDesc.getTableFields());
    }

    public Map<String, SqlDataField> getFieldMap() {
        return new LinkedHashMap<>(_persisterDesc.getFieldMap());
    }

    /**
     * List of list-Fields to be filled in an persistable and the corresponding types
     */
    public Collection<ObjectRelation> getOneToManyRelations() {
        return _persisterDesc.getOneToManyRelations();
    }

    public Collection<ObjectRelation> getOneToOneRelations() {
        return _persisterDesc.getOneToOneRelations();
    }

    @Override
    public String getSelectAllStatement(OrderByTerm... terms) {
        if (_persisterDesc.getTableFields().isEmpty())
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
        if (_persisterDesc.getTableFields().isEmpty())
            return null;

        StringBuilder sql = new StringBuilder("INSERT INTO ")
                .append(getTableName())
                .append(" ( ");

        int fieldCount = 0;
        for (SqlDataField fld : _persisterDesc.getTableFields()) {
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
        if (_persisterDesc.getTableFields().isEmpty())
            return null;

        StringBuilder sql = new StringBuilder("UPDATE ")
                .append(getTableName())
                .append(" SET ");

        int i = 1;
        for (SqlDataField field : _persisterDesc.getTableFields()) {
            sql.append(field.getSqlName());
            sql.append("=? ");
            if (i < getSqlFields().size())
                sql.append(", ");
            i++;
        }

        sql.append("WHERE _id=?");

        return sql.toString();
    }

    protected String createTriggerStatement(String prefix, Class<?> childType) {
        return new StringBuilder("CREATE TEMP TRIGGER trigger_").append(prefix)
                .append(getTableName()).append("_delete_").append(DbNameHelper.getTableName(childType))
                .append(" AFTER DELETE ON ")
                .append(getTableName())
                .append(" FOR EACH ROW BEGIN DELETE FROM ")
                .append(DbNameHelper.getTableName(childType))
                .append(" WHERE ")
                .append(DbNameHelper.getForeignKeyFieldName(getPersistentType()))
                .append("=OLD._id; END").toString();
    }

    protected Collection<String> createTriggerStatementLst(String prefix, Collection<ObjectRelation> relations) {
        Hashtable<Pair<String, Class>, String> sqlStatements = new Hashtable<>();
        for (ObjectRelation rel : relations) {
            if (rel.deleteChildren()) {
                Class<?> childType = rel.getReferencedListType();
                if (null == childType)
                    childType = rel.getField().getType();
                IPersister persister = getPM().getIPersister(childType);

                if (null == persister)
                    continue;

                if (persister.tableExists()) {
                    Pair key = new Pair<String, Class>(prefix, childType);
                    if (!sqlStatements.contains(key)) {
                        String sqlTrigger = createTriggerStatement(prefix, childType);
                        sqlStatements.put(key, sqlTrigger);
                    }
                }

                List<AutomaticPersister> lst = persister.getExtendingPersisters();
                for (AutomaticPersister autoPersister : lst) {
                    if (autoPersister.tableExists()) {
                        childType = autoPersister.getPersistentType();
                        Pair key = new Pair<String, Class>(prefix, childType);
                        if (!sqlStatements.contains(key)) {
                            String sqlTrigger = createTriggerStatement(prefix, childType);
                            sqlStatements.put(key, sqlTrigger);
                        }
                    }
                }
            }
        }

        return sqlStatements.values();
    }

    protected String[] getTriggerStatements() {
        ArrayList<String> sqlStatements = new ArrayList<>();

        /**
         * Create delete Triggers
         */
        sqlStatements.addAll(createTriggerStatementLst("one_", _persisterDesc.getOneToOneRelations()));
        sqlStatements.addAll(createTriggerStatementLst("lst_", _persisterDesc.getOneToManyRelations()));

        String[] sql = new String[sqlStatements.size()];
        return sqlStatements.toArray(sql);
    }

    /**
     * Creates the SQL-Statement for creating the database
     *
     * @return SQL-Statement (create)
     */
    protected String getCreateStatement() {
        if (_persisterDesc.getTableFields().isEmpty())
            return null;

        /**
         * Create tables
         */
        StringBuilder sql = new StringBuilder("CREATE TABLE ")
                .append(getTableName())
                .append(" ( ")
                .append("_id INTEGER PRIMARY KEY AUTOINCREMENT");

        //Handle normal DB-Fields
        for (SqlDataField field : _persisterDesc.getTableFields()) {
            sql.append(", ")
                    .append(field.getSqlName()).append(" ")
                    .append(field.getSqlTypeString());

            if (field.isMandatory())
                sql.append(" NOT NULL");
        }

        //Add the foreign key-fields and constraints
        for (SqlDataField field : _persisterDesc.getForeignKeyFields()) {
            sql.append(", ")
                    .append(field.getSqlName()).append(" ")
                    .append(field.getSqlTypeString());

            // Foreign-Key Fields do never have a NOT NULL constraint
            // since they are deferred
        }
        for (SqlForeignKeyField field : _persisterDesc.getForeignKeyFields()) {
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
        super.init(obj);

        String insertStatement = getInsertStatement();
        if (null != insertStatement)
            insert = getDb().compileStatement(insertStatement);
        String updateStatement = getUpdateStatement();
        if (null != updateStatement)
            update = getDb().compileStatement(getUpdateStatement());
        if (_persisterDesc.hasForeignKeyFields()) {
            for (SqlForeignKeyField fld : _persisterDesc.getForeignKeyFields()) {
                fld.compileUpdateStatement(getDb());
            }
        }

        String[] triggerSQLs = getTriggerStatements();
        if (null != triggerSQLs) {
            for (String triggerSQL : triggerSQLs) {
                if (null != triggerSQL)
                    getPM().getDb().execSQL(triggerSQL);
            }
        }

        List<SqlDataField> proxyCntFields = _persisterDesc.getAndResetProxyCntFields();
        if (null != proxyCntFields) {
            for (SqlDataField collectionProxySizeFld : proxyCntFields) {
                // Create an update-Statement for the collection-proxy-size
                SQLiteStatement updateListCountStatement = createUpdateListCountStatement(getTableName(), collectionProxySizeFld);
                addUpdateProxyStatement(collectionProxySizeFld.getField(), updateListCountStatement);
            }

            proxyCntFields.clear();
        }
    }

    public void bind(SQLiteStatement sql, int idx, SqlDataField sqlField, T persistable) throws IllegalAccessException, IllegalArgumentException {
        Field fld = sqlField.getField();
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
                java.text.DateFormat df = new SimpleDateFormat(DATE_FORMAT);
                val = fld.get(persistable);
                valStr = null;
                if (null != val)
                    valStr = df.format((Date) val);
                bind(sql, idx, valStr);
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

                bind(sql, idx, DbNameHelper.getDbTypeName(referencedObj.getClass()));
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
            for (SqlDataField field : _persisterDesc.getTableFields()) {
                bind(sql, i++, field, persistable);
            }
        } catch (Exception e) {
            throw new DBException(
                    String.format("Error binding to SQL statement \"%s\"", sql.toString()), e);
        }
    }

    @Override
    public long insert(T persistable) throws DBException {
        // First save the referenced objects
        try {

            for (ObjectRelation rel : _persisterDesc.getOneToOneRelations()) {
                IPersistable other = (IPersistable) rel.getField().get(persistable);
                if (null == other)
                    continue;

                if (other.getDbId() == null ? true : other.getDbId().getDirty())
                    getPM().save(other);
            }
        } catch (Exception ex) {
            throw new DBException(String.format("Error saving one-to-one relation in type \"%s\"!",
                    persistable.getClass().getName()), ex);
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
        bind(update, _persisterDesc.getTableFields().size() + 1, persistable.getDbId().getId());

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
            SqlForeignKeyField fld = _persisterDesc.getForeignKeyField(foreignType);

            if (fld != null) {
                SQLiteStatement updateForeignKey = fld.getUpdateForeingKey();
                updateForeignKey.clearBindings();
                updateForeignKey.bindLong(1, foreignId.getId());
                updateForeignKey.bindLong(2, persistableId.getId());

                int ret = updateForeignKey.executeUpdateDelete();
                if (ret == 0)
                    throw new DBException(String.format("Updating Foreign-key-relation to type \"%s\" in type \"%s\" failed!",
                            foreignType.getName(), getPersistentType().getName()));
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

    @Override
	public T rowToObject(int pos, Cursor csr) throws DBException {
		T obj = null;
		SqlDataField currentField = null;
        long referencedObjectId = -1;
        Field referencedObjectField = null;

		try {
			obj = getConstructor().newInstance();
			csr.moveToPosition(pos);
            getPM().assignDbId(obj, csr.getLong(0));

            //Iterate over the first _tableFields.size() columns -> All further columns are foreign-key-fields
            int colIndex = 1;
            Collection<SqlDataField> coll = _persisterDesc.getTableFields();
            for (SqlDataField field : coll) {
                currentField = field;

                Field fld = field.getField();
                fld.setAccessible(true);
                switch(field.getSqlType())
                {
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
                        fld.set(obj, csr.getInt(colIndex)==1 ? true : false);
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
                        if (null != objectName) {
                            if ((referencedObjectId > -1) && (referencedObjectField != null)) {
                                Class clazz = getPM().getPersistentType(objectName);
                                IPersistable referencedObj = getPM().load(obj.getDbId(), clazz, referencedObjectId);
                                referencedObjectField.set(obj, referencedObj);
                            }
                        }

                        referencedObjectId = -1;
                        referencedObjectField = null;

                        break;
                    case COLLECTION_REFERENCE:
                        int collSize = 0;
                        if (!csr.isNull(colIndex))
                            collSize = csr.getInt(colIndex);

                        //TODO: If object is not lazily loaded omit the proxy-stuff ...
                        //TODO: Get type of objects in collection...
                        ICursorLoader loader = getPM().getLoader(getPersistentType(), field.getReferencedType());
                        Collection lstItems = CollectionProxyFactory.getCollectionProxy(getPM(), (Class)field.getReferencedType(), obj, collSize, loader);

                        fld.set(obj, lstItems);
                        break;

                    default:
                        throw new DBException("Unknow data-type");
                }

                colIndex++;

                if (colIndex ==  _persisterDesc.getTableFields().size() + 1)
                    break;

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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("persisted type", getPersistentType().getSimpleName())
                .add("persisted fields", _persisterDesc.getTableFields())
                .toString();
    }

}
