package com.privatesecuredata.arch.db;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.privatesecuredata.arch.db.annotations.DbField;
import com.privatesecuredata.arch.db.annotations.DbForeignKeyField;
import com.privatesecuredata.arch.db.annotations.DbThisToMany;
import com.privatesecuredata.arch.db.annotations.DbThisToOne;
import com.privatesecuredata.arch.exceptions.ArgumentException;
import com.privatesecuredata.arch.exceptions.DBException;

public class AutomaticPersister<T extends IPersistable<T>> extends AbstractPersister<T> {
	
	SQLiteStatement insert;
	SQLiteStatement update;
	
	private String _tableName;
	private List<SqlDataField> _tableFields;
	
	/**
	 * ID-Fields in the table of the current persister which point to rows in other tables
	 * (Counter-part of an OneToMany- or an OneToOne-Relation of another Persister 
	 */
	private Hashtable<Class<?>, SqlForeignKeyField> _foreignKeyFields;
	
	/**
	 * List of Fields to be filled in an persistable and the corresponding types 
	 */
	private List<ObjectRelation> _lstOneToOneRelations;
	
	/**
	 * List of list-Fields to be filled in an persistable and the corresponding types 
	 */
	private List<ObjectRelation> _lstOneToManyRelations;
	
	/**
	 * Constructor of the Persistable for which this Persister is used for
	 */
	Constructor<T> _const;
	
	/**
	 * Type of the Persistable which is persited with this persister
	 */
	Class<T> _persistentType;
	
	
	public AutomaticPersister(PersistanceManager pm, Class<T> persistentType) throws Exception
	{
		_tableName = DbNameHelper.getTableName(persistentType);
		_persistentType = persistentType;
		Field[] fields = persistentType.getDeclaredFields();
		_const = persistentType.getConstructor((Class<?>[])null);
		_tableFields = new ArrayList<SqlDataField>();
		_lstOneToOneRelations = new ArrayList<ObjectRelation>();
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
			
			DbThisToOne oneToOneAnno = field.getAnnotation(DbThisToOne.class);
			if (null != oneToOneAnno) {
				field.setAccessible(true);
				_lstOneToOneRelations.add(new ObjectRelation(field));
				
				// At the moment DbThisToOne-Annotations are always saved in a long-field of the referencing
				// object -> Maybe later: also make it possible to save as a foreign-key in the referenced object.
				_tableFields.add(new SqlDataField(field));
			}
			
			// At the moment DbThisToMany-Annotations are always saved as a foreign-key in the table of the referenced object
			DbThisToMany oneToManyAnno = field.getAnnotation(DbThisToMany.class);
			if (null != oneToManyAnno) {
				field.setAccessible(true);
				_lstOneToManyRelations.add(new ObjectRelation(field));
				
				Class<?> referencedType = oneToManyAnno.referencedType();
				ICursorLoader loader = new IdCursorLoader(getPM(), persistentType, referencedType);
				getPM().registerCursorLoader(persistentType, referencedType, loader);
				
				// Add table-field for the collection-proxy-size
				_tableFields.add(new SqlDataField(field));
			}
		}

