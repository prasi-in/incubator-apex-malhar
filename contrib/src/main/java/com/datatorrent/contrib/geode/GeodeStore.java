/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.datatorrent.contrib.geode;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gemstone.gemfire.cache.CacheClosedException;
import com.gemstone.gemfire.cache.CacheWriterException;
import com.gemstone.gemfire.cache.EntryNotFoundException;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.TimeoutException;
import com.gemstone.gemfire.cache.client.ClientCache;
import com.gemstone.gemfire.cache.client.ClientCacheFactory;
import com.gemstone.gemfire.cache.client.ClientRegionShortcut;
import com.gemstone.gemfire.cache.query.FunctionDomainException;
import com.gemstone.gemfire.cache.query.NameResolutionException;
import com.gemstone.gemfire.cache.query.QueryInvocationTargetException;
import com.gemstone.gemfire.cache.query.SelectResults;
import com.gemstone.gemfire.cache.query.TypeMismatchException;

import com.datatorrent.lib.db.KeyValueStore;

/**
 * Provides the implementation of a Geode store.
 *
 * 
 */
public class GeodeStore implements KeyValueStore, Serializable
{
  /**
   * 
   */
  private static final long serialVersionUID = -5076452548893319967L;
  private static final Logger LOG = LoggerFactory.getLogger(GeodeStore.class);
  private transient ClientCache clientCache = null;
  private transient Region<Object, Object> region = null;
  private String locatorHost;
  private int locatorPort;
  private String regionName;

  private ClientCache initClient()
  {
    try {
      clientCache = new ClientCacheFactory().addPoolLocator(getLocatorHost(), getLocatorPort()).create();
    } catch (CacheClosedException ex) {
      LOG.info("error initiating client ", ex);
    }

    return clientCache;
  }

  /**
   * @return the regionName
   */
  public String getRegionName()
  {
    return regionName;
  }

  /**
   * @param regionName
   *          the regionName to set
   */
  public void setRegionName(String regionName)
  {
    this.regionName = regionName;
  }

  /**
   * @return the clientCache
   */
  public ClientCache getClientCache()
  {
    return clientCache;
  }

  /**
   * @param clientCache
   *          the clientCache to set
   */
  public void setClientCache(ClientCache clientCache)
  {
    this.clientCache = clientCache;
  }

  /**
   * @return the locatorPort
   */
  public int getLocatorPort()
  {
    return locatorPort;
  }

  /**
   * @param locatorPort
   *          the locatorPort to set
   */
  public void setLocatorPort(int locatorPort)
  {
    this.locatorPort = locatorPort;
  }

  /**
   * @return the locatorHost
   */
  public String getLocatorHost()
  {
    return locatorHost;
  }

  /**
   * @param locatorHost
   *          the locatorHost to set
   */
  public void setLocatorHost(String locatorHost)
  {
    this.locatorHost = locatorHost;
  }

  /**
   * @return the region
   * @throws IOException
   */
  public Region<Object, Object> getRegion() throws IOException
  {
    // return region;
    if (clientCache == null && clientCache.isClosed()) {
      initClient();
    }

    if (region == null) {
      region = clientCache.getRegion(regionName);
      if (region == null) {
        region = clientCache.<Object, Object> createClientRegionFactory(ClientRegionShortcut.PROXY).create(regionName);
      }
    }

    return region;
  }

  /**
   * @param region
   *          the region to set
   */
  public void setRegion(Region<Object, Object> region) throws IOException
  {
    this.region = region;
  }

  @Override
  public void connect() throws IOException
  {
    try {
      clientCache = new ClientCacheFactory().addPoolLocator(getLocatorHost(), getLocatorPort()).create();
    } catch (CacheClosedException ex) {
      LOG.info("error initiating client ", ex);
    }

    region = clientCache.getRegion(getRegionName());

    if (region == null) {
      region = clientCache.<Object, Object> createClientRegionFactory(ClientRegionShortcut.PROXY).create(
          getRegionName());
    }

  }

  @Override
  public void disconnect() throws IOException
  {
    clientCache.close();

  }

  @Override
  public boolean isConnected()
  {
    return (clientCache.isClosed());

  }

  /**
   * Gets the value given the key. Note that it does NOT work with hash values
   * or list values
   *
   * @param key
   * @return The value.
   */
  @Override
  public Object get(Object key)
  {

    try {
      return (getRegion().get(key));
    } catch (IOException ex) {
      LOG.info("error getting object ", ex);
      return null;
    }

  }

  /**
   * Gets all the values given the keys. Note that it does NOT work with hash
   * values or list values
   *
   * @param keys
   * @return All values for the given keys.
   */
  @SuppressWarnings("unchecked")
  @Override
  public List<Object> getAll(List<Object> keys)
  {

    List<Object> values = new ArrayList<Object>();

    try {
      final Map<Object, Object> entries = getRegion().getAll(keys);
      for (int i = 0; i < keys.size(); i++) {
        values.add(entries.get(keys.get(i)));
      }
    } catch (IOException ex) {
      LOG.info("error getting region ", ex);
    }

    return (values);

  }

  public Map<Object, Object> getallMap(List<Object> keys)
  {

    try {
      final Map<Object, Object> entries = getRegion().getAll(keys);
      return (entries);
    } catch (IOException ex) {
      LOG.info("error getting object ", ex);
      return null;
    }

  }

  @SuppressWarnings("rawtypes")
  public SelectResults query(String predicate)
  {
    try {
      return (getRegion().query(predicate));
    } catch (FunctionDomainException | TypeMismatchException | NameResolutionException | QueryInvocationTargetException
        | IOException e) {
      e.printStackTrace();
      return null;
    }

  }

  @Override
  public void put(Object key, Object value)
  {
    try {
      getRegion().put(key, value);
    } catch (IOException e) {
      LOG.info("while putting in region", e);
    }
  }

  @Override
  public void putAll(Map<Object, Object> map)
  {
    try {
      getRegion().putAll(map);
    } catch (IOException e) {
      LOG.info("while putting all in region", e);
    }
  }

  @Override
  public void remove(Object key)
  {
    try {
      getRegion().destroy(key);
    } catch (TimeoutException | CacheWriterException | EntryNotFoundException | IOException e) {
      LOG.info("while deleting", e);
    }
  }

}
