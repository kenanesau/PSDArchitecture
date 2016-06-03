package com.privatesecuredata.arch.mvvm.android;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import com.privatesecuredata.arch.R;
import com.privatesecuredata.arch.mvvm.IViewModelChangedListener;
import com.privatesecuredata.arch.mvvm.binder.TransientViewToVmBinder;
import com.privatesecuredata.arch.mvvm.vm.IListViewModel;
import com.privatesecuredata.arch.mvvm.vm.IModelChangedListener;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;
import com.privatesecuredata.arch.mvvm.vm.OrderBy;
import com.privatesecuredata.arch.mvvm.vm.SimpleValueVM;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

/**
 * ListAdapter to enable the display of ListViewModels based on a database cursor in an Android ListView
 *
 * @param <M> Type of Model
 * @param <COMPLEXVM> Type of ViewModel
 */
public class MVVMRecyclerViewModelAdapter<M, COMPLEXVM extends IViewModel> extends RecyclerView.Adapter<MVVMRecyclerViewModelAdapter.ViewHolder>
															implements IModelChangedListener, Filterable,
                                                            IViewModelChangedListener

{
    public interface IClickListener {
        void onClick(View v, int pos);
    };

    public interface ILongClickListener {
        boolean onLongClick(View v, int pos);
    };

    public interface IViewHolderFactory<M> {
        MVVMRecyclerViewModelAdapter.ViewHolder createViewHolder(ViewGroup parent);
    };

    public static abstract class ViewHolder<V> extends RecyclerView.ViewHolder
            implements  View.OnClickListener, View.OnLongClickListener {
        private View rowView;
        private ViewGroup parentView;
        private MVVMRecyclerViewModelAdapter adapter;

        protected ViewHolder(View view) {
            super(view);
            this.rowView = view;
            //this.rowView.setOnTouchListener(this);
            this.rowView.setOnClickListener(this);
            this.rowView.setOnLongClickListener(this);
        }

        public ViewHolder(ViewGroup parent, int id) {
            this(LayoutInflater.from(parent.getContext()).inflate(id, parent, false));
            this.parentView = parent;
        }

        public void updateViews(MVVMRecyclerViewModelAdapter adapter, V model) {
            View v = getRowView();

            if (adapter.hasActionMode()) {
                boolean activated = adapter.isItemChecked(getAdapterPosition());
                v.setActivated(activated);
            }
        };

        public View getRowView() { return rowView; }
        public ViewGroup getParentView() { return parentView; }

        @Override
        public void onClick(View v) {
            adapter.onClick(v, getAdapterPosition());
        }

        @Override
        public boolean onLongClick(View v) {
            return adapter.onLongClick(v, getAdapterPosition());
        }

        public void setAdapter(MVVMRecyclerViewModelAdapter adapter) {
            this.adapter = adapter;
        }
    }

    private int defaultBackgroundResource = R.drawable.activated_selector;
    private int actionModeBgResource = R.drawable.activated_selector_no_press;

    private SparseBooleanArray selectedPos = new SparseBooleanArray();
    private int selectedCnt = 0;

	private final Context ctx;
	private IListViewModel<M, COMPLEXVM> data;
    private List<ViewManipulator> manipulators = new ArrayList<>();
    private ArrayList<IViewModel> updatingVMs = new ArrayList<>();
    private OrderBy[] sortOrder;
    private IViewHolderFactory<M> recyclerStrategy;
    private ActionMode actionMode;
    private ActionMode.Callback actionModeCb;
    private IClickListener onClickCb;
    private ILongClickListener onLongClickCb;
    private RecyclerView view;


	/**
	 * id of the Checkbox in the Row-Layout which is used for selection
	 */
	private int selectionViewId = -1;

	private Hashtable<Integer, TransientViewToVmBinder<?>> view2ModelAdapters = new Hashtable<Integer, TransientViewToVmBinder<?>>();
	private ArrayList<SimpleValueVM<Boolean>> selectedItemVMs;
    private String filteredParamId = null;

    public MVVMRecyclerViewModelAdapter(Context ctx, IViewHolderFactory strategy)
	{
		this.ctx = ctx;
        this.recyclerStrategy = strategy;
	}

	public MVVMRecyclerViewModelAdapter(Context ctx, IListViewModel<M, COMPLEXVM> lstVMs, IViewHolderFactory strategy)
	{
		this(ctx, strategy);
		setData(lstVMs);
	}

    public void setDefaultBackgroundResource(int defaultBackgroundResource) {
        this.defaultBackgroundResource = defaultBackgroundResource;
    }

    public void setActionModeBgResource(int actionModeBgResource) {
        this.actionModeBgResource = actionModeBgResource;
    }

    public void onClick(View v, int pos) {
        if (actionMode != null) {
            v.setBackgroundResource(R.drawable.activated_selector_no_press);
            setItemChecked(pos, !isItemChecked(pos));

            if (null != onLongClickCb) {

                onLongClickCb.onLongClick(v, pos);
                notifyItemChanged(pos);
            }
        }
        else {
            v.setBackgroundResource(R.drawable.activated_selector);
            if (null != onClickCb) {
                onClickCb.onClick(v, pos);
                notifyItemChanged(pos);
            }
        }
    }

    public boolean onLongClick(View v, int pos) {
        boolean ret = false;
        if ( (actionModeCb != null) && (actionMode == null) ) {
            v.setBackgroundResource(R.drawable.activated_selector_no_press);
            setItemChecked(pos, !isItemChecked(pos));

            if (ctx instanceof Activity) {
                ((Activity)ctx).startActionMode(getActionModeCb());
            }
        }
        else {
            v.setBackgroundResource(R.drawable.activated_selector);
        }

        if (null != onLongClickCb)
            ret = onLongClickCb.onLongClick(v, pos);

        notifyItemChanged(pos);
        return ret;
    }
	
	/*
	 * This method discards the old VM and register a new one
	 */
	public void setData(IListViewModel<M, COMPLEXVM> data)
	{
		if (null != this.data)
			this.data.delModelListener(this);
		this.data = data;
        if (null != this.data) {
            this.data.size(); // force loading
            this.data.addModelListener(this);
            if (null != sortOrder)
                this.data.setSortOrder(sortOrder);
        }
        if (null != filteredParamId)
            this.data.setFilterParamId(filteredParamId);

        notifyDataSetChanged();
	}

    public IListViewModel<M, COMPLEXVM> getData()
    {
        return this.data;
    }

    public int getItemCount() {
        return data == null ? 0 : data.size();
    }

    public int getCheckedItemCount() { return selectedCnt; }
    protected void setCheckedItemCount(int cnt) {

        if ( (selectedCnt > 0) && (cnt == 0) )
        {
            ActionMode m = getActionMode();
            if ( m != null )
                m.finish();
        }
        selectedCnt = cnt;

    }
	
	public MVVMRecyclerViewModelAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewHolder holder = recyclerStrategy.createViewHolder(parent);
        holder.setAdapter(this);
        return holder;
    }

    public void onBindViewHolder(MVVMRecyclerViewModelAdapter.ViewHolder holder, int position) {
        M model = data.get(position);
        holder.updateViews(this, model);

        /** Search for View-IDs of the views to manipulate and register them with the viewholder **/
        for (ViewManipulator manipulator : manipulators)
        {
            manipulator.manipulate(position, model, holder.getRowView(), holder.getParentView());
        }

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
	
    /**
     * Called by underlying EncapsulatedListViewModel when the Model has changed
     */
    @Override
    public void notifyModelChanged(IViewModel<?> vm, IViewModel<?> originator) {
        //notify (list)view of changed data -> redraw
        this.notifyDataSetChanged();
    }

    @Override
    public Filter getFilter() {
        return null == data ? null : data.getFilter();
    }

    public void setFilterParamId(String filterParamId)
    {
        if (null == data)
            this.filteredParamId = filterParamId;
        else
            data.setFilterParamId(filterParamId);
    }

    public void setSortOrder(OrderBy... sortOrderTerms) {
        this.sortOrder = sortOrderTerms;
        if (null != data)
            data.setSortOrder(sortOrderTerms);
    }

    public void updateViewOnChange(SimpleValueVM vm) {
        this.updatingVMs.add(vm);
        vm.addViewModelListener(this);
    }

    public void clearUpdateViewOnChange()
    {
        for (IViewModel vm : updatingVMs)
            vm.delViewModelListener(this);

        updatingVMs.clear();
    }

    public void notifyViewModelDirty(IViewModel<?> vm, IViewModelChangedListener originator) {
        MVVMRecyclerViewModelAdapter.this.notifyDataSetChanged();
    }

    /**
     * unregister from the VMs...
     */
    public void dispose()
    {
        for (IViewModel vm : updatingVMs)
        {
            vm.delViewModelListener(this);
        }
        updatingVMs.clear();
    }

    public Context getContext() { return this.ctx; }

    public void setItemChecked(int position, boolean isChecked) {
        selectedPos.put(position, isChecked);
        if (isChecked)
            setCheckedItemCount(getCheckedItemCount() + 1);
        else
            setCheckedItemCount(getCheckedItemCount() - 1);
        notifyItemChanged(position);
    }

    public boolean isItemChecked(int position) {
        return selectedPos.get(position);
    }

    public SparseBooleanArray getSelectedPositions() {
        return selectedPos;
    }

    public void clearSelectedPositions() {
        selectedPos.clear();
        setCheckedItemCount(0);
        notifyDataSetChanged();
    }

    public boolean hasActionMode() {
        return this.actionModeCb != null;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.view = recyclerView;
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        this.view = null;
        super.onDetachedFromRecyclerView(recyclerView);
    }

    public void setActionModeCb(final ActionMode.Callback cb)
    {
        this.actionModeCb = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                actionMode = mode;
                MVVMRecyclerViewModelAdapter.this.view.setLongClickable(false);
                return cb.onCreateActionMode(mode, menu);
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return cb.onPrepareActionMode(mode, menu);
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return cb.onActionItemClicked(mode, item);
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                actionMode = null;
                MVVMRecyclerViewModelAdapter.this.view.setLongClickable(true);
                if (getCheckedItemCount() > 0)
                    clearSelectedPositions();
                cb.onDestroyActionMode(mode);
            }
        };
    }

    public ActionMode getActionMode() { return actionMode; }
    public boolean isInActionMode() { return actionMode != null; }
    public ActionMode.Callback getActionModeCb() { return this.actionModeCb; }

    public void setOnClickCb(IClickListener onClickCb) {
        this.onClickCb = onClickCb;
    }

    public void setOnLongClickCb(ILongClickListener onLongClickCb) {
        this.onLongClickCb = onLongClickCb;
    }


}
