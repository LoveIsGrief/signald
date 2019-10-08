package io.finn.signald.handlers.http;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.finn.signald.JsonMessageWrapper;
import io.finn.signald.JsonRequest;
import io.finn.signald.JsonStatusMessage;
import io.finn.signald.handlers.BaseJsonHandler;
import spark.Request;
import spark.Response;
import spark.Route;

import java.io.IOException;
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
public abstract class BaseHttpHandler implements Route {

  private BaseJsonHandler jsonHandler;
  private ObjectMapper mpr = new ObjectMapper();

  BaseHttpHandler(BaseJsonHandler jsonHandler) {
    this.jsonHandler = jsonHandler;
    this.mpr.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY); // disable autodetect
    this.mpr.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    this.mpr.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    this.mpr.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
  }

  protected abstract JsonRequest convertRequest(Request request);

  @Override
  public Object handle(Request request, Response response) {
    JsonRequest jsonRequest = convertRequest(request);
    String errorType = null;
    String errorMessage = null;
    int returnCode = 200;
    JsonMessageWrapper msgWrapper = null;
    try {
      checkAnnotations(request);
      msgWrapper = this.jsonHandler.handle(jsonRequest);
    } catch (UnacceptedHttpMethod e) {
      returnCode = 405;
      errorType = "http_method_error";
      errorMessage = e.getMessage();
    } catch (IOException e) {
      returnCode = 500;
      errorType = "io_error";
      errorMessage = e.getMessage();
    }
    if (msgWrapper == null) {
      msgWrapper = new JsonMessageWrapper(
          errorType,
          new JsonStatusMessage(0, errorMessage, jsonRequest),
          jsonRequest.id
      );
    }
    try {
      writeResponseJson(msgWrapper, response, returnCode);
    } catch (IOException e) {
      return "INTERNAL SERVER ERROR: " + e.getMessage();
    }
    return response.body();
  }

  private void checkAnnotations(Request request) throws UnacceptedHttpMethod {
    final Annotation[] annotations = this.getClass().getAnnotations();
    for (Annotation annotation : annotations) {

      // Only accept annotated HTTP methods
      if (annotation instanceof AcceptedMethods) {
        final String requestMethod = request.requestMethod();
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

  protected void writeResponseJson(JsonMessageWrapper message, Response response) throws IOException {
    writeResponseJson(message, response, 200);
  }

  protected void writeResponseJson(JsonMessageWrapper message, Response response, int returnCode) throws IOException {
    String jsonMessage = this.mpr.writeValueAsString(message);
    response.type("application/json");
    response.status(returnCode);
    response.body(jsonMessage);
  }
}
