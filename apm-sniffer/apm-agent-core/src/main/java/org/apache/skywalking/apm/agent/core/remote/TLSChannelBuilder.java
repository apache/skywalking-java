/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.apm.agent.core.remote;

import io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.NegotiationType;
import io.grpc.netty.NettyChannelBuilder;
import io.netty.handler.ssl.SslContextBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.skywalking.apm.agent.core.boot.AgentPackageNotFoundException;
import org.apache.skywalking.apm.agent.core.boot.AgentPackagePath;
import org.apache.skywalking.apm.agent.core.conf.Config;
import org.apache.skywalking.apm.agent.core.logging.api.ILog;
import org.apache.skywalking.apm.agent.core.logging.api.LogManager;
import org.apache.skywalking.apm.agent.core.util.PrivateKeyUtil;
import org.apache.skywalking.apm.util.StringUtil;

/**
 * If only ca.crt exists, start TLS. If cert, key and ca files exist, enable mTLS.
 */
public class TLSChannelBuilder implements ChannelBuilder<NettyChannelBuilder> {
    private static final ILog LOGGER = LogManager.getLogger(TLSChannelBuilder.class);

    @Override
    public NettyChannelBuilder build(
        NettyChannelBuilder managedChannelBuilder) throws AgentPackageNotFoundException, IOException {

        File caFile = new File(AgentPackagePath.getPath(), Config.Agent.SSL_TRUSTED_CA_PATH);
        boolean isCAFileExist = caFile.exists() && caFile.isFile();
        if (Config.Agent.FORCE_TLS || isCAFileExist) {
            SslContextBuilder builder = GrpcSslContexts.forClient();

            if (isCAFileExist) {
                String certPath = Config.Agent.SSL_CERT_CHAIN_PATH;
                String keyPath = Config.Agent.SSL_KEY_PATH;
                if (StringUtil.isNotBlank(certPath) && StringUtil.isNotBlank(keyPath)) {
                    File keyFile = new File(AgentPackagePath.getPath(), keyPath);
                    File certFile = new File(AgentPackagePath.getPath(), certPath);

                    if (certFile.isFile() && keyFile.isFile()) {
                        try (InputStream cert = new FileInputStream(certFile);
                             InputStream key = PrivateKeyUtil.loadDecryptionKey(keyFile.getAbsolutePath())) {
                            builder.keyManager(cert, key);
                        }
                    } else if (!certFile.isFile() || !keyFile.isFile()) {
                        LOGGER.warn("Failed to enable mTLS caused by cert or key cannot be found.");
                    }
                }

                builder.trustManager(caFile);
            }
            managedChannelBuilder.negotiationType(NegotiationType.TLS).sslContext(builder.build());
        }
        return managedChannelBuilder;
    }

}
