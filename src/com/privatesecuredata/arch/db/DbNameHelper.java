package com.privatesecuredata.arch.db;

import java.lang.reflect.Field;
import java.util.Locale;

public class DbNameHelper {
	public static String getTableName(Class<?> persistable)
	{
		return String.format("tbl_%s", persistable.getSimpleName().toLowerCase(Locale.US));
	}
	
	public static String getFieldName(Field field)
	{
		return String.format("fld_%s", field.getName().toLowerCase(Locale.US));
	}
	
	public static String getForeignKeyFieldName(Class<?> persistable)
	{
		StringBuilder sbl = new StringBuilder("fld_")
		  .append(getTableName(persistable))
		  .append("_id");
		
		return sbl.toString();
	}
}
