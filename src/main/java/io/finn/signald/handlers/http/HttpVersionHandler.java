package io.finn.signald.handlers.http;

import com.sun.net.httpserver.HttpExchange;
import io.finn.signald.JsonRequest;
import io.finn.signald.handlers.JsonVersionHandler;

public class HttpVersionHandler extends BaseHttpHandler {
  public HttpVersionHandler() {
    super(new JsonVersionHandler());
  }

  @Override
  protected JsonRequest convertExchange(HttpExchange httpExchange) {
    return new JsonRequest();
  }
}
