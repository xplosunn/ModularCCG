package unit.client.network;

import client.network.ServerHandler;
import org.junit.Test;
import unit.UnitTestConstants;
import unit.common.network.messages.FakeServer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Created by xs on 23-09-2014.
 */
public class ServerHandlerTest {

    @Test
    public void connectCases(){
        String username = "MrTest";
        assertEquals(false, new ServerHandler(username).connect()._1());
        FakeServer server = new FakeServer();
        server.start();
        assertTrue(server.running);
        assertEquals(true, new ServerHandler(username).connect()._1());
        assertEquals(false, new ServerHandler(username).connect()._1());
        server.shutdown();
        try {
            Thread.sleep(UnitTestConstants.processMillis());
        } catch (InterruptedException e) {
            fail();
        }
        server = new FakeServer();
        server.start();
        assertTrue(server.running);
        try {
            Thread.sleep(UnitTestConstants.processMillis());
        } catch (InterruptedException e) {
            fail();
        }
        assertEquals(true, new ServerHandler(username).connect()._1());
        assertEquals(false, new ServerHandler(username).connect()._1());
        server.shutdown();
    }
}
