/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.qpid.jms.provider.failover;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * Test for the behavior of the FailoverUriPool
 */
public class FailoverUriPoolTest {

    private List<URI> uris;

    @Before
    public void setUp() throws Exception {
        uris = new ArrayList<URI>();

        uris.add(new URI("tcp://192.168.2.1:5672"));
        uris.add(new URI("tcp://192.168.2.2:5672"));
        uris.add(new URI("tcp://192.168.2.3:5672"));
        uris.add(new URI("tcp://192.168.2.4:5672"));
    }

    @Test
    public void testCreateEmptyPool() {
        FailoverUriPool pool = new FailoverUriPool();
        assertEquals(FailoverUriPool.DEFAULT_RANDOMIZE_ENABLED, pool.isRandomize());

        assertTrue(pool.isEmpty());
        assertEquals(0, pool.size());
        assertNotNull(pool.getNestedOptions());
        assertTrue(pool.getNestedOptions().isEmpty());
    }

    @Test
    public void testCreateEmptyPoolFromNullUris() {
        FailoverUriPool pool = new FailoverUriPool(null, null);
        assertEquals(FailoverUriPool.DEFAULT_RANDOMIZE_ENABLED, pool.isRandomize());

        assertNotNull(pool.getNestedOptions());
        assertTrue(pool.getNestedOptions().isEmpty());
    }

    @Test
    public void testCreateEmptyPoolWithURIs() throws URISyntaxException {
        FailoverUriPool pool = new FailoverUriPool(uris.toArray(new URI[0]), null);
        assertEquals(FailoverUriPool.DEFAULT_RANDOMIZE_ENABLED, pool.isRandomize());

        assertNotNull(pool.getNestedOptions());
        assertTrue(pool.getNestedOptions().isEmpty());
    }

    @Test
    public void testGetNextFromEmptyPool() {
        FailoverUriPool pool = new FailoverUriPool();
        assertEquals(FailoverUriPool.DEFAULT_RANDOMIZE_ENABLED, pool.isRandomize());

        assertNull(pool.getNext());
    }

    @Test
    public void testGetNextFromSingleValuePool() {
        FailoverUriPool pool = new FailoverUriPool(new URI[] {uris.get(0) }, null);

        assertEquals(uris.get(0), pool.getNext());
        assertEquals(uris.get(0), pool.getNext());
        assertEquals(uris.get(0), pool.getNext());
    }

    @Test
    public void testAddUriToEmptyPool() {
        FailoverUriPool pool = new FailoverUriPool();
        assertTrue(pool.isEmpty());
        pool.add(uris.get(0));
        assertFalse(pool.isEmpty());
        assertEquals(uris.get(0), pool.getNext());
    }

    @Test
    public void testDuplicatesNotAdded() {
        FailoverUriPool pool = new FailoverUriPool(uris.toArray(new URI[0]), null);

        assertEquals(uris.size(), pool.size());
        pool.add(uris.get(0));
        assertEquals(uris.size(), pool.size());
        pool.add(uris.get(1));
        assertEquals(uris.size(), pool.size());
    }

    @Test
    public void testDuplicatesNotAddedWhenQueryPresent() throws URISyntaxException {
        FailoverUriPool pool = new FailoverUriPool();

        assertTrue(pool.isEmpty());
        pool.add(new URI("tcp://127.0.0.1:5672?transport.tcpNoDelay=true"));
        assertFalse(pool.isEmpty());

        assertEquals(1, pool.size());
        pool.add(new URI("tcp://localhost:5672?transport.tcpNoDelay=true"));
        assertEquals(1, pool.size());

        assertEquals(1, pool.size());
        pool.add(new URI("tcp://localhost:5672?transport.tcpNoDelay=false"));
        assertEquals(1, pool.size());
    }

    @Test
    public void testDuplicatesNotAddedWithHostResolution() throws URISyntaxException {
        FailoverUriPool pool = new FailoverUriPool();

        assertTrue(pool.isEmpty());
        pool.add(new URI("tcp://127.0.0.1:5672"));
        assertFalse(pool.isEmpty());

        assertEquals(1, pool.size());
        pool.add(new URI("tcp://localhost:5672"));
        assertEquals(1, pool.size());

        assertEquals(1, pool.size());
        pool.add(new URI("tcp://localhost:5673"));
        assertEquals(2, pool.size());
    }

