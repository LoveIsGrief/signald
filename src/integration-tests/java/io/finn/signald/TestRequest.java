package io.finn.signald;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Assertions;

import org.newsclub.net.unix.AFUNIXSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import java.io.File;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import okhttp3.OkHttpClient;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.Request;
import okhttp3.Response;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestRequest {

    private Thread signaldMain;
    private static File SOCKET_FILE = new File(System.getProperty("java.io.tmpdir"), "signald.sock");
    private AFUNIXSocket socket;
    private PrintWriter writer;
    private BufferedReader reader;
    static final Logger logger = LoggerFactory.getLogger(TestRequest.class);
    private ObjectMapper mpr = new ObjectMapper();
    OkHttpClient client = new OkHttpClient();

    public String generateUsername() {
        return String.format("+1202555%04d", ThreadLocalRandom.current().nextInt(0, 10000));
    }

    @BeforeAll
    public void startSignald() throws InterruptedException {
        this.signaldMain = new Thread(new RunnableMain(SOCKET_FILE.getAbsolutePath()), "main");
        this.signaldMain.start();
        while(!SOCKET_FILE.exists()) {
            logger.info("Waiting for " + SOCKET_FILE.getAbsolutePath() + " to exist...");
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

        this.writer = new PrintWriter(this.socket.getOutputStream(), true);
        this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
        System.out.println(this.reader.readLine());
    }


    @AfterEach
    public void disconnectSocket() throws IOException {
        this.socket.close();
    }

    @DisplayName("Register a new account")
    @Test
    public void testRegister() throws IOException {
        String username = generateUsername();
        this.writer.println("{\"type\": \"register\", \"username\": \"" + username + "\"}");
        JsonNode root = mpr.readTree(this.reader.readLine());
        Assertions.assertEquals(root.findValue("type").textValue(), "verification_required");

        String code = getVerificationCode(username);

        System.out.println("Got verification code " + code);

        this.writer.println("{\"type\": \"verify\", \"username\": \"" + username + "\", \"code\": \"" + code + "\"}");

        root = (JsonNode)mpr.readTree(this.reader.readLine());
        System.out.println(root);
        Assertions.assertEquals(root.findValue("type").textValue(), "verification_succeeded");

    }

    private String getVerificationCode(String username) throws IOException {
        OkHttpClient client = new OkHttpClient();

        RequestBody body = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart("number", username).build();

        Request request = new Request.Builder().url(BuildConfig.SIGNAL_URL + "/helper/verification-code").post(body).build();

        Response response = client.newCall(request).execute();

        return response.body().string();
    }
}
