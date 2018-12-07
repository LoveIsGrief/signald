package io.finn.signald;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.fail;

import org.newsclub.net.unix.AFUNIXSocket;

import java.io.PrintWriter;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestRequest {

    private Thread signaldMain;
    private static String SOCKET_PATH = "/tmp/signald.sock";
    private AFUNIXSocket socket;
    private PrintWriter writer;
    private PrintReader reader;

    @BeforeAll
    public void startSignald() {
        this.signaldMain = new Thread(new RunnableMain(SOCKET_PATH), "main");
        this.signaldMain.start();
    }

    @AfterAll
    public void stopSignald() {
        this.signaldMain.interrupt();
    }

    @BeforeEach
    public void connectSocket() {
        this.socket.connect(SOCKET_PATH);
        this.writer = new PrintWriter(this.socket.getOutputStream);
        this.reader = new PrintReader(this.socket.getInputStream);
        System.out.println(this.reader.readLine());
    }


    @AfterEach
    public void disconnectSocket() {
        this.socket.disconnect();
    }

    @DisplayName("Register a new account")
    @Test
    public void testRegister() {
        this.writer.println("{\"type\": \"register\", \"username\": \"tbd\"}");
    }

}
