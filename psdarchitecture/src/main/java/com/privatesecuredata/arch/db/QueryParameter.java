package com.privatesecuredata.arch.db;

/**
 * Created by kenan on 1/29/16.
 */
public class QueryParameter {
    private String id;
    private String fieldName;
    private Object value;

    public QueryParameter(String paraId, String paraField) {
        this.id = paraId;
        this.fieldName = paraField;
    }

    public String id() { return  id; }
    public String fieldName() { return fieldName; }
    public void setValue(Object obj) { this.value = obj; }
    public Object value() { return value; }
    public QueryParameter clone() {
        QueryParameter newPara = new QueryParameter(this.id, this.fieldName);
        newPara .value = this.value;

        return newPara;
    }

}
