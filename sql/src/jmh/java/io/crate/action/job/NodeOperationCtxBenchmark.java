/*
 * Licensed to Crate under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.  Crate licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial
 * agreement.
 */

package io.crate.action.job;

import com.carrotsearch.hppc.cursors.IntCursor;
import io.crate.executor.transport.NodeOperationTreeGenerator;
import io.crate.operation.NodeOperation;
import io.crate.operation.NodeOperationTree;
import io.crate.planner.ExecutionPlan;
import io.crate.testing.DiscoveryNodes;
import io.crate.testing.SQLExecutor;
import org.elasticsearch.cluster.service.ClusterService;
import org.elasticsearch.common.settings.ClusterSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.util.set.Sets;
import org.elasticsearch.threadpool.TestThreadPool;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.AverageTime, Mode.SingleShotTime})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 5)
@Measurement(iterations = 10)
@State(Scope.Benchmark)
public class NodeOperationCtxBenchmark {

    private TestThreadPool threadPool;
    private Collection<NodeOperation> nodeOperations;

    @Setup
    public void setupNodeOperations() {
        threadPool = new TestThreadPool("testing");
        SQLExecutor e = SQLExecutor.builder(
            new ClusterService(Settings.builder().put("cluster.name", "ClusterServiceTests").build(),
                new ClusterSettings(Settings.EMPTY, Sets.newHashSet(ClusterSettings.BUILT_IN_CLUSTER_SETTINGS)),
                threadPool,
                () -> DiscoveryNodes.newNode("benchmarkNode")))
            .build();
        ExecutionPlan executionPlan = e.plan("select name from sys.cluster group by name");

        NodeOperationTree nodeOperationTree = NodeOperationTreeGenerator.fromPlan(executionPlan, "noop_id");
        nodeOperations = nodeOperationTree.nodeOperations();
    }

    @TearDown
    public void cleanup() throws InterruptedException {
        threadPool.shutdown();
        threadPool.awaitTermination(20, TimeUnit.SECONDS);
    }

    @Benchmark
    public Iterable<? extends IntCursor> measureCreateNodeOperationCtxPlusFindLeafs() {
        ContextPreparer.NodeOperationCtx ctx = new ContextPreparer.NodeOperationCtx("noop_id", nodeOperations);
        return ctx.findLeafs();
    }
}
