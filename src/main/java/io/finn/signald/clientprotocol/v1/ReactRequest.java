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

package io.finn.signald.clientprotocol.v1;

import io.finn.signald.Manager;
import io.finn.signald.NoSuchAccountException;
import io.finn.signald.annotations.Doc;
import io.finn.signald.annotations.OneOfRequired;
import io.finn.signald.annotations.Required;
import io.finn.signald.annotations.SignaldClientRequest;
import io.finn.signald.clientprotocol.Request;
import io.finn.signald.clientprotocol.RequestType;
import io.finn.signald.exceptions.InvalidRecipientException;
import org.asamk.signal.GroupNotFoundException;
import org.asamk.signal.NotAGroupMemberException;
import org.signal.zkgroup.InvalidInputException;
import org.whispersystems.signalservice.api.messages.SendMessageResult;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;

import java.io.IOException;
import java.util.List;

@SignaldClientRequest(type = "react", ResponseClass = SendResponse.class)
@Doc("react to a previous message")
public class ReactRequest implements RequestType {

  @Required public String username;
  @OneOfRequired({"recipientGroupId"}) public JsonAddress recipientAddress;
  @OneOfRequired({"recipientAddress"}) public String recipientGroupId;
  @Required public JsonReaction reaction;
  public long timestamp;

  @Override
  public void run(Request request) throws IOException, GroupNotFoundException, NotAGroupMemberException, InvalidRecipientException, NoSuchAccountException, InvalidInputException {
    Manager manager = Manager.get(username);

    if (timestamp > 0) {
      timestamp = System.currentTimeMillis();
    }

    SignalServiceDataMessage.Builder messageBuilder = SignalServiceDataMessage.newBuilder();
    messageBuilder.withReaction(reaction.getReaction());
    List<SendMessageResult> results = manager.send(messageBuilder, recipientAddress, recipientGroupId);
    request.reply(new SendResponse(results, timestamp));
  }
}
