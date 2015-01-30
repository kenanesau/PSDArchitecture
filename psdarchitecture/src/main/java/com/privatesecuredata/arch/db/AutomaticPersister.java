package com.privatesecuredata.arch.db;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import com.google.common.base.MoreObjects;
import com.privatesecuredata.arch.db.annotations.DbField;
import com.privatesecuredata.arch.db.annotations.DbForeignKeyField;
import com.privatesecuredata.arch.db.annotations.DbThisToMany;
import com.privatesecuredata.arch.db.annotations.DbThisToOne;
import com.privatesecuredata.arch.exceptions.DBException;

public class AutomaticPersister<T extends IPersistable<T>> extends AbstractPersister<T> {
	
	SQLiteStatement insert;
	SQLiteStatement update;
	
	private String _tableName;
	private List<SqlDataField> _tableFields = new ArrayList<SqlDataField>();
	
	/**
	 * ID-Fields in the table of the current persister which point to rows in other tables
	 * (Counter-part of an OneToMany- or an OneToOne-Relation of another Persister 
	 */
	private Hashtable<Class<?>, SqlForeignKeyField> _foreignKeyFields;
	
	/**
	 * List of Fields to be filled in an persistable and the corresponding types 
	 */
	private List<ObjectRelation> _lstThisToOneRelations;
	
	private List<ObjectRelation> _lstOneToManyRelations;
	
	private Constructor<T> _const;
	
	private Class<T> _persistentType;

    protected AutomaticPersister() {}

    public AutomaticPersister(PersistanceManager pm, Class<T> persistentType) throws Exception
	{
		_tableName = DbNameHelper.getTableName(persistentType);
		setPersistentType(persistentType);
		Field[] fields = persistentType.getDeclaredFields();
		setConstructor(persistentType.getConstructor((Class<?>[])null));
		_lstThisToOneRelations = new ArrayList<ObjectRelation>();
		_lstOneToManyRelations = new ArrayList<ObjectRelation>();
		_foreignKeyFields = new Hashtable<Class<?>, SqlForeignKeyField>();
		
		setPM(pm);
		Annotation[] annos = persistentType.getAnnotations();
		for(Annotation anno : annos)
		{
			if (anno instanceof DbForeignKeyField)
			{
				DbForeignKeyField dbAnno = (DbForeignKeyField) anno;
				Class<?> foreignKeyType = dbAnno.foreignType();
				
				SqlForeignKeyField sqlForeignKeyField = new SqlForeignKeyField(_tableName, foreignKeyType); 
				if (dbAnno.isMandatory())
					sqlForeignKeyField.setMandatory();
				Class<?> key = foreignKeyType; //sqlForeignKeyField.createHashtableKey();
				_foreignKeyFields.put(key, sqlForeignKeyField);
			}
		}
		
		for (Field field : fields)
		{			
			DbField fieldAnno = field.getAnnotation(DbField.class);
			if (null != fieldAnno)
				addSqlField(field, fieldAnno);
			
			DbThisToOne thisToOneAnno = field.getAnnotation(DbThisToOne.class);
			if (null != thisToOneAnno) {
				field.setAccessible(true);
                ObjectRelation objRel = new ObjectRelation(field, getPersistentType());
				_lstThisToOneRelations.add(objRel);
				
				// At the moment DbThisToOne-Annotations are always saved in a long-field of the referencing
				// object -> Maybe later: also make it possible to save as a foreign-key in the referenced object.
                SqlDataField idField = new SqlDataField(field);
                addSqlField(idField);
                String fldName = String.format("fld_tpy_%s", field.getName());
                SqlDataField fldTypeName = new SqlDataField(fldName, SqlDataField.SqlFieldType.OBJECT_NAME);
                fldTypeName.setField(idField.getField());
                addSqlField(fldTypeName);
			}
			
			// At the moment DbThisToMany-Annotations are always saved as a foreign-key in the table of the referenced object
			DbThisToMany oneToManyAnno = field.getAnnotation(DbThisToMany.class);
			if (null != oneToManyAnno) {
				field.setAccessible(true);
				getOneToManyRelations().add(new ObjectRelation(field, getPersistentType()));
				
				Class<?> referencedType = oneToManyAnno.referencedType();
				ICursorLoader loader = new IdCursorLoader(getPM(), persistentType, referencedType);
				getPM().registerCursorLoader(persistentType, referencedType, loader);
				
				// Add table-field for the collection-proxy-size
				addSqlField(new SqlDataField(field, referencedType));
			}
		}

		if (getTableFieldsInternal().isEmpty())
			throw new Exception(String.format("No DBField-annotation found in type \"%s\"", persistentType.getName()));
	}

    protected List<SqlDataField> getTableFieldsInternal() {
        return _tableFields;
    }

