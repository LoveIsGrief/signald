package io.finn.signald;

import com.sun.net.httpserver.HttpServer;
import io.finn.signald.handlers.http.HttpRegisterHandler;
import io.finn.signald.handlers.http.HttpVersionHandler;

import java.io.IOException;
import java.net.InetSocketAddress;

public class HttpServerFactory {

  private static final int USE_DEFAULT_SYSTEM_VALUE = 0;

  static HttpServer create(InetSocketAddress addr) throws IOException {
    HttpServer server = HttpServer.create(addr, USE_DEFAULT_SYSTEM_VALUE);
    server.createContext("/register", new HttpRegisterHandler());
    server.createContext("/send", new HttpRegisterHandler());
    server.createContext("/version", new HttpVersionHandler());
    return server;
  }
}
