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

import com.endpoint.lg.support.window.WindowInstanceIdentity;
import com.endpoint.lg.support.window.ManagedWindow;
import com.endpoint.lg.support.window.WindowIdentity;

import interactivespaces.activity.impl.ros.BaseRoutableRosActivity;
import interactivespaces.util.data.json.JsonBuilder;
import interactivespaces.util.data.json.JsonNavigator;
import interactivespaces.util.data.json.StandardJsonBuilder;
import interactivespaces.util.data.json.StandardJsonMapper;
import interactivespaces.util.data.json.StandardJsonNavigator;

import com.endpoint.lg.support.evdev.InputAbsState;
import com.endpoint.lg.support.evdev.InputEventCodes;
import com.endpoint.lg.support.message.MessageWrapper;
import com.endpoint.lg.support.message.panoviewer.MessageTypesPanoviewer;
import com.endpoint.lg.support.message.RosMessageHandler;
import com.endpoint.lg.support.message.RosMessageHandlers;

import com.endpoint.lg.support.message.Scene;
import com.endpoint.lg.support.message.Window;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

/**
 * A panoramic image viewer activity, copied from Matt's streetview pano
 * activity
 * XXX One day the stuff copied from Streetview probably ought to be put in a
 * library somewhere
 *
 * @author Josh Tolley <josh@endpoint.com>
 */
public class PanoViewerMasterActivity extends BaseRoutableRosActivity {

  /**
   * The dynamic configuration handler will catch requests for this file.
   */
  public static final String CONFIG_HANDLER_PATH = "is.config.js";

  /**
   * The dynamic configuration handler will catch requests for this file.
   */
  public static final String PROXY_HANDLER_PATH = "proxy";

  /**
   * Coefficient of input event value to POV translation.
   */
  public static final double INPUT_SENSITIVITY = 0.0032;

  /**
   * How much "momentum" on a controller is needed to move forward or backward.
   */
  public static final int INPUT_MOVEMENT_COUNT = 10;

  /**
   * Controller forward/backward axes must exceed this value for movement (after
   * sensitivity).
   */
  public static final double INPUT_MOVEMENT_THRESHOLD = 1.0;

  /**
   * After axial movement, wait this many milliseconds before moving again.
   */
  public static final int INPUT_MOVEMENT_COOLDOWN = 250;

  private RosMessageHandlers rosHandlers;
  private Object lastMsg;
  private double lastSpnavMsg;

  /**
   * Sends incoming Ros messages to the Ros message handlers.
   */
  @Override
  public void onNewInputJson(String channel, Map<String, Object> message) {
    rosHandlers.handleMessage(channel, message);
  }

  /**
   * Handle an EV_ABS state update.
   *
   * @param state
   *          the axis state
   */
  private void onRosAbsStateChange(InputAbsState state) {
    Map<String, Object> data = new HashMap<String, Object>();
    data.put("roll", new Double(state.getValue(InputEventCodes.ABS_RX) * INPUT_SENSITIVITY));
    data.put("tilt", new Double(state.getValue(InputEventCodes.ABS_RY) * INPUT_SENSITIVITY));
    data.put("yaw", new Double(state.getValue(InputEventCodes.ABS_RZ) * INPUT_SENSITIVITY));

    JsonBuilder message = MessageWrapper.newTypedMessage("navigation", data);
    sendOutputJsonBuilder("pano_viewsync", message);
  }

  /**
   * Registers message relays and sets up window management.
   */
  @Override
  public void onActivitySetup() {
    rosHandlers = new RosMessageHandlers(getLog());

    // handle absolute axis state changes, if activated
    rosHandlers.registerHandler("EV_ABS", new RosMessageHandler() {
      public void handleMessage(JsonNavigator json) {
        if (isActivated())
          onRosAbsStateChange(new InputAbsState(json));
      }
    });

    rosHandlers.registerHandler("director", new RosMessageHandler() {
      public void handleMessage(JsonNavigator json) {
        Scene scene;
        String jsonStr = StandardJsonMapper.INSTANCE.toString(json.getRoot());

        try {
          scene = Scene.fromJson(jsonStr);
        }
        catch (IOException e) {
          getLog().error("Error parsing scene message");
          getLog().error(e.getMessage());
          return;
        }

        for (Window w : scene.windows) {
          if (w.activity.equals("pano")) {
            getLog().info("Found a pano scene");

            JsonBuilder data = new StandardJsonBuilder();
            data.newObject("extra");
            data.put("fileurl", w.assets[0]);
            data.put("type", "pano");
            data.put("filetype", "image");
            data.up();
            JsonBuilder message = MessageWrapper.newTypedMessage(MessageTypesPanoviewer.MESSAGE_TYPE_VIEWSYNC, data.build());
            getLog().info("Switching pano viewer image to " + w.assets[0]);
            getLog().info(message);
            sendOutputJsonBuilder("pano_viewsync", message);
          }
        }
      }
    });
  }

  /**
   * Starts the web configuration handler.
   */
  @Override
  public void onActivityStartup() {
    lastMsg = null;
    lastSpnavMsg = 0;
  }
}
