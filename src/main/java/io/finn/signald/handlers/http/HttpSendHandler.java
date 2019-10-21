package io.finn.signald.handlers.http;

import io.finn.signald.JsonQuote;
import io.finn.signald.JsonRequest;
import io.finn.signald.handlers.BaseJsonHandler;
import spark.Request;

public class HttpSendHandler extends BaseHttpHandler {
    public HttpSendHandler(BaseJsonHandler jsonHandler) {
        super(jsonHandler);
    }

    @Override
    protected JsonRequest convertRequest(Request request) {
        JsonRequest jsonRequest = new JsonRequest();
        if(request.queryMap("quote").hasKeys()){
            JsonQuote quote = new JsonQuote();
//            quote.id =
//            jsonRequest.quote = quote;
        }
        jsonRequest.username = request.queryMap("username").value().trim();
        jsonRequest.recipientNumber =  request.queryMap("recipientNumber").value().trim();

        return jsonRequest;
    }
}
