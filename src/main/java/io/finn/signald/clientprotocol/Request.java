/*
 * Copyright (C) 2020 Finn Herzfeld
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.finn.signald.clientprotocol;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.finn.signald.JsonMessageWrapper;
import io.finn.signald.annotations.OneOfRequired;
import io.finn.signald.annotations.Required;
import io.finn.signald.annotations.RequiredNonEmpty;
import io.finn.signald.annotations.SignaldClientRequest;
import io.finn.signald.clientprotocol.v1.JsonSendMessageResult;
import io.finn.signald.clientprotocol.v1.VersionRequest;
import io.finn.signald.clientprotocol.v1alpha1.*;
import io.finn.signald.util.JSONUtil;
import io.finn.signald.util.RequestUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.whispersystems.signalservice.api.messages.SendMessageResult;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.Socket;
import java.util.*;

import static io.finn.signald.util.RequestUtil.getVersion;
import static io.finn.signald.util.RequestUtil.requestTypes;

public class Request {
  public static final Map<String, Map<String, Class<? extends RequestType>>> requests = getRequests();
  public static final Map<String, String> defaultVersions = getDefaultVersions();

  private RequestType requestType;
  private String type;
  private String version;
  private String id;
  private Socket socket;
  private PrintWriter writer;
  private Logger logger;

  private final ObjectMapper mapper = JSONUtil.GetMapper();

  public static Map<String, Map<String, Class<? extends RequestType>>> getRequests() {
    Map<String, Map<String, Class<? extends RequestType>>> requests = new HashMap<>();
    for (Class<? extends RequestType> t : requestTypes) {
      SignaldClientRequest annotation = t.getDeclaredAnnotation(SignaldClientRequest.class);
      if (!requests.containsKey(annotation.type())) {
        requests.put(annotation.type(), new HashMap<>());
      }
      Map<String, Class<? extends RequestType>> versions = requests.get(annotation.type());
      versions.put(RequestUtil.getVersion(t), t);
      requests.put(annotation.type(), versions);
      requests.get(annotation.type()).put(getVersion(t), t);
    }
    return requests;
  }

  public static Map<String, String> getDefaultVersions() {
    Map<String, String> v = new HashMap<>();
    v.put(VersionRequest.class.getAnnotation(SignaldClientRequest.class).type(), "v1");
    v.put(ProtocolRequest.class.getAnnotation(SignaldClientRequest.class).type(), "v1alpha1");
    v.put(GetLinkedDevicesRequest.class.getAnnotation(SignaldClientRequest.class).type(), "v1alpha1");
    v.put(RemoveLinkedDeviceRequest.class.getAnnotation(SignaldClientRequest.class).type(), "v1alpha1");
    v.put(AcceptInvitationRequest.class.getAnnotation(SignaldClientRequest.class).type(), "v1alpha1");
    v.put(ApproveMembershipRequest.class.getAnnotation(SignaldClientRequest.class).type(), "v1alpha1");
    v.put(GetGroupRequest.class.getAnnotation(SignaldClientRequest.class).type(), "v1alpha1");
    v.put(JoinGroupRequest.class.getAnnotation(SignaldClientRequest.class).type(), "v1alpha1");
    return v;
  }

  public Request(JsonNode request, Socket s) throws IOException {
    socket = s;
    initializeMapper();
    writer = new PrintWriter(socket.getOutputStream(), true);

    if (request.has("id")) {
      id = request.get("id").asText();
    }

    List<String> problems = new ArrayList<>();
    if (!request.has("type")) {
      problems.add("missing required argument: type");
    }

    type = request.get("type").asText();

    if (request.has("version")) {
      version = request.get("version").asText();
    } else if (defaultVersions.containsKey(type)) {
      version = defaultVersions.get(type);
    } else {
      problems.add("missing required argument: version");
    }

    if (problems.size() > 0) {
      error(new RequestValidationFailure(problems));
      return;
    }

    logger = LogManager.getLogger(type);

    if (!requests.containsKey(type)) {
      error(new RequestValidationFailure("Unknown request type: " + type));
      return;
    }

    if (!requests.get(type).containsKey(version)) {
      error(new RequestValidationFailure("unknown version of that request type"));
      return;
    }

    requestType = mapper.convertValue(request, requests.get(type).get(version));
    List<String> validationFailures = validate(request);
    if (validationFailures.size() > 0) {
      logger.warn("invalid request");
      error(new RequestValidationFailure(validationFailures));
      return;
    }

    try {
      requestType.run(this);
    } catch (Throwable throwable) {
      error(new RequestProcessingError(throwable));
      logger.error("error while handling request", throwable);
    }
  }

  private List<String> validate(JsonNode request) {
    List<String> errors = new ArrayList<>();

    for (Field f : requestType.getClass().getFields()) {
      // Field does not exist in request
      if (!request.has(f.getName())) {
        if (f.getAnnotation(Required.class) != null || f.getAnnotation(RequiredNonEmpty.class) != null) {
          errors.add("missing required argument: " + f.getName());
        }

        if (f.getAnnotation(OneOfRequired.class) != null) {
          OneOfRequired requirement = f.getAnnotation(OneOfRequired.class);
          int found = 0;
          for (String option : requirement.value()) {
            if (request.has(option)) {
              found++;
            }
          }
          if (found != 1) {
            errors.add("exactly one required of: " + f.getName() + " or " + String.join(" or ", requirement.value()));
          }
        }
      } else { // argument is present
        if (f.getAnnotation(RequiredNonEmpty.class) != null) {
          JsonNode field = request.get(f.getName());
          if (field.isArray()) {
            if (field.size() == 0) {
              errors.add(f.getName() + " must have at least 1 entry");
            }
          }
        }
      }
    }

    return errors;
  }

  public void error(Object data) throws JsonProcessingException { reply(JsonMessageWrapper.error(type, data, id)); }

  public void reply(Object data) throws JsonProcessingException { reply(new JsonMessageWrapper(type, data, id)); }

  private void reply(JsonMessageWrapper message) throws JsonProcessingException { writer.println(mapper.writeValueAsString(message)); }

  public void replyWithSendResults(List<SendMessageResult> sendMessageResults) throws JsonProcessingException {
    List<JsonSendMessageResult> results = new ArrayList<>();
    for (SendMessageResult r : sendMessageResults) {
      if (r != null) {
        results.add(new JsonSendMessageResult(r));
      }
    }
    reply(results);
  }

  public void replyWithSendResult(SendMessageResult sendMessageResults) throws JsonProcessingException { replyWithSendResults(Collections.singletonList(sendMessageResults)); }

  public Socket getSocket() { return socket; }

  private void initializeMapper() {
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.setSerializationInclusion(Include.NON_NULL);
    mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
    mapper.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
  }
}