    protected void setTableFieldsInternal(List<SqlDataField> _tableFields) {
        this._tableFields = _tableFields;
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

    /**
     * Type of the Persistable which is persited with this persister
     */
    protected Class<T> getPersistentType() {
        return _persistentType;
    }

    protected void setPersistentType(Class<T> _persistentType) {
        this._persistentType = _persistentType;
    }

    protected List<SqlDataField> getSqlFields() { return new ArrayList<SqlDataField>(getTableFieldsInternal()); }

    protected void addSqlField (SqlDataField sqlField)
    {
        getTableFieldsInternal().add(sqlField);
    }

    protected void addSqlField(Field field, DbField anno)
	{
		SqlDataField sqlField = new SqlDataField(field);
        if (anno.isMandatory())
            sqlField.setMandatory();
        if (!anno.id().equals(""))
            sqlField.setId(anno.id());

        addSqlField(sqlField);
	}


    /**
     * List of list-Fields to be filled in an persistable and the corresponding types
     */
    public List<ObjectRelation> getOneToManyRelations() {
        return _lstOneToManyRelations;
    }
	
	@Override
	public String getTableName() {
		return _tableName;
	}

	/**
	 * Creates the SQL-Statement for inserting a new row to the database
	 * 
	 * @return SQL-Statement (insert)
	 */
	protected String getInsertStatement()
	{
		StringBuilder sql = new StringBuilder("INSERT INTO ")
			.append(getTableName())
			.append(" ( ");
		
		int fieldCount = 0;
		for(SqlDataField fld : getTableFieldsInternal())
		{
			if (fieldCount > 0)
				sql.append(", ");
			
			sql.append(fld.getName());
			
			fieldCount++;
		}
		
		sql.append(" ) VALUES ( ");
		
		for (int i = 0; i < fieldCount; i++)
		{
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
	protected String getUpdateStatement()
	{
		StringBuilder sql = new StringBuilder("UPDATE ")
			.append(getTableName())
			.append(" SET ");
		
		int i = 1;
		for (SqlDataField field : getTableFieldsInternal())
		{
			sql.append(field.getName());
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
	protected String getCreateStatement()
	{
		StringBuilder sql = new StringBuilder("CREATE TABLE ")
			.append(getTableName())
			.append(" ( ")
			.append("_id INTEGER PRIMARY KEY AUTOINCREMENT");
		
		//Handle normal DB-Fields
		for (SqlDataField field : getTableFieldsInternal())
		{
			sql.append(", ")
				.append(field.getName()).append(" ")
				.append(field.getSqlTypeString()).append(" ");
			
			if (field.isMandatory())
				sql.append("NOT NULL");
		}
		
		//Add the foreign key-fields and constraints
		for (SqlDataField field : _foreignKeyFields.values())
		{
			sql.append(", ")
				.append(field.getName()).append(" ")
				.append(field.getSqlTypeString()).append(" ");
			
			// Foreign-Key Fields do never have a NOT NULL constraint
			// since they are deferred
		}
		for (SqlForeignKeyField field : _foreignKeyFields.values())
		{
			Class<?> cls = field.getForeignKeyType();
			if ( ( field.isMandatory()) && (null != cls) )
			{
				sql.append(",\nFOREIGN KEY(")
					.append(field.getName()).append(") REFERENCES ")
					.append(cls.getSimpleName()).append("(_id) DEFERRABLE INITIALLY DEFERRED");
			}
		}
		
		sql.append(" ) ");

		return sql.toString();
	}
	
	@Override
	public void init(Object obj) throws DBException {
		super.init(obj);
		setPM((PersistanceManager)obj);

        String insertStatement = getInsertStatement();
        if (null != insertStatement)
		    insert = getDb().compileStatement(insertStatement);
        String updateStatement = getUpdateStatement();
        if (null != updateStatement)
		    update = getDb().compileStatement(getUpdateStatement());
        if (null != _foreignKeyFields) {
            for (SqlForeignKeyField fld : _foreignKeyFields.values()) {
                fld.compileUpdateStatement(getDb());
            }
        }
	}
	
	public void bind(SQLiteStatement sql, int idx, SqlDataField sqlField, T persistable) throws IllegalAccessException, IllegalArgumentException
	{
		Field fld = sqlField.getField();
        if (null != fld)
            fld.setAccessible(true);

        switch(sqlField.getSqlType())
        {
        case STRING:
            Object val = fld.get(persistable);
            String valStr = null;
            if (null != val)
                valStr = val.toString();
            bind(sql, idx, valStr);
            break;
        case DATE:
            java.text.DateFormat df = java.text.DateFormat.getDateTimeInstance();
            val = fld.get(persistable);
            valStr = null;
            if (null != val)
                valStr = df.format((Date)val);
            bind(sql, idx, valStr);
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
            IPersistable<?> referencedObj = (IPersistable<?>) fld.get(persistable);
            if (null == referencedObj)
            {
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
            referencedObj = (IPersistable<?>) fld.get(persistable);
            if (null == referencedObj) {
                bindNull(sql, idx);
                break;
            }

            bind(sql, idx, referencedObj.getClass().getName());
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
	
	public void bind(SQLiteStatement sql, T persistable)
	{
		sql.clearBindings();
		
		int i=1;
		try {
			for (SqlDataField field : getTableFieldsInternal()) {
				bind(sql, i++, field, persistable);
			}
		} 
		catch (Exception e) 
		{
			throw new DBException(
					String.format("Error binding to SQL statement \"%s\"", sql.toString()), e);
		}
	}

	@Override
	public long insert(T persistable) throws DBException 
	{
		// First save the referenced objects 
		try {
			
			for(ObjectRelation rel : _lstThisToOneRelations)
			{
				IPersistable other = (IPersistable) rel.getField().get(persistable);
				if (null == other)
					continue;
				
				getPM().save(other);
			}
		}
		catch (Exception ex)
		{
			throw new DBException(String.format("Error saving one-to-one relation in type \"%s\"!", 
					persistable.getClass().getName()));
		}
		
		bind(insert, persistable);
		
		long id = insert.executeInsert();
		DbId<?> dbId = null;
		
		if ( (_lstThisToOneRelations.size() > 0) ||
		     (getOneToManyRelations().size() > 0) )
			dbId = getPM().assignDbId(persistable, id);

        saveAndUpdateForeignKeyRelations(persistable, dbId);

        return id;
	}

    private void saveAndUpdateForeignKeyRelations(T persistable, DbId<?> dbId) {
        try {

            for(ObjectRelation rel : getOneToManyRelations())
            {
                Collection<?> others = (Collection<?>) rel.getField().get(persistable);
                if (null == others)
                    continue;
                if(Proxy.isProxyClass(others.getClass()))
                {
                    LazyCollectionInvocationHandler handler = (LazyCollectionInvocationHandler)Proxy.getInvocationHandler(others);
                    /**
                     * If there was no change -> skip it
                     */
                    if (!handler.isLoaded())
                        continue;
                }

                for(Object other : others)
                {
                    getPM().saveAndUpdateForeignKey((IPersistable<?>)other, dbId);
                }
            }
        }
        catch (Exception ex)
        {
            throw new DBException(String.format("Error saving one-to-many relation in type \"%s\"!",
                    persistable.getClass().getName()), ex);
        }
    }

    @Override
	public long update(T persistable) throws DBException {
		bind(update, persistable);
		bind(update, getTableFieldsInternal().size()+1, persistable.getDbId().getId());
		
		int rowsAffected = update.executeUpdateDelete();

        DbId<T> dbId = persistable.getDbId();
        saveAndUpdateForeignKeyRelations(persistable, dbId);

		return rowsAffected;
	}

	@Override
	public void updateForeignKey(T persistable, DbId<?> foreignId)
			throws DBException {
		IPersistable<?> foreignObj = foreignId.getObj();
		if (null != foreignObj)
		{
			Class<?> foreignType = foreignObj.getClass();
			SqlForeignKeyField fld = _foreignKeyFields.get(foreignType);
			
			if (fld != null)
			{
				SQLiteStatement updateForeignKey = fld.getUpdateForeingKey();
				updateForeignKey.clearBindings();
				updateForeignKey.bindLong(1, foreignId.getId());
				updateForeignKey.bindLong(2, persistable.getDbId().getId());
				
				int ret = updateForeignKey.executeUpdateDelete();
				if (ret == 0)
					throw new DBException(String.format("Updating Foreign-key-relation to type \"%s\" in type \"%s\" failed!",
							foreignType.getName(), getPersistentType().getName()));
			}
			else 
			{
				throw new DBException(String.format("Could not find Foreign-key-relation to type \"%s\" in type \"%s\"",
						foreignType.getName(), getPersistentType().getName()));
			}
		}
        else
        {
            throw new DBException(String.format("Could not find update Foreign-key-relation in type \"%s\" since there is no persistable object in the DbId!",
                    getPersistentType().getName()));
        }
	}

	@Override
	public T rowToObject(int pos, Cursor csr) throws DBException {
		T obj = null;
		SqlDataField field = null;
        long referencedObjectId = -1;
        Field referencedObjectField = null;

		try {
			obj = getConstructor().newInstance();
			csr.moveToPosition(pos);
            getPM().assignDbId(obj, csr.getLong(0));

            //Iterate over the first _tableFields.size() columns -> All further columns are foreign-key-fieds
			for (int colIndex=1; colIndex< getTableFieldsInternal().size() + 1; colIndex++)
			{
				field = getTableFieldsInternal().get(colIndex - 1);
				//int colIndex = csr.getColumnIndex(field.getName());

				Field fld = field.getField();
				fld.setAccessible(true);
				switch(field.getSqlType())
				{
				case DATE:
					String dateStr = csr.getString(colIndex);
					java.text.DateFormat df = java.text.DateFormat.getDateTimeInstance();
					Date val = null;
					if (null != dateStr)
						val = df.parse(dateStr);
					fld.set(obj, val);
					break;
				case STRING:
					fld.set(obj, csr.getString(colIndex));
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
			}

        } catch (Exception e) {
			if (field != null) {
				throw new DBException(
					String.format("Error converting value for Field \"%s\" in object of type \"%s\"",
							field.getName(),
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
                .add("persisted fields", getTableFieldsInternal())
                .toString();
    }

}
