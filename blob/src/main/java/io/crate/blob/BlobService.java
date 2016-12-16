/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package io.crate.blob;

import io.crate.blob.pending_transfer.BlobHeadRequestHandler;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.indices.recovery.BlobRecoverySource;
import org.elasticsearch.transport.TransportService;

public class BlobService extends AbstractLifecycleComponent<BlobService> {

    private final Injector injector;
    private final BlobHeadRequestHandler blobHeadRequestHandler;

    @Inject
    public BlobService(Settings settings,
                       Injector injector,
                       BlobHeadRequestHandler blobHeadRequestHandler) {
        super(settings);
        this.injector = injector;
        this.blobHeadRequestHandler = blobHeadRequestHandler;
    }

    public RemoteDigestBlob newBlob(String index, String digest) {
        return new RemoteDigestBlob(this, index, digest);
    }

    public Injector getInjector() {
        return injector;
    }

    @Override
    protected void doStart() throws ElasticsearchException {
        logger.info("BlobService.doStart() {}", this);

        // suppress warning about replaced recovery handler
        ESLogger transportServiceLogger = Loggers.getLogger(TransportService.class);
        String previousLevel = transportServiceLogger.getLevel();
        transportServiceLogger.setLevel("ERROR");
        injector.getInstance(BlobRecoverySource.class).registerHandler();
        transportServiceLogger.setLevel(previousLevel);

        blobHeadRequestHandler.registerHandler();

        if (!settings.getAsBoolean("http.enabled", true)) {
            logger.warn("Http server should be enabled for blob support");
        }
    }

    @Override
    protected void doStop() throws ElasticsearchException {
    }

    @Override
    protected void doClose() throws ElasticsearchException {
    }
}
