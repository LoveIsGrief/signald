package io.finn.signald.handlers.http;

import java.util.List;

public class UnacceptedHttpMethod extends Exception {
  private String method;
  private List<String> acceptedMethods;

  UnacceptedHttpMethod(String method, List<String> acceptedMethods) {
    this.method = method;
    this.acceptedMethods = acceptedMethods;
  }

  @Override
  public String getMessage() {
    return String.format("%1s is not of the accepted methods %2s",
        method, acceptedMethods
    );
  }
}
