/*
 * Copyright (c) 2016 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.couchbase.client.core.config;

import com.couchbase.client.core.config.parser.BucketConfigParser;
import com.couchbase.client.core.env.CoreEnvironment;
import com.couchbase.client.core.service.ServiceType;
import com.couchbase.client.core.util.Resources;
import com.couchbase.client.core.utils.NetworkAddress;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.mockito.Mockito.mock;

/**
 * Verifies that parsing various bucket configs works as expected through the
 * {@link BucketConfigParser}.
 */
public class DefaultCouchbaseBucketConfigTest {

    @Test
    public void shouldHavePrimaryPartitionsOnNode() {
        String raw = Resources.read("config_with_mixed_partitions.json", getClass());
        CouchbaseBucketConfig config = (CouchbaseBucketConfig)
            BucketConfigParser.parse(raw, mock(CoreEnvironment.class));

        assertTrue(config.hasPrimaryPartitionsOnNode(NetworkAddress.create("1.2.3.4")));
        assertFalse(config.hasPrimaryPartitionsOnNode(NetworkAddress.create("2.3.4.5")));
        assertEquals(BucketNodeLocator.VBUCKET, config.locator());
        assertFalse(config.ephemeral());
    }

    @Test
    public void shouldFallbackToNodeHostnameIfNotInNodesExt() {
        String raw = Resources.read("nodes_ext_without_hostname.json", getClass());
        CouchbaseBucketConfig config = (CouchbaseBucketConfig)
            BucketConfigParser.parse(raw, mock(CoreEnvironment.class));

        NetworkAddress expected = NetworkAddress.create("1.2.3.4");
        assertEquals(1, config.nodes().size());
        assertEquals(expected, config.nodes().get(0).hostname());
        assertEquals(BucketNodeLocator.VBUCKET, config.locator());
        assertFalse(config.ephemeral());
    }

    @Test
    public void shouldGracefullyHandleEmptyPartitions() {
        String raw = Resources.read("config_with_no_partitions.json", getClass());
        CouchbaseBucketConfig config = (CouchbaseBucketConfig)
            BucketConfigParser.parse(raw, mock(CoreEnvironment.class));

        assertEquals(DefaultCouchbaseBucketConfig.PARTITION_NOT_EXISTENT, config.nodeIndexForMaster(24, false));
        assertEquals(DefaultCouchbaseBucketConfig.PARTITION_NOT_EXISTENT, config.nodeIndexForReplica(24, 1, false));
        assertFalse(config.ephemeral());
    }

    @Test
    public void shouldLoadEphemeralBucketConfig() {
        String raw = Resources.read("ephemeral_bucket_config.json", getClass());
        CouchbaseBucketConfig config = (CouchbaseBucketConfig)
            BucketConfigParser.parse(raw, mock(CoreEnvironment.class));

        assertTrue(config.ephemeral());
        assertTrue(config.serviceEnabled(ServiceType.BINARY));
        assertFalse(config.serviceEnabled(ServiceType.VIEW));
    }

    @Test
    public void shouldLoadConfigWithoutBucketCapabilities() {
        String raw = Resources.read("config_without_capabilities.json", getClass());
        CouchbaseBucketConfig config = (CouchbaseBucketConfig)
            BucketConfigParser.parse(raw, mock(CoreEnvironment.class));

        assertFalse(config.ephemeral());
        assertEquals(0, config.numberOfReplicas());
        assertEquals(64, config.numberOfPartitions());
        assertEquals(2, config.nodes().size());
        assertTrue(config.serviceEnabled(ServiceType.BINARY));
        assertTrue(config.serviceEnabled(ServiceType.VIEW));
    }

    @Test
    public void shouldLoadConfigWithSameNodesButDifferentPorts() {
        String raw = Resources.read("cluster_run_two_nodes_same_host.json", getClass());
        CouchbaseBucketConfig config = (CouchbaseBucketConfig)
            BucketConfigParser.parse(raw, mock(CoreEnvironment.class));

        assertFalse(config.ephemeral());
        assertEquals(1, config.numberOfReplicas());
        assertEquals(1024, config.numberOfPartitions());
        assertEquals(2, config.nodes().size());
        assertEquals("192.168.1.194", config.nodes().get(0).hostname().address());
        assertEquals(9000, (int)config.nodes().get(0).services().get(ServiceType.CONFIG));
        assertEquals("192.168.1.194", config.nodes().get(1).hostname().address());
        assertEquals(9001, (int)config.nodes().get(1).services().get(ServiceType.CONFIG));
    }

    @Test
    public void shouldLoadConfigWithMDS() {
        String raw = Resources.read("cluster_run_three_nodes_mds_with_localhost.json", getClass());
        CouchbaseBucketConfig config = (CouchbaseBucketConfig)
            BucketConfigParser.parse(raw, mock(CoreEnvironment.class));

        assertEquals(3, config.nodes().size());
        assertEquals("192.168.0.102", config.nodes().get(0).hostname().address());
        assertEquals("127.0.0.1", config.nodes().get(1).hostname().address());
        assertEquals("127.0.0.1", config.nodes().get(2).hostname().address());
        assertTrue(config.nodes().get(0).services().containsKey(ServiceType.BINARY));
        assertTrue(config.nodes().get(1).services().containsKey(ServiceType.BINARY));
        assertFalse(config.nodes().get(2).services().containsKey(ServiceType.BINARY));
    }

    @Test
    public void shouldLoadConfigWithIPv6() {
        assumeFalse(NetworkAddress.FORCE_IPV4);

        String raw = Resources.read("config_with_ipv6.json", getClass());
        CouchbaseBucketConfig config = (CouchbaseBucketConfig)
            BucketConfigParser.parse(raw, mock(CoreEnvironment.class));

        assertEquals(2, config.nodes().size());
        assertEquals("fd63:6f75:6368:2068:1471:75ff:fe25:a8be", config.nodes().get(0).hostname().address());
        assertEquals("fd63:6f75:6368:2068:c490:b5ff:fe86:9cf7", config.nodes().get(1).hostname().address());

        assertEquals(1, config.numberOfReplicas());
        assertEquals(1024, config.numberOfPartitions());
    }

    /**
     * This is a regression test. It has been added to make sure a config with a bucket
     * capability that is not known to the client still makes it parse properly.
     */
    @Test
    public void shouldIgnoreUnknownBucketCapabilities() {
        String raw = Resources.read("config_with_invalid_capability.json", getClass());
        BucketConfig config = BucketConfigParser.parse(raw, mock(CoreEnvironment.class));
        assertEquals(1, config.nodes().size());
    }

    @Test
    public void shouldReadBucketUuid() {
        String raw = Resources.read("config_with_mixed_partitions.json", getClass());
        CouchbaseBucketConfig config = (CouchbaseBucketConfig)
            BucketConfigParser.parse(raw, mock(CoreEnvironment.class));

        assertEquals("aa4b515529fa706f1e5f09f21abb5c06", config.uuid());
    }

    @Test
    public void shouldHandleMissingBucketUuid() throws Exception {
        String raw = Resources.read("config_without_uuid.json", getClass());
        CouchbaseBucketConfig config = (CouchbaseBucketConfig)
                BucketConfigParser.parse(raw, mock(CoreEnvironment.class));

        assertNull(config.uuid());
    }
}
