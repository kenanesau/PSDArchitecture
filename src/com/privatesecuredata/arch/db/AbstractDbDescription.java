package com.privatesecuredata.arch.db;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public abstract class AbstractDbDescription implements IDbDescription {


	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o instanceof IDbDescription) {
			IDbDescription that = (IDbDescription) o;
			return Objects.equal(this.getName(), that.getName()) &&
				   Objects.equal(this.getVersion(), that.getVersion()) &&
				   Objects.equal(this.getInstance(), that.getInstance());
		}
		else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return Objects.hashCode(getName(), getVersion(), getInstance());
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
		  .add("name", getName())
		  .add("version", getVersion())
		  .add("instance", getInstance())
		  .toString();
	}
}
