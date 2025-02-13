package com.privatesecuredata.arch.mvvm.android;

import android.app.Activity;
import android.os.Looper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
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
import com.privatesecuredata.arch.db.query.QueryParameterCache;
import com.privatesecuredata.arch.mvvm.IViewModelChangedListener;
import com.privatesecuredata.arch.mvvm.binder.TransientViewToVmBinder;
import com.privatesecuredata.arch.mvvm.vm.EncapsulatedListViewModel;
import com.privatesecuredata.arch.mvvm.vm.IDbBackedListViewModel;
import com.privatesecuredata.arch.mvvm.vm.IListViewModel;
import com.privatesecuredata.arch.mvvm.vm.IModelChangedListener;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;
import com.privatesecuredata.arch.mvvm.vm.OrderBy;
import com.privatesecuredata.arch.mvvm.vm.SimpleValueVM;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import io.reactivex.Observable;

/**
 * ListAdapter to enable the display of ListViewModels based on a database cursor in an Android ListView
 *
 * @param <M> Type of Model
 * @param <COMPLEXVM> Type of ViewModel
 */
public class MVVMRecyclerViewModelAdapter<M, COMPLEXVM extends IViewModel> extends RecyclerView.Adapter<MVVMRecyclerViewModelAdapter.ViewHolder>
															implements IModelChangedListener, Filterable,
                                                            IViewModelChangedListener, IDbBackedListViewModel

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
        private View emptyView;
        private ViewGroup parentView;
        private MVVMRecyclerViewModelAdapter adapter;

        protected ViewHolder(View view) {
            super(view);

            if (this.rowView != null) {
                this.rowView.setOnClickListener(null);
                this.rowView.setOnLongClickListener(null);
            }

            this.rowView = view;
            //this.rowView.setOnTouchListener(this);
            this.rowView.setOnClickListener(this);
            this.rowView.setOnLongClickListener(this);
        }

        public ViewHolder(ViewGroup parent, int id) {
            this(LayoutInflater.from(parent.getContext()).inflate(id, parent, false));
            this.parentView = parent;
        }

        public boolean showEmptyView() {
            return true;
        }

        public void updateViews(MVVMRecyclerViewModelAdapter adapter, V model) {
            View v = getRowView();
            getRowView().setVisibility(adapter.isEmpty() && showEmptyView() ? View.GONE : View.VISIBLE);

            if (!adapter.isEmpty()) {
                if (emptyView != null)
                    emptyView.setVisibility(View.GONE);
                if (adapter.hasActionMode()) {
                    boolean activated = adapter.isItemChecked(getAdapterPosition());
                    v.setActivated(activated);
                }
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

	private final Activity ctx;
	private IListViewModel<M> data;
    private List<ViewManipulator> manipulators = new ArrayList<>();
    private ArrayList<IViewModel> updatingVMs = new ArrayList<>();
    private IViewHolderFactory<M> recyclerStrategy;
    private ActionMode actionMode;
    private ActionMode.Callback actionModeCb;
    private IClickListener onClickCb;
    private ILongClickListener onLongClickCb;
    private RecyclerView view;
    private boolean isEmpty = true;
    private boolean isBinding = false;

    private QueryParameterCache queryParaCache = new QueryParameterCache();

    /**
	 * id of the Checkbox in the Row-Layout which is used for selection
	 */
	private int selectionViewId = -1;

	private Hashtable<Integer, TransientViewToVmBinder<?>> view2ModelAdapters = new Hashtable<Integer, TransientViewToVmBinder<?>>();
	private ArrayList<SimpleValueVM<Boolean>> selectedItemVMs;

    public MVVMRecyclerViewModelAdapter(Activity ctx, IViewHolderFactory strategy)
	{
		this.ctx = ctx;
        this.recyclerStrategy = strategy;
        setEmpty(true);
	}

	public MVVMRecyclerViewModelAdapter(Activity ctx, IListViewModel<M> lstVMs, IViewHolderFactory strategy)
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
            setItemChecked(pos, !isItemChecked(pos));

            if (null != onLongClickCb) {

                onLongClickCb.onLongClick(v, pos);
                notifyItemChanged(pos);
            }
        }
        else {
            if (null != onClickCb) {
                onClickCb.onClick(v, pos);
                notifyItemChanged(pos);
            }
        }
    }

    public boolean onLongClick(View v, int pos) {
        boolean ret = false;
        if ( (actionModeCb != null) && (actionMode == null) ) {
            setItemChecked(pos, !isItemChecked(pos));

            ctx.startActionMode(getActionModeCb());
            ret = true;
        }

        /**
         * Call onLongClickCb.onLongCLick() even if action-mode is active
         */
        if (null != onLongClickCb)
            ret = onLongClickCb.onLongClick(v, pos);

        notifyItemChanged(pos);
        return ret;
    }

    protected void checkEmpty() {
        if ( (null != data) && (this.data.size() > 0) )
            setEmpty(false);
        else
            setEmpty(true);
    }
	
	/*
	 * This method discards the old VM and register a new one
	 */
	public void setData(IListViewModel<M> newData)
	{
        if (newData == this.data)
            return;

        if (null != this.data) {
            this.data.delModelListener(this);
        }

        this.data = newData;
        if (null != this.data) {
            this.data.addModelListener(this);

            if (this.data.db() != null) {
                queryParaCache.configureListViewModel(this.data.db());
            }
            checkEmpty();
        }
        else
            setEmpty(true);

        redrawViews();
	}

    public IListViewModel<M> getData()
    {
        return this.data;
    }

    /**
     * Returns the number of items
     * If the list has no data and you want to display a special "empty item" overwrite this and
     * return 1 although the list is empty.
     *
     * If the list contains data or not please use isEmpty()
     * @return the number of (displayed) items
     * @sa isEmpty()
     */
    public int getItemCount() {
        int ret = 0;

        if ( (data == null) || (data.size() == 0) )
        {
            setEmpty(true);
        }
        else {
            ret = data.size();
            setEmpty(false);
        }


        return ret;
    }

    /**
     * @return true if the list contains data, false otherwise
     */
    public boolean isEmpty() {
        return isEmpty;
    }

    /**
     * Internal setter for empty-information
     * @param empty
     * @sa isEmpty()
     */
    private void setEmpty(final boolean empty)
    {
        if (empty != isEmpty) {
            /// TODO: Make this optional via setting in adapter otherwise empty row-View would be obsolete

            ctx.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (null != MVVMRecyclerViewModelAdapter.this.view) {
                        //MVVMRecyclerViewModelAdapter.this.view.setVisibility(empty ? View.GONE : View.VISIBLE);
                    }
                }
            });
        }

        this.isEmpty = empty;
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
        M model = null;
        isBinding = true;

        if (!isEmpty())
            model = position < data.size() ? data.get(position) : null;

        holder.updateViews(this, model);

        /** Search for View-IDs of the views to manipulate and register them with the viewholder **/
        for (ViewManipulator manipulator : getManipulators()) {
            manipulator.manipulate(position, model, holder.getRowView(), holder.getParentView());
        }
        isBinding = false;
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

    protected List<ViewManipulator> getManipulators() {
        return manipulators;
    }
	
    /**
     * Called by underlying EncapsulatedListViewModel when the Model has changed
     */
    @Override
    public void notifyModelChanged(IViewModel<?> vm, IViewModel<?> originator) {
        checkEmpty();
        //notify (list)view of changed data -> redraw
        redrawViews();
    }

    private void doUiUpdate() {
        MVVMRecyclerViewModelAdapter.this.notifyDataSetChanged();
    }


    protected void redrawViews() {
        if (!isBinding) {
            if (Looper.myLooper() == Looper.getMainLooper())
                doUiUpdate();
            else {
                ctx.runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                doUiUpdate();
                            }
                        }
                );
            }
        }
    }

    @Override
    public Filter getFilter() {
        return null == data || data.db() == null ? null : data.db().getFilter();
    }

    protected boolean isDbBacked() {
        return ( (null != data) && (null != data.db()) );
    }

    @Override
    public void setQueryId(String queryId) {
        queryParaCache.setQueryId(queryId);

        if (isDbBacked())
            data.db().setQueryId(queryId);
    }

    @Override
    public void where(String id, Object val) {
        queryParaCache.where(id, val);

        if (isDbBacked()) {
            data.db().setQueryId(this.queryParaCache.getQueryId());
            data.db().where(id, val);
        }
    }

    @Override
    public void where(String id, Class val) {
        queryParaCache.where(id, val);

        if (isDbBacked()) {
            data.db().setQueryId(this.queryParaCache.getQueryId());
            data.db().where(id, val);
        }
    }

    @Override
    public void loadData() {
        if (null != this.data.db())
            this.data.db().loadData();
    }

    @Override
    public Observable<IListViewModel<M>> loadDataAsync() {
        return (null != this.data.db()) ? this.data.db().loadDataAsync() : Observable.empty();
    }

    public void setFilterParamId(String objFieldName)
    {
        this.queryParaCache.setFilterParamId(objFieldName);
        if (isDbBacked())
            data.db().setFilterParamId(this.queryParaCache.getFilteredParamId());
    }

    public void setSortOrder(OrderBy... sortOrderTerms) {
        this.queryParaCache.setSortOrder(sortOrderTerms);
        if (isDbBacked())
            data.db().setSortOrder(this.queryParaCache.getSortOrder());
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
        // do nothing in lists -- list-changes have to be comitted before the UI is redrawn
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

    public Activity getContext() { return this.ctx; }

    public void setItemChecked(final int position, boolean isChecked) {
        selectedPos.put(position, isChecked);
        if (isChecked)
            setCheckedItemCount(getCheckedItemCount() + 1);
        else
            setCheckedItemCount(getCheckedItemCount() - 1);

        if (!isBinding) {
            ctx.runOnUiThread(new Runnable() {
                                  @Override
                                  public void run() {
                                      notifyItemChanged(position);
                                  }
                              }
            );
        }
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
        redrawViews();
    }

    public boolean hasActionMode() {
        return this.actionModeCb != null;
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.view = recyclerView;
        //this.view.setVisibility(isEmpty() ? View.GONE : View.VISIBLE);
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
