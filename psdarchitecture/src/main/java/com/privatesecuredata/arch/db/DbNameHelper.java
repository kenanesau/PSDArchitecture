package com.privatesecuredata.arch.db;

import com.privatesecuredata.arch.db.SqlDataField.SqlFieldType;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DbNameHelper {
    public static final String DATE_FORMAT = "yyyy-MM-dd";

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
	
	public static String getForeignKeyFieldName(Class<?> persistableType)
	{
		return getForeignKeyFieldName(getTableName(persistableType));
	}

    public static String getForeignKeyFieldName(String tableName)
    {
        StringBuilder sbl = new StringBuilder("fld_")
                .append(tableName)
                .append("_id");

        return sbl.toString();
    }

    /**
     * Convert a type to its DB-string representation
     *
     * @param persistentType
     * @return String-representation of type-name (e.g
     * FoodStockItemDetails.class -> "com.privatesecuredata.psdstoremanagerentities.FoodStockItemDetails")
     */
    public static  String getDbTypeName(Class persistentType) {
        String dbTypeName;
        String typeName = persistentType.getName();
        String[] tokens = typeName.split("[.]");
        int version = 0;
        if (tokens.length > 2)
        {
            StringBuilder strBuilder = new StringBuilder();
            for (int i=0; i<tokens.length; i++)
            {
                if (i == tokens.length - 2) {
                    if (tokens[i].matches("^[vV]\\d+"))
                        continue;
                }

                strBuilder.append(tokens[i]);

                if (i < tokens.length - 1)
                    strBuilder.append(".");
            }

            dbTypeName = strBuilder.toString();
        }
        else {
            dbTypeName = typeName;
        }

        return dbTypeName;
    }

    public static String getDbDateString(Date val)
    {
        java.text.DateFormat df = new SimpleDateFormat(DATE_FORMAT);
        String valStr = null;
        if (null != val)
            valStr = df.format((Date) val);

        return valStr;
    }
}
