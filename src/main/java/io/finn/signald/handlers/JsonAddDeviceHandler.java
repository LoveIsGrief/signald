package io.finn.signald.handlers;

import io.finn.signald.*;
import org.whispersystems.libsignal.InvalidKeyException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class JsonAddDeviceHandler extends BaseJsonHandler {
  @Override
  public JsonMessageWrapper handle(JsonRequest request) throws IOException {
    Manager m = ManagerFactory.getManager(request.username);
    try {
      m.addDeviceLink(new URI(request.uri));
    } catch (InvalidKeyException |
            URISyntaxException e) {
      return wrapException(e, "add_device_error", request);
    }
    return new JsonMessageWrapper("device_added",
        new JsonStatusMessage(4, "Successfully linked device"),
        request.id
    );
  }
}
