package com.privatesecuredata.arch.db;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public abstract class AbstractDbDescription implements IDbDescription {

	@Override
	public Class<?>[] getPersistentTypes() {
		return new Class[] {};
	}

    @Override
    public Class<?>[] getQueryBuilderTypes() {
        return new Class[] {};
    }

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o instanceof IDbDescription) {
			IDbDescription that = (IDbDescription) o;
			return Objects.equal(this.getBaseName(), that.getBaseName()) &&
				   Objects.equal(this.getVersion(), that.getVersion()) &&
				   Objects.equal(this.getInstance(), that.getInstance());
		}
		else {
			return false;
		}
	}

    public String getName() {
        return String.format("%s_I%d_V%d.db", getBaseName(), getInstance(), getVersion());
    }
	
	@Override
	public int hashCode() {
		return Objects.hashCode(getBaseName(), getVersion(), getInstance());
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
		  .add("name", getBaseName())
		  .add("version", getVersion())
		  .add("instance", getInstance())
		  .toString();
	}
}
