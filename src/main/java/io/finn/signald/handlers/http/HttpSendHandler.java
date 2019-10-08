package io.finn.signald.handlers.http;

import com.sun.net.httpserver.HttpExchange;
import io.finn.signald.JsonRequest;
import io.finn.signald.handlers.BaseJsonHandler;

public class HttpSendHandler extends BaseHttpHandler {
    public HttpSendHandler(BaseJsonHandler jsonHandler) {
        super(jsonHandler);
    }

    @Override
    protected JsonRequest convertExchange(HttpExchange httpExchange) {
        return null;
    }
}
