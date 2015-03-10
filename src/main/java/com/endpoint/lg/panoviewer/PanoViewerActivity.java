/*
 * Copyright (C) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.endpoint.lg.panoviewer;

import interactivespaces.activity.impl.web.BaseRoutableRosWebServerActivity;
import interactivespaces.service.web.server.WebServer;
import interactivespaces.util.data.json.JsonBuilder;
import interactivespaces.util.data.json.JsonNavigator;

import com.endpoint.lg.support.message.MessageWrapper;
import com.endpoint.lg.support.web.WebConfigHandler;
import com.endpoint.lg.support.message.RosMessageHandler;
import com.endpoint.lg.support.message.WebsocketMessageHandler;
import com.endpoint.lg.support.message.RosMessageHandlers;
import com.endpoint.lg.support.message.WebsocketMessageHandlers;
import com.endpoint.lg.support.message.panoviewer.MessageTypesPanoviewer;

import java.util.Map;

/**
 * A panoramic image viewer activity, copied from Matt's streetview pano
 * activity
 * 
 * @author Josh Tolley <josh@endpoint.com>
 */
public class PanoViewerActivity extends BaseRoutableRosWebServerActivity {

  /**
   * The dynamic configuration handler will catch requests for this file.
   */
  public static final String CONFIG_HANDLER_PATH = "is.config.js";

  private WebsocketMessageHandlers wsHandlers;
  private RosMessageHandlers rosHandlers;
  private Object lastMsg;

  /**
   * Sends initialization packet when a new connection arrives
   */
  @Override
  public void onNewWebSocketConnection(String connectionId) {
    if (lastMsg != null) {
      sendWebSocketJson(connectionId, lastMsg);
    }
  }

  /**
   * Sends incoming web socket messages to the web socket message handlers.
   */
  @Override
  public void onWebSocketReceive(String connectionId, Object d) {
    lastMsg = d;
    wsHandlers.handleMessage(connectionId, d);
  }

  /**
   * Sends incoming Ros messages to the Ros message handlers.
   */
  @Override
  public void onNewInputJson(String channel, Map<String, Object> message) {
    rosHandlers.handleMessage(channel, message);
  }

  /**
   * Registers a handler for forwarding messages from websockets to Ros.
   * 
   * @param handlers
   *          the websocket handler registry
   * @param type
   *          the message type/channel
   */
  private void relayWebsocketToRos(WebsocketMessageHandlers handlers, final String type) {
    handlers.registerHandler(type, new WebsocketMessageHandler() {
      public void handleMessage(String connectionId, JsonNavigator json) {
        sendOutputJsonBuilder(type, json.getCurrentAsJsonBuilder());
      }
    });
  }

  /**
   * Registers a handler for forwarding messages from Ros to websockets.
   * 
   * @param handlers
   *          the Ros handler registry
   * @param type
   *          the message type/channel
   */
  private void relayRosToWebsocket(RosMessageHandlers handlers, final String type) {
    handlers.registerHandler(type, new RosMessageHandler() {
      public void handleMessage(JsonNavigator json) {
        JsonBuilder message = MessageWrapper.newTypedMessage(type, json.getRoot());

        sendAllWebSocketJsonBuilder(message);
      }
    });
  }

  /**
   * Registers message relays and sets up window management.
   */
  @Override
  public void onActivitySetup() {
    wsHandlers = new WebsocketMessageHandlers(getLog());

    relayWebsocketToRos(wsHandlers, MessageTypesPanoviewer.MESSAGE_TYPE_VIEWSYNC);

    rosHandlers = new RosMessageHandlers(getLog());

    relayRosToWebsocket(rosHandlers, MessageTypesPanoviewer.MESSAGE_TYPE_VIEWSYNC);
  }

  /**
   * Starts the web configuration handler.
   */
  @Override
  public void onActivityStartup() {
    lastMsg = null;
    WebServer webserver = getWebServer();
    WebConfigHandler configHandler = new WebConfigHandler(getConfiguration());
    webserver.addDynamicContentHandler(CONFIG_HANDLER_PATH, false, configHandler);
  }
}
