package io.finn.signald;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInstance;

import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import java.io.File;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestRequest {

    private Thread signaldMain;
    private static File SOCKET_FILE = new File(System.getProperty("java.io.tmpdir"), "signald.sock");
    private AFUNIXSocket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    @BeforeAll
    public void startSignald() throws InterruptedException {
        this.signaldMain = new Thread(new RunnableMain(SOCKET_FILE.getAbsolutePath()), "main");
        this.signaldMain.start();
        while(!SOCKET_FILE.exists()) {
          TimeUnit.SECONDS.sleep(1);
        }
    }

    @AfterAll
    public void stopSignald() {
        this.signaldMain.interrupt();
    }

    @BeforeEach
    public void connectSocket() throws IOException {
        this.socket = AFUNIXSocket.newInstance();
        this.socket.connect(new AFUNIXSocketAddress(SOCKET_FILE));

        this.writer = new PrintWriter(this.socket.getOutputStream());
        this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        System.out.println(this.reader.readLine());
    }


    @AfterEach
    public void disconnectSocket() throws IOException {
        this.socket.close();
    }

    @DisplayName("Register a new account")
    @Test
    public void testRegister() {
        this.writer.println("{\"type\": \"register\", \"username\": \"tbd\"}");
    }

}
