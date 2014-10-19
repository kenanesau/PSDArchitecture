package com.privatesecuredata.arch.db;

import java.util.Locale;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class SqlForeignKeyField extends SqlDataField {
	
	public class ForeignkeyKey
	{
		public Class<?> foreignType;
		public String   fieldName;
		
		@Override
		public int hashCode() {
			return Objects.hashCode(foreignType, fieldName);
		}
		
		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o instanceof SqlForeignKeyField.ForeignkeyKey) {
				ForeignkeyKey that = (SqlForeignKeyField.ForeignkeyKey) o;
				return Objects.equal(this.foreignType, that.foreignType) &&
					   Objects.equal(this.fieldName, that.fieldName);
			}
			else {
				return false;
			}
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.add("Type", foreignType.getName())
					.add("Name", fieldName)
					.toString();
		}
	}
	
	private Class<?> _foreignKeyType;
	SQLiteStatement _updateForeignKey;

	public SqlForeignKeyField(String tableName, Class<?> foreignKeyType)
	{
		setSqlType(SqlType.LONG);
		setTableName(tableName);
		String name = String.format("tbl_%s_id", foreignKeyType.getSimpleName().toLowerCase(Locale.US));
		setName(name);
		setForeingKeyType(foreignKeyType);
	}
	
	private void setForeingKeyType(Class<?> foreignKeyType) {
		_foreignKeyType = foreignKeyType;
	}

	public Class<?> getForeignKeyType()
	{
		return _foreignKeyType;
	}
	
	public void compileUpdateStatement(SQLiteDatabase db)
	{
		StringBuilder str = new StringBuilder("UPDATE ");
		str.append(getTableName())
			.append(" SET ")
			.append(getName()).append("=? WHERE _id=?");
		
		_updateForeignKey = db.compileStatement(str.toString());
	}

	public SqlForeignKeyField.ForeignkeyKey createHashtableKey() {
		ForeignkeyKey key = new ForeignkeyKey();
		key.fieldName   = getName();
		key.foreignType = getForeignKeyType();
		return key;
	}
	
	public SQLiteStatement getUpdateForeingKey() {
		return _updateForeignKey;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("SqlDataField", super.toString())
				.add("ForeignKeyType", _foreignKeyType.getName())
				.toString();
	}
}
