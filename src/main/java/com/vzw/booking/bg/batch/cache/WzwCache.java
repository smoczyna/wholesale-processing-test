/**
 * 
 */
package com.vzw.booking.bg.batch.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cache instance
 * 
 * @author torelfa
 *
 */
@Component
public class WzwCache {

	private static final Logger LOGGER = LoggerFactory.getLogger(WzwCache.class);

	private static WzwCache instance=null;

	private @Value("${com.springbatch.cache.file.location}") String fileLocation = "";

	protected Map<String, CacheItem<?>> cacheItems = new HashMap<>(0);

	/**
	 * Default Constructor
	 */
	protected WzwCache() {
		super();
		try {
			checkLoad();
		} catch (CacheException e) {
			LOGGER.error("Unable to initialize cache", e);
		}
	}

	/**
	 * @param itemName
	 */
	public boolean existsCacheItem(String itemName) {
		return cacheItems.get(itemName)!=null;
	}

	/**
	 * @param itemName
	 * @param type
	 */
	public <T> void createCacheItem(String itemName, Class<T> type) {
		CacheItem<T> item = new CacheItem<T>();
		cacheItems.put(itemName, item);
	}

	/**
	 * @param itemName
	 * @param key
	 * @param value
	 */
	@SuppressWarnings("unchecked")
	public <T> void addToItem(String itemName, String key, T value) {
		((CacheItem<T>) cacheItems.get(itemName)).addElement(key, value);
	}

	/**
	 * @param itemName
	 * @param indexer
	 * @param values
	 */
	@SuppressWarnings("unchecked")
	public <T> void addAllToItem(String itemName, Function<T, String> indexer, List<T> values) {
		((CacheItem<T>) cacheItems.get(itemName)).addMany(indexer, values);
	}

	/**
	 * @param itemName
	 * @param key
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> getValueFromItem(String itemName, String key) {
		List<T>  l = ((CacheItem<T>) cacheItems.get(itemName)).getRegistry().get(key);
		if (l==null) {
			return new ArrayList<>(0);
		}
		return l;
	}

//	/**
//	 * @param itemName
//	 * @return
//	 */
//	@SuppressWarnings("unchecked")
//	public <T> LList<T> getAllValuesFromItem(String itemName) {
//		return ((CacheItem<T>) cacheItems.get(itemName)).getRegistry().values().stream().collect(Collectors.toList());
//	}

	private String getFileName() {
		if (fileLocation == null || fileLocation.trim().isEmpty()) {
			return "cache-stogage.dat";
		}
		return fileLocation;
	}
	
	/**
	 * @return
	 * @throws CacheException
	 */
	public boolean checkLoad() throws CacheException {
		if (existsCacheFile()) {
			load();
		} else {
			save();
		}
		return false;
	}
	
	/**
	 * @throws CacheException
	 */
	public void checkSave() throws CacheException {
		save();
	}
	
	/**
	 * @return
	 */
	protected final boolean existsCacheFile() {
		String fileLocation = getFileName();
		File file = new File(fileLocation);
		return file.exists();
	}

	/**
	 * @throws CacheException
	 */
	protected final void load() throws CacheException {
		String fileLocation = getFileName();
		File file = new File(fileLocation);
		if (! file.exists()) {
			LOGGER.error("Cache binary file "+fileLocation+" doesn't exist!!");
			throw new CacheException("Unable to locate binary cache file at " + fileLocation);
		}
		try (ObjectInputStream os = new ObjectInputStream(new FileInputStream(file))) {
			Object o = os.readObject();
			if (WzwCache.class.isAssignableFrom(o.getClass())) {
				this.cacheItems.putAll(((WzwCache)o).cacheItems);
			} else {
				LOGGER.error("Unable to load Cache in file " + fileLocation + " due to unexpected cache data type");
				throw new CacheException("Unable to load Cache in file " + fileLocation + " due to unexpected cache data type");
			}
		} catch (Exception e) {
			LOGGER.error("Unable to load Cache in file " + fileLocation, e);
			throw new CacheException("Unable to load cache object from file at " + fileLocation, e);
		}
	}

	/**
	 * @throws CacheException
	 */
	protected final void save() throws CacheException {
		String fileLocation = getFileName();
		File file = new File(fileLocation);
		if (file.exists()) {
			boolean deleted = file.delete();
			LOGGER.info("Cache binary file "+fileLocation+" deleted : " + deleted);
		}
		try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(file))) {
			os.writeObject(this);
		} catch (Exception e) {
			LOGGER.error("Unable to save Cache in file " + fileLocation, e);
			throw new CacheException("Unable to save cache object from file at " + fileLocation, e);
		}
	}

	/**
	 * @return
	 */
	public static final WzwCache getInstance() {
		if (instance==null) {
			instance=new WzwCache();
		}
		return instance;
	}
	
}
