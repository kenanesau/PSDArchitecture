package com.privatesecuredata.arch.mvvm.android;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.privatesecuredata.arch.mvvm.binder.TransientViewToVmBinder;
import com.privatesecuredata.arch.mvvm.vm.ComplexViewModel;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

/**
 * Simplified ListAdapter which can be used to directly embed a List<COMPLEXVM> in a
 * ListView without the need for a real ListViewModel
 *
 * @param <COMPLEXVM>
 * @sa MVVMComplexVmAdapterTemplate
 */
public class ViewModelListAdapter<COMPLEXVM extends ComplexViewModel> extends BaseAdapter {

    private List<COMPLEXVM> data;
    private final MVVMActivity ctx;
    private List<MVVMComplexVmAdapter> viewHolders = new LinkedList<>();
    private Hashtable<Integer, TransientViewToVmBinder<?>> view2ModelAdapters = new Hashtable<Integer, TransientViewToVmBinder<?>>();
    private List<ViewManipulator> manipulators = new ArrayList<>();
    private MVVMComplexVmAdapterTemplate adapterTemplate;

    /**
     * id of the Row-layout
     */
    private int rowViewId = -1;

    public ViewModelListAdapter(MVVMComplexVmAdapterTemplate adapterTemplate, List<COMPLEXVM> data,
                                MVVMActivity ctx) {

        this.data = data;
        this.ctx = ctx;
        this.adapterTemplate = adapterTemplate;
    }

    public void setData(List<COMPLEXVM> lst) {
        this.data = lst;
        notifyDataSetChanged();
    }

    public void setRowViewId(int _rowViewId) {
        this.rowViewId = _rowViewId;
    }

    protected int getRowViewId() { return this.rowViewId; }

    @Override
    public int getCount() {
        return (null == data) ? 0 : data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View rowView = convertView;
        COMPLEXVM vm = data.get(position);

        if (null == rowView)
        {
            LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(getRowViewId(), parent, false);
            MVVMComplexVmAdapter holder;
            holder = new MVVMComplexVmAdapter<COMPLEXVM>(ctx, adapterTemplate, rowView, vm);
            viewHolders.add(holder);

            rowView.setTag(holder);
        }

        Object obj = rowView.getTag();
        if (null != obj) {
            if (obj instanceof MVVMComplexVmAdapter) {
                MVVMComplexVmAdapter viewHolder = (MVVMComplexVmAdapter)obj;
                viewHolder.updateViewModel(vm);
                viewHolder.updateView();
            }
        }


        /** Search for View-IDs of the views to manipulate and register them with the viewholder **/
        for (ViewManipulator manipulator : manipulators)
        {
            manipulator.manipulate(position, data, rowView, parent);
        }

        return rowView;
    }

    /**
     * Register a ViewManipulator for manipulating Views with an ID of viewId
     *
     * @param manipulator The View-Manipulator
     */
    public void registerViewManipulator(ViewManipulator manipulator)
    {
        manipulators.add(manipulator);
    }

}
