/*
 * Copyright 2017, OpenSkywalking Organization All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project repository: https://github.com/OpenSkywalking/skywalking
 */

package org.skywalking.apm.collector.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.skywalking.apm.collector.cache.dao.IServiceNameCacheDAO;
import org.skywalking.apm.collector.core.util.Const;
import org.skywalking.apm.collector.core.util.StringUtils;
import org.skywalking.apm.collector.storage.base.dao.DAOContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author peng-yongsheng
 */
public class ServiceNameCache {

    private static final Logger logger = LoggerFactory.getLogger(ServiceNameCache.class);

    //TODO size configuration
    private static Cache<Integer, String> CACHE = CacheBuilder.newBuilder().maximumSize(10000).build();

    public static String get(int serviceId) {
        IServiceNameCacheDAO dao = (IServiceNameCacheDAO)DAOContainer.INSTANCE.get(IServiceNameCacheDAO.class.getName());

        String serviceName = Const.EMPTY_STRING;
        try {
            serviceName = CACHE.get(serviceId, () -> dao.getServiceName(serviceId));
        } catch (Throwable e) {
            logger.error(e.getMessage(), e);
        }

        if (StringUtils.isEmpty(serviceName)) {
            serviceName = dao.getServiceName(serviceId);
            if (StringUtils.isNotEmpty(serviceName)) {
                CACHE.put(serviceId, serviceName);
            }
        }

        return serviceName;
    }

    public static String getSplitServiceName(String serviceName) {
        if (StringUtils.isNotEmpty(serviceName)) {
            String[] serviceNames = serviceName.split(Const.ID_SPLIT);
            if (serviceNames.length == 2) {
                return serviceNames[1];
            } else {
                return Const.EMPTY_STRING;
            }
        } else {
            return Const.EMPTY_STRING;
        }
    }
}
