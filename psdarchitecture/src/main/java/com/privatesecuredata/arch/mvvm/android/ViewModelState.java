package com.privatesecuredata.arch.mvvm.android;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.privatesecuredata.arch.db.DbId;
import com.privatesecuredata.arch.db.IPersistable;
import com.privatesecuredata.arch.db.PersistanceManager;
import com.privatesecuredata.arch.mvvm.vm.ComplexViewModel;
import com.privatesecuredata.arch.mvvm.vm.IViewModel;

import java.lang.reflect.Constructor;
import java.util.List;

/**
 * This is the actual parcelable object which stores and restores all the data
 * (Table + DB-ID) needed to store the state of an ViewModel
 *
 * @see MVVMInstanceStateHandler
 */
public class ViewModelState implements Parcelable {
    private String key;
    private Boolean containsNewObject = false;
    private IViewModel vm;
    private String typeName;
    private Long dbId = -1L;
    private List<ViewModelState> childStates;

    protected ViewModelState() {}

    /**
     * Constructor used by android when the state is restores
     *
     * @param parcel The parcel containing the state
     */
    public ViewModelState(Parcel parcel)
    {
        readStateFromParcel(parcel, this);
    }

    /**
     * Consturctor used by the MVVMInstanceStateHandler to save the instance state
     *
     * @param vm The Viewmodel containing an IPersistable
     * @see MVVMInstanceStateHandler
     */
    public ViewModelState(IViewModel vm) {
        key = vm.getModel().getClass().getCanonicalName();
        this.vm = vm;
    }

    /**
     * Consturctor used by the MVVMInstanceStateHandler to save the instance state
     *
     * @param key Alternative key when not the class-name is used as a key
     * @param vm The Viewmodel containing an IPersistable
     * @see MVVMInstanceStateHandler
     */
    public ViewModelState(String key, IViewModel vm) {
        this.key = key;
        this.vm = vm;
    }

    public String getKey() { return key; }

    public Long getDbId() {
        long ret = -1L;
        if (vm!=null)
        {
            IPersistable model = (IPersistable) vm.getModel();

            if (null != model)
            {
                DbId dbId = model.getDbId();
                if (dbId != null)
                    ret = dbId.getId();
            }
        }

        return ret;
    }

    public String getTypeName() {
        return (vm != null) ? vm.getModel().getClass().getCanonicalName() : typeName;
    }

    @Override
    public int describeContents() {
        return 0;
    }


    protected void readStateFromParcel(Parcel parcel, ViewModelState state) {
        String str = getClass().getName();
        Log.d(str, "read key (String)");
        state.key = parcel.readString();
        Log.d(str, "read typeName (String)");
        state.typeName = parcel.readString();
        Log.d(str, "read containsNewObject (byte)");
        state.containsNewObject = (parcel.readByte() != 0);
        if (!state.containsNewObject) {
            Log.d(str, "read dbId (long)");
            state.dbId = parcel.readLong();
        }
    }

    protected void writeStateToParcel(Parcel dest, ComplexViewModel vm)
    {
        String str = getClass().getName();
        Log.d(str, "write key (string)");
        dest.writeString(key);
        Log.d(str, "write typeName (string)");
        dest.writeString(getTypeName());
        long id = getDbId();
        Log.d(str, "write containsNewObject (byte)");
        dest.writeByte((byte) (id != -1L ? 0 : 1));
        if (id != -1L) {
            Log.d(str,"write dbId (long)");
            dest.writeLong(id);
        }

    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        writeStateToParcel(dest, (ComplexViewModel)vm);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<ViewModelState> CREATOR = new Parcelable.Creator<ViewModelState>() {
        @Override
        public ViewModelState createFromParcel(Parcel in) {
            return new ViewModelState(in);
        }

        @Override
        public ViewModelState[] newArray(int size) {
            return new ViewModelState[size];
        }
    };

    /**
     * Returns the ViewModel in any case (when the instance was saved before or not)
     *
     * @param pm The Persistance manager
     * @param <T>
     * @return Returns the ViewModel containing an IPersistable model
     */
    public <T extends IViewModel> T getVM(PersistanceManager pm)
    {
        T ret = (T)this.vm;
        if (ret != null)
            return ret;

        try {
            Class type = Class.forName(getTypeName());
            IPersistable model;
            if (!containsNewObject) {
                model = pm.load(type, getDbId());
            }
            else {
                Constructor constructor = type.getConstructor();
                model = (IPersistable)constructor.newInstance();
            }

            this.vm = pm.createMVVM().createVM(model);
            ret = (T) vm;

        }
        catch (Exception ex)
        {
            Log.d(getClass().getSimpleName(), String.format("Cannot create ViewModel of Type \"%s\": %s", getTypeName(), ex.getMessage()));
        }
        finally {
            return ret;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getKey(), getDbId(), getTypeName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof ViewModelState) {
            ViewModelState that = (ViewModelState) o;
            return Objects.equal(this.getKey(), that.getKey()) &&
                    Objects.equal(this.getDbId(), that.getDbId()) &&
                    Objects.equal(this.getTypeName(), that.getTypeName());
        }
        else {
            return false;
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("type", getTypeName() == null ? "null" : getTypeName())
                .add("dbId", getDbId() == null ? "null" : getDbId())
                .add("key", getKey())
                .toString();
    }
}
