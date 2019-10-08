package io.finn.signald.handlers.http;

import io.finn.signald.JsonRequest;
import io.finn.signald.handlers.BaseJsonHandler;
import spark.Request;

public class HttpSendHandler extends BaseHttpHandler {
    public HttpSendHandler(BaseJsonHandler jsonHandler) {
        super(jsonHandler);
    }

    @Override
    protected JsonRequest convertRequest(Request request) {
        return null;
    }
}
