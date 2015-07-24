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
import com.privatesecuredata.arch.mvvm.vm.ViewModel;

import java.lang.reflect.Constructor;
import java.util.List;

/**
 * This is the actual parcelable object which stores and restores all the data
 * needed to store the state of an ViewModel
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
        String str = getClass().getName();
//        Log.d(str, "read objCount (int)");
//        int objCount = parcel.readInt();

        readStateFromParcel(parcel, this);
//        objCount--;
//
//        for (int i = 0; i < objCount; i++) {
//            ViewModelState childState = new ViewModelState();
//            readStateFromParcel(parcel, childState);
//            childStates.add(childState);
//        }
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
        return (vm != null) ? ((IPersistable)vm.getModel()).getDbId().getId() : dbId;
    }

    public String getTypeName() {
        return (vm != null) ? vm.getClass().getCanonicalName() : typeName;
    }

    public List<ViewModelState> getChildStates()
    {
        return childStates;
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
        dest.writeString(vm.getModel().getClass().getCanonicalName());
        DbId dbId =  ((IPersistable)vm.getModel()).getDbId();
        Log.d(str, "write containsNewObject (byte)");
        dest.writeByte((byte) (dbId != null ? 0 : 1));
        if (dbId != null) {
            Log.d(str,"write dbId (long)");
            dest.writeLong(dbId.getId());
        }

    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        String str = getClass().getName();
        //List<ComplexViewModel> vms = vm.getComplexChildren();
        //int objCount = vms.hashCode();
//        Log.d(str, "write objCount (int)");
//        dest.writeInt(objCount + 1);

        writeStateToParcel(dest, (ComplexViewModel)vm);

//        for (ComplexViewModel childVM : vms)
//        {
//            writeStateToParcel(dest, childVM);
//        }
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
                .add("type", getTypeName())
                .add("dbId", getDbId())
                .add("key", getKey())
                .toString();
    }
}
