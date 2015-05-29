package com.privatesecuredata.arch.mvvm.vm;

/**
 * This is a simple VM which can be used to format strings around other
 * SimpleValueVMs.
 *
 * Each time a VM changes the StringFormatVM notifies its listeners too
 * which results in an UI-update...
 */
public class StringFormatVM extends SimpleValueVM<String> {
    private SimpleValueVM[] _valueVMs;

    /**
     * Constructor
     *
     * @param formatString format-string
     * @param valueVM      Value-parameter(s) for the format-string
     */
    public StringFormatVM(String formatString, SimpleValueVM... valueVM) {
        super(formatString);

        this._valueVMs = valueVM;
        if (null != _valueVMs) {
            for (SimpleValueVM valVM : _valueVMs)
                valVM.addViewModelListener(this);
        }
    }

    @Override
    public String get() {
        String str = null;
        if (null != _valueVMs) {
            Object[] objs = new Object[_valueVMs.length];
            for (int i = 0; i < objs.length; i++) {
                objs[i] = _valueVMs[i].get();
            }

            String format = super.get();
            str = String.format(format, objs);
        }
        else {
            str = super.get();
        }

        return str;
    }

}
