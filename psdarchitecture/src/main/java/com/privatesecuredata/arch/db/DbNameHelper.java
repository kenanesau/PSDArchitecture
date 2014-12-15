package com.privatesecuredata.arch.db;

import java.lang.reflect.Field;
import java.util.Locale;

import com.privatesecuredata.arch.db.SqlDataField.SqlFieldType;

public class DbNameHelper {
	public static String getTableName(Class<?> persistable)
	{
		return String.format("tbl_%s", persistable.getSimpleName().toLowerCase(Locale.US));
	}
	
	public static String getFieldName(Field field, SqlDataField.SqlFieldType type)
	{
		StringBuilder sbl = new StringBuilder("fld_")
			.append(field.getName().toLowerCase(Locale.US));
		
		if (type==SqlFieldType.OBJECT_REFERENCE)
			sbl.append("_id");
		else if (type==SqlFieldType.COLLECTION_REFERENCE)
			sbl.append("_cnt");
		
		return sbl.toString();
	}
	
	public static String getForeignKeyFieldName(Class<?> persistable)
	{
		StringBuilder sbl = new StringBuilder("fld_")
		  .append(getTableName(persistable))
		  .append("_id");
		
		return sbl.toString();
	}
}