		if (_tableFields.isEmpty())
			throw new Exception(String.format("No DBField-annotation found in type \"%s\"", persistentType.getName()));
	}
	
	protected void addSqlField(Field field, DbField anno) 
	{
		SqlDataField sqlField = new SqlDataField(field); 
		if (anno.isMandatory())
			sqlField.setMandatory();
		
		_tableFields.add(sqlField);
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
		for(SqlDataField fld : _tableFields )
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
		for (SqlDataField field : _tableFields)
		{
			sql.append(field.getName());
			sql.append("=? ");
			if (i < _tableFields.size())
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
		for (SqlDataField field : _tableFields)
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
		
		insert = getDb().compileStatement(getInsertStatement());
		update = getDb().compileStatement(getUpdateStatement());
		for(SqlForeignKeyField fld : _foreignKeyFields.values())
		{
			fld.compileUpdateStatement(getDb());
		}
	}
	
	public void bind(SQLiteStatement sql, int idx, SqlDataField sqlField, T persistable) throws IllegalAccessException, IllegalArgumentException
	{
		Field fld = sqlField.getField();
		if (null != fld)
		{
			fld.setAccessible(true);
			
			switch(sqlField.getSqlType())
			{
			case STRING:
			case DATE:
				Object val = fld.get(persistable);
				String valStr = null;
				if (null != val)
					valStr = val.toString();
				
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
			case REFERENCE:
				IPersistable<?> referencedObj = (IPersistable<?>) fld.get(persistable);
				if (null == referencedObj)
					break;
				
				DbId<?> dbId = referencedObj.getDbId();
				if (null == dbId)
					throw new DBException(String.format("Unable to safe reference from object of type \"%s\" to object of type \"%s\" since this object is not persistent yet!",
							persistable.getClass().getName(), referencedObj.getClass().getName()));
							
				bind(sql, idx, referencedObj.getDbId().getId());
				break;
			default:
				break;
			}
		}
		else
		{
			
		}
	}
	
	public void bind(SQLiteStatement sql, T persistable)
	{
		sql.clearBindings();
		
		int i=1;
		try {
			for (SqlDataField field : _tableFields) {
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
			
			for(ObjectRelation rel : _lstOneToOneRelations)
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
		
		if ( (_lstOneToOneRelations.size() > 0) ||
		     (_lstOneToManyRelations.size() > 0) )
			dbId = getPM().assignDbId(persistable, id);
			
		try {
			
			for(ObjectRelation rel : _lstOneToManyRelations)
			{
				List<?> others = (List<?>) rel.getField().get(persistable);
				if (null == others)
					continue;
				
				for(Object other : others)
				{
					getPM().saveAndUpdateForeignKey((IPersistable<?>)other, dbId);
				}
			}
		}
		catch (Exception ex)
		{
			throw new DBException(String.format("Error saving one-to-many relation in type \"%s\"!", 
					persistable.getClass().getName()));
		}
		
		return id;
	}

	@Override
	public void update(T persistable) throws DBException {
		bind(update, persistable);
		bind(update, _tableFields.size()+1, persistable.getDbId().getId());
		
		int rowsAffected = update.executeUpdateDelete();
		if (rowsAffected==0)
			throw new DBException(String.format("Update of \"%s\" was not successful", persistable.getClass().getName()));
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
							foreignType.getName(), _persistentType.getName()));
			}
			else 
			{
				throw new DBException(String.format("Could not find Foreign-key-relation to type \"%s\" in type \"%s\"",
						foreignType.getName(), _persistentType.getName())); 
			}
		}
	}

	@Override
	public T rowToObject(int pos, Cursor csr) throws DBException {
		T obj = null;
		SqlDataField field = null;
		
		try {
			obj = _const.newInstance();
			csr.moveToPosition(pos);
			
			obj.setDbId(new DbId<T>(csr.getLong(0)));
			
			//Iterate over the first _tableFields.size() columns -> All further columns are foreign-key-fieds
			for (int colIndex=1; colIndex<_tableFields.size() + 1; colIndex++)
			{
				field = _tableFields.get(colIndex - 1);
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
				case REFERENCE:
					long referencedObjectId = csr.getLong(colIndex);
					IPersistable referencedObj = getPM().load(obj.getDbId(), (Class)fld.getType(), referencedObjectId);
					fld.set(obj, referencedObj);
					break;
				case COLLECTION_PROXY_SIZE:
					//TODO: Implement me (create a proxy for the collection....)
					break;
				default:
					throw new DBException("Unknow data-type");
				}
			}
			
//			for (ObjectRelation rel : _lstOneToManyRelations)
//			{
//				Cursor cursor = getPM().getCursor(rel.getType());
//				
//				
//			}
		} catch (Exception e) {
			if (field != null) {
				throw new DBException(
					String.format("Error converting value for Field \"%s\" in object of type \"%s\"", 
							field.getName(),
							_persistentType.getName()), e);
			}
			else {
				throw new DBException(
						String.format("Error converting Cursor-row to object of type \"%s\"", 
								_persistentType.getName()), e);
			}
		} 
		
		return obj;
	}


}
