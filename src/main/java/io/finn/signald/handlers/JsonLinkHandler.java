package io.finn.signald.handlers;

import io.finn.signald.*;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeoutException;

public class JsonLinkHandler extends BaseJsonHandler {
  @Override
  public JsonMessageWrapper handle(JsonRequest request) throws IOException {
    Manager m = ManagerFactory.getManager();
    m.createNewIdentity();
    String deviceName = "signald"; // TODO: Set this to "signald on <hostname>"
    if (request.deviceName != null) {
      deviceName = request.deviceName;
    }
    try {
      URI deviceLinkUri = m.getDeviceLinkUri();
      // Async operation
      m.finishDeviceLink(deviceName);
      return new JsonMessageWrapper("linking_uri", new JsonLinkingURI(deviceLinkUri), request.id);
    } catch (TimeoutException e) {
      return new JsonMessageWrapper("linking_error", new JsonStatusMessage(1, "Timed out while waiting for device to link", request), request.id);
    } catch (IOException e) {
      return new JsonMessageWrapper("linking_error", new JsonStatusMessage(2, e.getMessage(), request), request.id);
    }

  }
}
