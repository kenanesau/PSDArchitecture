package com.privatesecuredata.arch.db;

import com.privatesecuredata.arch.db.SqlDataField.SqlFieldType;

import java.lang.reflect.Field;
import java.util.Locale;

public class DbNameHelper {
	public static String getTableName(Class<?> persistable)
	{
        return null == persistable ? null : String.format("tbl_%s", persistable.getSimpleName().toLowerCase(Locale.US));
	}

    protected static StringBuilder getSimpleFieldSB(String baseName)
    {
        return new StringBuilder("fld_")
                .append(baseName.toLowerCase(Locale.US));
    }

    public static String getSimpleFieldName(String baseName)
    {
        return getSimpleFieldSB(baseName).toString();
    }

    public static String getFieldName(String baseName, SqlDataField.SqlFieldType type) {
        StringBuilder sbl = null;

        if (type == SqlFieldType.OBJECT_NAME) {
            sbl = new StringBuilder("fld_tpy_");
            sbl.append(baseName);
        }
        else if (type==SqlFieldType.OBJECT_REFERENCE) {
            sbl = getSimpleFieldSB(baseName);
            sbl.append("_id");
        }
        else if (type==SqlFieldType.COLLECTION_REFERENCE) {
            sbl = getSimpleFieldSB(baseName);
            sbl.append("_cnt");
        }
        else {
            sbl = getSimpleFieldSB(baseName);
        }

        return sbl.toString();
    }

	public static String getFieldName(Field field, SqlDataField.SqlFieldType type)
	{
		return getFieldName(field.getName(), type);
	}
	
	public static String getForeignKeyFieldName(Class<?> persistable)
	{
		return getForeignKeyFieldName(getTableName(persistable));
	}

    public static String getForeignKeyFieldName(String tableName)
    {
        StringBuilder sbl = new StringBuilder("fld_")
                .append(tableName)
                .append("_id");

        return sbl.toString();
    }
}