    @Test
    public void testDuplicatesNotAddedUnresolvable() throws URISyntaxException {
        FailoverUriPool pool = new FailoverUriPool();

        assertTrue(pool.isEmpty());
        pool.add(new URI("tcp://shouldbeunresolvable:5672"));
        assertFalse(pool.isEmpty());

        assertEquals(1, pool.size());
        pool.add(new URI("tcp://shouldbeunresolvable:5672"));
        assertEquals(1, pool.size());

        assertEquals(1, pool.size());
        pool.add(new URI("tcp://SHOULDBEUNRESOLVABLE:5672"));
        assertEquals(1, pool.size());

        assertEquals(1, pool.size());
        pool.add(new URI("tcp://SHOULDBEUNRESOLVABLE2:5672"));
        assertEquals(2, pool.size());
    }

    @Test
    public void testDuplicatesNotAddedWhenQueryPresentAndUnresolveable() throws URISyntaxException {
        FailoverUriPool pool = new FailoverUriPool();

        assertTrue(pool.isEmpty());
        pool.add(new URI("tcp://shouldbeunresolvable:5672?transport.tcpNoDelay=true"));
        assertFalse(pool.isEmpty());

        assertEquals(1, pool.size());
        pool.add(new URI("tcp://shouldbeunresolvable:5672?transport.tcpNoDelay=false"));
        assertEquals(1, pool.size());

        assertEquals(1, pool.size());
        pool.add(new URI("tcp://SHOULDBEUNRESOLVABLE:5672?transport.tcpNoDelay=true"));
        assertEquals(1, pool.size());

        assertEquals(1, pool.size());
        pool.add(new URI("tcp://SHOULDBEUNRESOLVABLE2:5672?transport.tcpNoDelay=true"));
        assertEquals(2, pool.size());
    }

    @Test
    public void testAddUriToPoolRandomized() throws URISyntaxException {
        URI newUri = new URI("tcp://192.168.2." + (uris.size() + 1) + ":5672");

        FailoverUriPool pool = new FailoverUriPool(uris.toArray(new URI[0]), null);
        pool.setRandomize(true);
        pool.add(newUri);

        URI found = null;

        for (int i = 0; i < uris.size() + 1; ++i) {
            URI next = pool.getNext();
            if (newUri.equals(next)) {
                found = next;
            }
        }

        if (found == null) {
            fail("URI added was not retrieved from the pool");
        }
    }

    @Test
    public void testAddUriToPoolNotRandomized() throws URISyntaxException {
        URI newUri = new URI("tcp://192.168.2." + (uris.size() + 1) + ":5672");

        FailoverUriPool pool = new FailoverUriPool(uris.toArray(new URI[0]), null);
        pool.setRandomize(false);
        pool.add(newUri);

        for (int i = 0; i < uris.size(); ++i) {
            assertNotEquals(newUri, pool.getNext());
        }

        assertEquals(newUri, pool.getNext());
    }

    @Test
    public void testRemoveURIFromPool() throws URISyntaxException {
        FailoverUriPool pool = new FailoverUriPool(uris.toArray(new URI[0]), null);
        pool.setRandomize(false);

        URI removed = uris.get(0);

        pool.remove(removed);

        for (int i = 0; i < uris.size() + 1; ++i) {
            if (removed.equals(pool.getNext())) {
                fail("URI was not removed from the pool");
            }
        }
    }

    @Test
    public void testConnectedShufflesWhenRandomizing() {
        assertConnectedEffectOnPool(true, true);
    }

    @Test
    public void testConnectedDoesNotShufflesWhenNoRandomizing() {
        assertConnectedEffectOnPool(false, false);
    }

    private void assertConnectedEffectOnPool(boolean randomize, boolean shouldShuffle) {

        FailoverUriPool pool = new FailoverUriPool(uris.toArray(new URI[0]), null);
        pool.setRandomize(randomize);

        List<URI> current = new ArrayList<URI>();
        List<URI> previous = new ArrayList<URI>();

        boolean shuffled = false;

        for (int i = 0; i < 10; ++i) {

            for (int j = 0; j < uris.size(); ++j) {
                current.add(pool.getNext());
            }

            pool.connected();

            if (!previous.isEmpty() && !previous.equals(current)) {
                shuffled = true;
                break;
            }

            previous.clear();
            previous.addAll(current);
            current.clear();
        }

        if (shouldShuffle) {
            assertTrue("URIs did not get randomized", shuffled);
        } else {
            assertFalse("URIs should not get randomized", shuffled);
        }
    }

    @Test
    public void testAddOrRemoveNullHasNoAffect() throws URISyntaxException {
        FailoverUriPool pool = new FailoverUriPool(uris.toArray(new URI[0]), null);
        assertEquals(uris.size(), pool.size());

        pool.add(null);
        assertEquals(uris.size(), pool.size());
        pool.remove(null);
        assertEquals(uris.size(), pool.size());
    }
}