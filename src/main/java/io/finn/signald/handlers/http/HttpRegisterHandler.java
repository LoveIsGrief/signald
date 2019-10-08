package io.finn.signald.handlers.http;

import com.sun.net.httpserver.HttpExchange;
import io.finn.signald.JsonRequest;
import io.finn.signald.handlers.JsonRegisterHandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

@AcceptedMethods({HttpMethods.POST})
public class HttpRegisterHandler extends BaseHttpHandler {
    public HttpRegisterHandler() {
        super(new JsonRegisterHandler());
    }

    @Override
    protected JsonRequest convertExchange(HttpExchange httpExchange) {
        JsonRequest request = new JsonRequest();
        // TODO implement in Spark. No need to reinvent POST args parsing
        final List<String> lines = new BufferedReader(new InputStreamReader(httpExchange.getRequestBody())).lines().collect(Collectors.toList());
        request.type = "register";
        return request;
    }
}
