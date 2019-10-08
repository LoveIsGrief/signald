package io.finn.signald.handlers.http;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.finn.signald.JsonMessageWrapper;
import io.finn.signald.JsonRequest;
import io.finn.signald.JsonStatusMessage;
import io.finn.signald.handlers.BaseJsonHandler;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

enum HttpMethods {
  GET("GET"), POST("POST"), PUT("PUT"), DELETE("DELETE");

  private final String name;

  HttpMethods(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return this.name;
  }
}

@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface AcceptedMethods {
  HttpMethods[] value();
}

// TODO switch to https://sparkjava.com
@AcceptedMethods({HttpMethods.GET})
public abstract class BaseHttpHandler implements HttpHandler {

  private BaseJsonHandler jsonHandler;
  private ObjectMapper mpr = new ObjectMapper();

  BaseHttpHandler(BaseJsonHandler jsonHandler) {
    this.jsonHandler = jsonHandler;
    this.mpr.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY); // disable autodetect
    this.mpr.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    this.mpr.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    this.mpr.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
  }

  protected abstract JsonRequest convertExchange(HttpExchange httpExchange);

  public void handle(HttpExchange httpExchange) throws IOException {
    JsonRequest jsonRequest = convertExchange(httpExchange);
    try {
      checkAnnotations(httpExchange);
      writeResponseJson(this.jsonHandler.handle(jsonRequest), httpExchange);
    } catch (UnacceptedHttpMethod e) {
      writeResponseJson(
          new JsonMessageWrapper("http_method_error",
              new JsonStatusMessage(0, e.getMessage(), jsonRequest)
          ),
          httpExchange, 405
      );
    }
  }

  private void checkAnnotations(HttpExchange httpExchange) throws UnacceptedHttpMethod {
    final Annotation[] annotations = this.getClass().getAnnotations();
    for (Annotation annotation : annotations) {

      // Only accept annotated HTTP methods
      if (annotation instanceof AcceptedMethods) {
        final String requestMethod = httpExchange.getRequestMethod();
        final List<String> acceptedMethods = Arrays
            .stream(((AcceptedMethods) annotation).value())
            .map(HttpMethods::toString)
            .collect(Collectors.toList());
        boolean isAcceptedMethod = acceptedMethods.contains(requestMethod);
        if (!isAcceptedMethod) {
          throw new UnacceptedHttpMethod(requestMethod, acceptedMethods);
        }
      }
    }
  }

  protected void writeResponseJson(JsonMessageWrapper message, HttpExchange httpExchange) throws IOException {
    writeResponseJson(message, httpExchange, 200);
  }

  protected void writeResponseJson(JsonMessageWrapper message, HttpExchange httpExchange, int returnCode) throws IOException {
    String jsonMessage = this.mpr.writeValueAsString(message);
    httpExchange.getResponseHeaders().add("Accepted", "application/json");
    httpExchange.sendResponseHeaders(returnCode, 0);
    PrintWriter out = new PrintWriter(httpExchange.getResponseBody(), true);
    out.println(jsonMessage);
    out.close();
  }
}
