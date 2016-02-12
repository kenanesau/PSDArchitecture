package com.privatesecuredata.arch.db.query;

/**
 * The QueryParameter holds the actual value which will be put into the SQL-query.
 */
public class QueryParameter {
    private String id;
    private String fieldName;
    private Object value;
    private IQueryParamValueFilter filter = null;

    public QueryParameter(String paraId, String paraField) {
        this.id = paraId;
        this.fieldName = paraField;
    }

    public String id() { return  id; }

    /**
     * @return Name of the field like it is used in the Object
     */
    public String fieldName() { return fieldName; }

    /**
     * @param obj New Value for the parameter
     */
    public void setValue(Object obj) {
        this.value = filter == null ? obj : filter.filterValue(obj);
    }

    /**
     * @return The value of the parameter
     */
    public Object value() { return value; }

    /**
     * Creates a clone of the QueryParameter
     *
     * @return an exact deep Copy
     */
    public QueryParameter clone() {
        QueryParameter newPara = new QueryParameter(this.id, this.fieldName);
        newPara.value = this.value;
        newPara.filter = this.filter;

        return newPara;
    }

    /**
     * Set an Callback-Interface of type IQueryParamValueFilter which will be called
     * each time someone calls setValue on this QueryParameter-instance.
     *
     * By implementing this callback you can modify the value which is saved to the
     * QueryParameter-instance
     *
     * @param filter Callback-Interface
     * @sa IQueryParamValueFilter
     */
    public void setValueFilter(IQueryParamValueFilter filter) { this.filter = filter; }

}
