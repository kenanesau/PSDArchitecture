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
public class ModelState implements Parcelable {
    private String key;
    private Boolean containsNewObject = false;
    private IPersistable model;
    private String typeName;
    private Long dbId = -1L;
    private List<ModelState> childStates;

    protected ModelState() {}

    /**
     * Constructor used by android when the state is restores
     *
     * @param parcel The parcel containing the state
     */
    public ModelState(Parcel parcel)
    {
        readStateFromParcel(parcel, this);
    }

    /**
     * Consturctor used by the MVVMInstanceStateHandler to save the instance state
     *
     * @param model The model
     * @see MVVMInstanceStateHandler
     */
    public ModelState(IPersistable model) {
        key = this.model.getClass().getCanonicalName();
        this.model = model;
    }

    /**
     * Consturctor used by the MVVMInstanceStateHandler to save the instance state
     *
     * @param key Alternative key when not the class-name is used as a key
     * @param model The Model
     * @see MVVMInstanceStateHandler
     */
    public ModelState(String key, IPersistable model) {
        this.key = key;
        this.model = model;
    }

    public String getKey() { return key; }

    public Long getDbId() {
        long ret = -1L;

        if (null != model) {
            DbId dbId = model.getDbId();
            if (dbId != null)
                ret = dbId.getId();
        }
        else {
            ret = dbId;
        }

        return ret;
    }

    public String getTypeName() {
        return (model != null) ? model.getClass().getCanonicalName() : typeName;
    }

    @Override
    public int describeContents() {
        return 0;
    }


    protected void readStateFromParcel(Parcel parcel, ModelState state) {
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

    protected void writeStateToParcel(Parcel dest)
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
        writeStateToParcel(dest);
    }

    @SuppressWarnings("unused")
    public static final Creator<ModelState> CREATOR = new Creator<ModelState>() {
        @Override
        public ModelState createFromParcel(Parcel in) {
            return new ModelState(in);
        }

        @Override
        public ModelState[] newArray(int size) {
            return new ModelState[size];
        }
    };

    /**
     * Returns the ViewModel in any case (when the instance was saved before or not)
     *
     * @param pm The Persistance manager
     * @param <T>
     * @return Returns the ViewModel containing an IPersistable model
     */
    public <T extends IPersistable> T getModel(PersistanceManager pm)
    {
        T ret = (T)this.model;
        if (ret != null)
            return ret;

        try {
            Class type = Class.forName(getTypeName());
            if (!containsNewObject) {
                ret = (T)pm.load(type, getDbId());
            }
            else {
                Constructor constructor = type.getConstructor();
                ret = (T)constructor.newInstance();
            }
        }
        catch (Exception ex)
        {
            Log.d(getClass().getSimpleName(), String.format("Cannot create Model of Type \"%s\": %s", getTypeName(), ex.getMessage()));
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
        if (o instanceof ModelState) {
            ModelState that = (ModelState) o;
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
