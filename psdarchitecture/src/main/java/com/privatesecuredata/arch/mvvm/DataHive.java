package com.privatesecuredata.arch.mvvm;

import java.util.Hashtable;
import java.util.UUID;

/**
 * 
 * @author kenan
 *
 * Class for transferring data easily from one Activity to the next.
 * 
 * Just put arbitrary data and get an UUID (as string) back. This string can 
 * be easily sent to the next activity via Intent. This activity can get the
 * data back using the string. 
 */
public class DataHive
{
	private static DataHive instance = null;
	private Hashtable<UUID, Object> data = new Hashtable<UUID, Object>();
			
	private DataHive() {}
	
	public static DataHive getInstance()
	{
		if (null == instance)
			instance = new DataHive();
		
		return instance;
	}

	public <T> String put(T obj, String tag)
	{
		UUID ret = UUID.nameUUIDFromBytes(tag.getBytes());
		data.put(ret, obj);
		return ret.toString();
	}
	
	public <T> String put(T obj)
	{
		UUID ret = UUID.randomUUID();
		data.put(ret, obj);
		return ret.toString();
	}

	public <T> T getTag(String tag)
	{
		return get(UUID.nameUUIDFromBytes(tag.getBytes()));
	}
	public <T> T get(String key)
	{
		return get(UUID.fromString(key));		
	}

	public <T> T get(UUID key)
	{
		T ret = null;
		return  (T)data.get(key);
	}

	public <T> T removeTag(String tag)
	{
		return remove(UUID.nameUUIDFromBytes(tag.getBytes()));
	}
	public <T> T remove(String key)
	{
		return this.remove(UUID.fromString(key));
	}
	public <T> T remove(UUID key)
	{
		return (T)data.remove(key);
	}
}
