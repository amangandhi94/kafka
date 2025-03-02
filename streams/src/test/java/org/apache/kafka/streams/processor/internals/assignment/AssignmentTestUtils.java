/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.kafka.streams.processor.internals.assignment;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.internals.KafkaFutureImpl;
import org.apache.kafka.streams.processor.TaskId;
import org.apache.kafka.streams.processor.internals.Task;
import org.apache.kafka.streams.processor.internals.TopologyMetadata.Subtopology;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.apache.kafka.common.utils.Utils.entriesToMap;
import static org.apache.kafka.common.utils.Utils.intersection;
import static org.apache.kafka.streams.processor.internals.assignment.StreamsAssignmentProtocolVersions.LATEST_SUPPORTED_VERSION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class AssignmentTestUtils {

    public static final UUID UUID_1 = uuidForInt(1);
    public static final UUID UUID_2 = uuidForInt(2);
    public static final UUID UUID_3 = uuidForInt(3);
    public static final UUID UUID_4 = uuidForInt(4);
    public static final UUID UUID_5 = uuidForInt(5);
    public static final UUID UUID_6 = uuidForInt(6);
    public static final UUID UUID_7 = uuidForInt(7);
    public static final UUID UUID_8 = uuidForInt(8);
    public static final UUID UUID_9 = uuidForInt(9);

    public static final String RACK_0 = "rock0";
    public static final String RACK_1 = "rock1";
    public static final String RACK_2 = "rock2";
    public static final String RACK_3 = "rock3";
    public static final String RACK_4 = "rock4";

    public static final Node NODE_0 = new Node(0, "node0", 1, RACK_0);
    public static final Node NODE_1 = new Node(1, "node1", 1, RACK_1);
    public static final Node NODE_2 = new Node(2, "node2", 1, RACK_2);
    public static final Node NODE_3 = new Node(2, "node2", 1, RACK_3);
    public static final Node NO_RACK_NODE = new Node(3, "node3", 1);

    public static final Node[] REPLICA_0 = new Node[] {NODE_0, NODE_1};
    public static final Node[] REPLICA_1 = new Node[] {NODE_1, NODE_2};
    public static final Node[] REPLICA_2 = new Node[] {NODE_0, NODE_2};
    public static final Node[] REPLICA_3 = new Node[] {NODE_1, NODE_3};

    public static final String TP_0_NAME = "topic0";
    public static final String TP_1_NAME = "topic1";

    public static final TopicPartition TP_0_0 = new TopicPartition(TP_0_NAME, 0);
    public static final TopicPartition TP_0_1 = new TopicPartition(TP_0_NAME, 1);
    public static final TopicPartition TP_0_2 = new TopicPartition(TP_0_NAME, 2);
    public static final TopicPartition TP_1_0 = new TopicPartition(TP_1_NAME, 0);
    public static final TopicPartition TP_1_1 = new TopicPartition(TP_1_NAME, 1);
    public static final TopicPartition TP_1_2 = new TopicPartition(TP_1_NAME, 2);

    public static final PartitionInfo PI_0_0 = new PartitionInfo(TP_0_NAME, 0, NODE_0, REPLICA_0, REPLICA_0);
    public static final PartitionInfo PI_0_1 = new PartitionInfo(TP_0_NAME, 1, NODE_1, REPLICA_1, REPLICA_1);
    public static final PartitionInfo PI_1_0 = new PartitionInfo(TP_1_NAME, 0, NODE_2, REPLICA_2, REPLICA_2);
    public static final PartitionInfo PI_1_1 = new PartitionInfo(TP_1_NAME, 1, NODE_3, REPLICA_3, REPLICA_3);
    public static final PartitionInfo PI_1_2 = new PartitionInfo(TP_1_NAME, 2, NODE_0, REPLICA_0, REPLICA_0);

    public static final TaskId TASK_0_0 = new TaskId(0, 0);
    public static final TaskId TASK_0_1 = new TaskId(0, 1);
    public static final TaskId TASK_0_2 = new TaskId(0, 2);
    public static final TaskId TASK_0_3 = new TaskId(0, 3);
    public static final TaskId TASK_0_4 = new TaskId(0, 4);
    public static final TaskId TASK_0_5 = new TaskId(0, 5);
    public static final TaskId TASK_0_6 = new TaskId(0, 6);
    public static final TaskId TASK_1_0 = new TaskId(1, 0);
    public static final TaskId TASK_1_1 = new TaskId(1, 1);
    public static final TaskId TASK_1_2 = new TaskId(1, 2);
    public static final TaskId TASK_1_3 = new TaskId(1, 3);
    public static final TaskId TASK_2_0 = new TaskId(2, 0);
    public static final TaskId TASK_2_1 = new TaskId(2, 1);
    public static final TaskId TASK_2_2 = new TaskId(2, 2);
    public static final TaskId TASK_2_3 = new TaskId(2, 3);

    public static final TaskId NAMED_TASK_T0_0_0 = new TaskId(0, 0, "topology0");
    public static final TaskId NAMED_TASK_T0_0_1 = new TaskId(0, 1, "topology0");
    public static final TaskId NAMED_TASK_T0_1_0 = new TaskId(1, 0, "topology0");
    public static final TaskId NAMED_TASK_T0_1_1 = new TaskId(1, 1, "topology0");
    public static final TaskId NAMED_TASK_T1_0_0 = new TaskId(0, 0, "topology1");
    public static final TaskId NAMED_TASK_T1_0_1 = new TaskId(0, 1, "topology1");
    public static final TaskId NAMED_TASK_T2_0_0 = new TaskId(0, 0, "topology2");
    public static final TaskId NAMED_TASK_T2_2_0 = new TaskId(2, 0, "topology2");

    public static final Subtopology SUBTOPOLOGY_0 = new Subtopology(0, null);
    public static final Subtopology SUBTOPOLOGY_1 = new Subtopology(1, null);
    public static final Subtopology SUBTOPOLOGY_2 = new Subtopology(2, null);

    public static final Set<TaskId> EMPTY_TASKS = emptySet();
    public static final Map<TopicPartition, Long> EMPTY_CHANGELOG_END_OFFSETS = new HashMap<>();
    public static final List<String> EMPTY_RACK_AWARE_ASSIGNMENT_TAGS = Collections.emptyList();
    public static final Map<String, String> EMPTY_CLIENT_TAGS = Collections.emptyMap();

    private AssignmentTestUtils() {}

    static Map<UUID, ClientState> getClientStatesMap(final ClientState... states) {
        final Map<UUID, ClientState> clientStates = new HashMap<>();
        int nthState = 1;
        for (final ClientState state : states) {
            clientStates.put(uuidForInt(nthState), state);
            ++nthState;
        }
        return clientStates;
    }

    // If you don't care about setting the end offsets for each specific topic partition, the helper method
    // getTopicPartitionOffsetMap is useful for building this input map for all partitions
    public static AdminClient createMockAdminClientForAssignor(final Map<TopicPartition, Long> changelogEndOffsets) {
        final AdminClient adminClient = mock(AdminClient.class);

        final ListOffsetsResult result = mock(ListOffsetsResult.class);
        when(adminClient.listOffsets(any())).thenReturn(result);
        for (final Map.Entry<TopicPartition, Long> entry : changelogEndOffsets.entrySet()) {
            final KafkaFutureImpl<ListOffsetsResultInfo> partitionFuture = new KafkaFutureImpl<>();
            final ListOffsetsResultInfo info = mock(ListOffsetsResultInfo.class);
            lenient().when(info.offset()).thenReturn(entry.getValue());
            partitionFuture.complete(info);
            lenient().when(result.partitionResult(entry.getKey())).thenReturn(partitionFuture);
        }

        return adminClient;
    }

    public static SubscriptionInfo getInfo(final UUID processId,
                                           final Set<TaskId> prevTasks,
                                           final Set<TaskId> standbyTasks) {
        return new SubscriptionInfo(
            LATEST_SUPPORTED_VERSION, LATEST_SUPPORTED_VERSION, processId, null, getTaskOffsetSums(prevTasks, standbyTasks), (byte) 0, 0, EMPTY_CLIENT_TAGS);
    }

    public static SubscriptionInfo getInfo(final UUID processId,
                                           final Set<TaskId> prevTasks,
                                           final Set<TaskId> standbyTasks,
                                           final String userEndPoint) {
        return new SubscriptionInfo(
            LATEST_SUPPORTED_VERSION, LATEST_SUPPORTED_VERSION, processId, userEndPoint, getTaskOffsetSums(prevTasks, standbyTasks), (byte) 0, 0, EMPTY_CLIENT_TAGS);
    }

    public static SubscriptionInfo getInfo(final UUID processId,
                                           final Set<TaskId> prevTasks,
                                           final Set<TaskId> standbyTasks,
                                           final byte uniqueField) {
        return new SubscriptionInfo(
            LATEST_SUPPORTED_VERSION, LATEST_SUPPORTED_VERSION, processId, null, getTaskOffsetSums(prevTasks, standbyTasks), uniqueField, 0, EMPTY_CLIENT_TAGS);
    }

    public static SubscriptionInfo getInfo(final UUID processId,
                                           final Set<TaskId> prevTasks,
                                           final Set<TaskId> standbyTasks,
                                           final byte uniqueField,
                                           final Map<String, String> clientTags) {
        return new SubscriptionInfo(
            LATEST_SUPPORTED_VERSION, LATEST_SUPPORTED_VERSION, processId, null, getTaskOffsetSums(prevTasks, standbyTasks), uniqueField, 0, clientTags);
    }

    // Stub offset sums for when we only care about the prev/standby task sets, not the actual offsets
    private static Map<TaskId, Long> getTaskOffsetSums(final Collection<TaskId> activeTasks, final Collection<TaskId> standbyTasks) {
        final Map<TaskId, Long> taskOffsetSums = activeTasks.stream().collect(Collectors.toMap(t -> t, t -> Task.LATEST_OFFSET));
        taskOffsetSums.putAll(standbyTasks.stream().collect(Collectors.toMap(t -> t, t -> 0L)));
        return taskOffsetSums;
    }

    /**
     * Builds a UUID by repeating the given number n. For valid n, it is guaranteed that the returned UUIDs satisfy
     * the same relation relative to others as their parameter n does: iff n < m, then uuidForInt(n) < uuidForInt(m)
     */
    public static UUID uuidForInt(final int n) {
        return new UUID(0, n);
    }

    static void assertValidAssignment(final int numStandbyReplicas,
                                      final Set<TaskId> statefulTasks,
                                      final Set<TaskId> statelessTasks,
                                      final Map<UUID, ClientState> assignedStates,
                                      final StringBuilder failureContext) {
        assertValidAssignment(
            numStandbyReplicas,
            0,
            statefulTasks,
            statelessTasks,
            assignedStates,
            failureContext
        );
    }

    static void assertValidAssignment(final int numStandbyReplicas,
                                      final int maxWarmupReplicas,
                                      final Set<TaskId> statefulTasks,
                                      final Set<TaskId> statelessTasks,
                                      final Map<UUID, ClientState> assignedStates,
                                      final StringBuilder failureContext) {
        final Map<TaskId, Set<UUID>> assignments = new TreeMap<>();
        for (final TaskId taskId : statefulTasks) {
            assignments.put(taskId, new TreeSet<>());
        }
        for (final TaskId taskId : statelessTasks) {
            assignments.put(taskId, new TreeSet<>());
        }
        for (final Map.Entry<UUID, ClientState> entry : assignedStates.entrySet()) {
            validateAndAddActiveAssignments(statefulTasks, statelessTasks, failureContext, assignments, entry);
            validateAndAddStandbyAssignments(statefulTasks, statelessTasks, failureContext, assignments, entry);
        }

        final AtomicInteger remainingWarmups = new AtomicInteger(maxWarmupReplicas);

        final TreeMap<TaskId, Set<UUID>> misassigned =
            assignments
                .entrySet()
                .stream()
                .filter(entry -> {
                    final int expectedActives = 1;
                    final boolean isStateless = statelessTasks.contains(entry.getKey());
                    final int expectedStandbys = isStateless ? 0 : numStandbyReplicas;
                    // We'll never assign even the expected number of standbys if they don't actually fit in the cluster
                    final int expectedAssignments = Math.min(
                        assignedStates.size(),
                        expectedActives + expectedStandbys
                    );
                    final int actualAssignments = entry.getValue().size();
                    if (actualAssignments == expectedAssignments) {
                        return false; // not misassigned
                    } else {
                        if (actualAssignments == expectedAssignments + 1 && remainingWarmups.get() > 0) {
                            remainingWarmups.getAndDecrement();
                            return false; // it's a warmup, so it's fine
                        } else {
                            return true; // misassigned
                        }
                    }
                })
                .collect(entriesToMap(TreeMap::new));

        if (!misassigned.isEmpty()) {
            assertThat(
                new StringBuilder().append("Found some over- or under-assigned tasks in the final assignment with ")
                                   .append(numStandbyReplicas)
                                   .append(" and max warmups ")
                                   .append(maxWarmupReplicas)
                                   .append(" standby replicas, stateful tasks:")
                                   .append(statefulTasks)
                                   .append(", and stateless tasks:")
                                   .append(statelessTasks)
                                   .append(failureContext)
                                   .toString(),
                misassigned,
                is(emptyMap()));
        }
    }

    private static void validateAndAddStandbyAssignments(final Set<TaskId> statefulTasks,
                                                         final Set<TaskId> statelessTasks,
                                                         final StringBuilder failureContext,
                                                         final Map<TaskId, Set<UUID>> assignments,
                                                         final Map.Entry<UUID, ClientState> entry) {
        for (final TaskId standbyTask : entry.getValue().standbyTasks()) {
            if (statelessTasks.contains(standbyTask)) {
                throw new AssertionError(
                    new StringBuilder().append("Found a standby task for stateless task ")
                                       .append(standbyTask)
                                       .append(" on client ")
                                       .append(entry)
                                       .append(" stateless tasks:")
                                       .append(statelessTasks)
                                       .append(failureContext)
                                       .toString()
                );
            } else if (assignments.containsKey(standbyTask)) {
                assignments.get(standbyTask).add(entry.getKey());
            } else {
                throw new AssertionError(
                    new StringBuilder().append("Found an extra standby task ")
                                       .append(standbyTask)
                                       .append(" on client ")
                                       .append(entry)
                                       .append(" but expected stateful tasks:")
                                       .append(statefulTasks)
                                       .append(failureContext)
                                       .toString()
                );
            }
        }
    }

    private static void validateAndAddActiveAssignments(final Set<TaskId> statefulTasks,
                                                        final Set<TaskId> statelessTasks,
                                                        final StringBuilder failureContext,
                                                        final Map<TaskId, Set<UUID>> assignments,
                                                        final Map.Entry<UUID, ClientState> entry) {
        for (final TaskId activeTask : entry.getValue().activeTasks()) {
            if (assignments.containsKey(activeTask)) {
                assignments.get(activeTask).add(entry.getKey());
            } else {
                throw new AssertionError(
                    new StringBuilder().append("Found an extra active task ")
                                       .append(activeTask)
                                       .append(" on client ")
                                       .append(entry)
                                       .append(" but expected stateful tasks:")
                                       .append(statefulTasks)
                                       .append(" and stateless tasks:")
                                       .append(statelessTasks)
                                       .append(failureContext)
                                       .toString()
                );
            }
        }
    }

    static void assertBalancedStatefulAssignment(final Set<TaskId> allStatefulTasks,
                                                 final Map<UUID, ClientState> clientStates,
                                                 final StringBuilder failureContext) {
        double maxStateful = Double.MIN_VALUE;
        double minStateful = Double.MAX_VALUE;
        for (final ClientState clientState : clientStates.values()) {
            final Set<TaskId> statefulTasks =
                intersection(HashSet::new, clientState.assignedTasks(), allStatefulTasks);
            final double statefulTaskLoad = 1.0 * statefulTasks.size() / clientState.capacity();
            maxStateful = Math.max(maxStateful, statefulTaskLoad);
            minStateful = Math.min(minStateful, statefulTaskLoad);
        }
        final double statefulDiff = maxStateful - minStateful;

        if (statefulDiff > 1.0) {
            final StringBuilder builder = new StringBuilder()
                .append("detected a stateful assignment balance factor violation: ")
                .append(statefulDiff)
                .append(">")
                .append(1.0)
                .append(" in: ");
            appendClientStates(builder, clientStates);
            fail(builder.append(failureContext).toString());
        }
    }

    static void assertBalancedActiveAssignment(final Map<UUID, ClientState> clientStates,
                                               final StringBuilder failureContext) {
        double maxActive = Double.MIN_VALUE;
        double minActive = Double.MAX_VALUE;
        for (final ClientState clientState : clientStates.values()) {
            final double activeTaskLoad = clientState.activeTaskLoad();
            maxActive = Math.max(maxActive, activeTaskLoad);
            minActive = Math.min(minActive, activeTaskLoad);
        }
        final double activeDiff = maxActive - minActive;
        if (activeDiff > 1.0) {
            final StringBuilder builder = new StringBuilder()
                .append("detected an active assignment balance factor violation: ")
                .append(activeDiff)
                .append(">")
                .append(1.0)
                .append(" in: ");
            appendClientStates(builder, clientStates);
            fail(builder.append(failureContext).toString());
        }
    }

    static void assertBalancedTasks(final Map<UUID, ClientState> clientStates) {
        final TaskSkewReport taskSkewReport = analyzeTaskAssignmentBalance(clientStates);
        if (taskSkewReport.totalSkewedTasks() > 0) {
            fail("Expected a balanced task assignment, but was: " + taskSkewReport);
        }
    }

    static TaskSkewReport analyzeTaskAssignmentBalance(final Map<UUID, ClientState> clientStates) {
        final Function<Integer, Map<UUID, AtomicInteger>> initialClientCounts =
            i -> clientStates.keySet().stream().collect(Collectors.toMap(c -> c, c -> new AtomicInteger(0)));

        final Map<Integer, Map<UUID, AtomicInteger>> subtopologyToClientsWithPartition = new TreeMap<>();
        for (final Map.Entry<UUID, ClientState> entry : clientStates.entrySet()) {
            final UUID client = entry.getKey();
            final ClientState clientState = entry.getValue();
            for (final TaskId task : clientState.activeTasks()) {
                final int subtopology = task.subtopology();
                subtopologyToClientsWithPartition
                    .computeIfAbsent(subtopology, initialClientCounts)
                    .get(client)
                    .incrementAndGet();
            }
        }

        int maxTaskSkew = 0;
        final Set<Integer> skewedSubtopologies = new TreeSet<>();

        for (final Map.Entry<Integer, Map<UUID, AtomicInteger>> entry : subtopologyToClientsWithPartition.entrySet()) {
            final Map<UUID, AtomicInteger> clientsWithPartition = entry.getValue();
            int max = Integer.MIN_VALUE;
            int min = Integer.MAX_VALUE;
            for (final AtomicInteger count : clientsWithPartition.values()) {
                max = Math.max(max, count.get());
                min = Math.min(min, count.get());
            }
            final int taskSkew = max - min;
            maxTaskSkew = Math.max(maxTaskSkew, taskSkew);
            if (taskSkew > 1) {
                skewedSubtopologies.add(entry.getKey());
            }
        }

        return new TaskSkewReport(maxTaskSkew, skewedSubtopologies, subtopologyToClientsWithPartition);
    }

    static Matcher<ClientState> hasAssignedTasks(final int taskCount) {
        return hasProperty("assignedTasks", ClientState::assignedTaskCount, taskCount);
    }

    static Matcher<ClientState> hasActiveTasks(final int taskCount) {
        return hasProperty("activeTasks", ClientState::activeTaskCount, taskCount);
    }

    static Matcher<ClientState> hasStandbyTasks(final int taskCount) {
        return hasProperty("standbyTasks", ClientState::standbyTaskCount, taskCount);
    }

    static <V> Matcher<ClientState> hasProperty(final String propertyName,
                                                final Function<ClientState, V> propertyExtractor,
                                                final V propertyValue) {
        return new BaseMatcher<ClientState>() {
            @Override
            public void describeTo(final Description description) {
                description.appendText(propertyName).appendText(":").appendValue(propertyValue);
            }

            @Override
            public boolean matches(final Object actual) {
                if (actual instanceof ClientState) {
                    return Objects.equals(propertyExtractor.apply((ClientState) actual), propertyValue);
                } else {
                    return false;
                }
            }
        };
    }

    static void appendClientStates(final StringBuilder stringBuilder,
                                   final Map<UUID, ClientState> clientStates) {
        stringBuilder.append('{').append('\n');
        for (final Map.Entry<UUID, ClientState> entry : clientStates.entrySet()) {
            stringBuilder.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
        }
        stringBuilder.append('}').append('\n');
    }

    static final class TaskSkewReport {
        private final int maxTaskSkew;
        private final Set<Integer> skewedSubtopologies;
        private final Map<Integer, Map<UUID, AtomicInteger>> subtopologyToClientsWithPartition;

        private TaskSkewReport(final int maxTaskSkew,
                               final Set<Integer> skewedSubtopologies,
                               final Map<Integer, Map<UUID, AtomicInteger>> subtopologyToClientsWithPartition) {
            this.maxTaskSkew = maxTaskSkew;
            this.skewedSubtopologies = skewedSubtopologies;
            this.subtopologyToClientsWithPartition = subtopologyToClientsWithPartition;
        }

        int totalSkewedTasks() {
            return skewedSubtopologies.size();
        }

        Set<Integer> skewedSubtopologies() {
            return skewedSubtopologies;
        }

        @Override
        public String toString() {
            return "TaskSkewReport{" +
                "maxTaskSkew=" + maxTaskSkew +
                ", skewedSubtopologies=" + skewedSubtopologies +
                ", subtopologyToClientsWithPartition=" + subtopologyToClientsWithPartition +
                '}';
        }
    }
}
