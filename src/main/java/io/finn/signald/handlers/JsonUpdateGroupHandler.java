package io.finn.signald.handlers;

import io.finn.signald.*;
import org.asamk.signal.AttachmentInvalidException;
import org.asamk.signal.GroupNotFoundException;
import org.asamk.signal.NotAGroupMemberException;
import org.whispersystems.signalservice.api.crypto.UntrustedIdentityException;
import org.whispersystems.signalservice.api.push.exceptions.EncapsulatedExceptions;
import org.whispersystems.signalservice.internal.util.Base64;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JsonUpdateGroupHandler extends BaseJsonHandler {
  @Override
  public JsonMessageWrapper handle(JsonRequest request) throws IOException {
    Manager m = ManagerFactory.getManager(request.username);

    byte[] groupId = null;
    if (request.recipientGroupId != null) {
      groupId = Base64.decode(request.recipientGroupId);
    }
    if (groupId == null) {
      groupId = new byte[0];
    }

    String groupName = request.groupName;
    if (groupName == null) {
      groupName = "";
    }

    List<String> groupMembers = request.members;
    if (groupMembers == null) {
      groupMembers = new ArrayList<String>();
    }

    String groupAvatar = request.avatar;
    if (groupAvatar == null) {
      groupAvatar = "";
    }

    byte[] newGroupId = new byte[0];
    try {
      newGroupId = m.updateGroup(groupId, groupName, groupMembers, groupAvatar);
    } catch (EncapsulatedExceptions |
            GroupNotFoundException |
            UntrustedIdentityException |
            NotAGroupMemberException |
            AttachmentInvalidException e) {
      return wrapException(e, "group_update_error", request);
    }

    if (groupId.length != newGroupId.length) {
      return new JsonMessageWrapper("group_created", new JsonStatusMessage(5, "Created new group " + groupName + "."), request.id);
    } else {
      return new JsonMessageWrapper("group_updated", new JsonStatusMessage(6, "Updated group"), request.id);
    }
  }
}
