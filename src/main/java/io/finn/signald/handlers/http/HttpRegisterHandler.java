package io.finn.signald.handlers.http;

import io.finn.signald.JsonRequest;
import io.finn.signald.handlers.JsonRegisterHandler;
import spark.Request;

@AcceptedMethods({HttpMethods.POST})
public class HttpRegisterHandler extends BaseHttpHandler {
    public HttpRegisterHandler() {
        super(new JsonRegisterHandler());
    }

    @Override
    protected JsonRequest convertRequest(Request request) {
        JsonRequest jsonRequest = new JsonRequest();
        jsonRequest.type = "register";
        jsonRequest.username = request.queryMap("username").value().trim();
        jsonRequest.voice = request.queryMap("voice").booleanValue();
        return jsonRequest;
    }
}
