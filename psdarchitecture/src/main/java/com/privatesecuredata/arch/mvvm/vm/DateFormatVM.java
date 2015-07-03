package com.privatesecuredata.arch.mvvm.vm;

import java.text.DateFormat;
import java.util.Date;

/**
 * This is a simple VM which can be used to format dates
 *
 * Each time the inner VM changes it notifies its listeners too
 * which results in an UI-update...
 */
public class DateFormatVM extends SimpleValueVM<String> {
    private SimpleValueVM<Date> _valueVM;
    private DateFormat _format;

    /**
     * Constructor
     *
     * @param format       Dateformat
     * @param valueVM      Value-parameter(s) for the format-string
     */
    public DateFormatVM(DateFormat format, SimpleValueVM<Date> valueVM) {
        super("");
        this._valueVM = valueVM;
        this._format = format;
        if (null != _valueVM) {
            valueVM.addViewModelListener(this);
        }
    }

    @Override
    public String get() {
        String str = null;
        if (null != _valueVM) {
            Date date = _valueVM.get();
            str = date != null ? _format.format(_valueVM.get()) : "";
        }
        else {
            str = super.get();
        }

        return str;
    }

}
