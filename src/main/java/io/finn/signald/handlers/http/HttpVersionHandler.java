package io.finn.signald.handlers.http;

import io.finn.signald.JsonRequest;
import io.finn.signald.handlers.JsonVersionHandler;
import spark.Request;

public class HttpVersionHandler extends BaseHttpHandler {
  public HttpVersionHandler() {
    super(new JsonVersionHandler());
  }

  @Override
  protected JsonRequest convertRequest(Request request) {
    return new JsonRequest();
  }
}
