/*
 * Copyright 2014. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.extensions.tuxedo;

import com.google.common.collect.Maps;

import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: abey.tom
 * Date: 4/17/14
 * Time: 10:32 AM
 * To change this template use File | Settings | File Templates.
 */
public class GroupCounter<K> {

    private Map<K, Long> countMap;

    public GroupCounter() {
        countMap = Maps.newHashMap();
    }

    public void increment(K key, long increment) {
        Long value = countMap.get(key);
        if (value == null) {
            countMap.put(key, increment);
        } else {
            countMap.put(key, value + increment);
        }
    }

    public void increment(K key) {
        increment(key, 1);
    }

    public Set<K> keys(){
        return countMap.keySet();
    }

   public Long get(K key){
        return countMap.get(key);
   }
}
