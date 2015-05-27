# com.endpoint.lg.panoviewer
Liquid Galaxy Panorama Viewer

An Interactive Spaces activity for displaying panoramas.

## Configuration

* **space.activity.panoviewer.slave** Set this to exactly "false" for a master pano viewer, of which there should probably only be one in a space. The other activities should be set to anything else, and they'll be slaves.
* **space.activity.panoviewer.yaw, space.activity.panoviewer.pitch, space.activity.panoviewer.roll** The yaw, pitch, and roll offset angles, in degrees, this particular instance should use
* **space.activity.panoviewer.vertFov** The vertical field of view this display should show. Changing this will probably also require changing the yaw angle. Default is 75, which is most likely far bigger than you want for any large display.
* **lg.window.viewport.target** The viewport name this activity should use
* **space.activity.webapp.web.server.port** The port the activity's internal web server should use. This should be unique for each instance of this activity. Yes, that's kinda a pain, if you have multiple instances of the live activity on a single controller, which you'll have to do if that controller runs multiple screens.

## Input routes

These routes are configured to sensible defaults in activity.conf, and might not need to be touched

* **EV_ABS** In order to use the space navigator, this activity must receive messages from the evdev demuxer activity, on this route
* **pano_viewsync** The route on which the slaves should expect viewsync input

## Output routes

This route is configured to a sensible default in activity.conf, and might not need to be touched

* **pano_viewsync** The route on which the master should send viewsync messages to the slaves

## Specifying the image to load

The viewsync messages' format is determined by the ViewSyncEffect library, but includes a section of "extra" data. The pano viewer expects this section to include the URL of the current image it is displaying. You can change it by sending your own viewsync message with only this extra information.

I accomplish this as follows. One day the touchscreen will hopefully do this automatically, but I haven't coded that part yet. So you've got to create the messages more manually. First, create a file of messages, formatted like this:

```
---
type: json
message: '{"extra":{"type":"pano","fileurl":"http://server/image1.jpg"},"src":"test"}'
---
type: json
message: '{"extra":{"type":"pano","fileurl":"http://server/image2.jpg"},"src":"test"}'
```

... and so on. Then, on your node, make sure your ROS environment variables are set such that it understands the interactive spaces message types

```
bash$ . /opt/ros/indigo/setup.bash
bash$ . /path/to/ros_cms/director/devel/setup.bash
bash$ rosmsg list | grep interactive
interactivespaces_msgs/GenericMessage
```

Then you can send messages to the pano viewer something like this:

```
bash$ rostopic pub -f name_of_your_file -r 0.5 /liquidgalaxy/generic/panoviewer/viewsync interactivespaces_msgs/GenericMessage
```

There's also a control panel embedded in each live activity, which might be easier to use. It's available at http://controller-hostname:live-activity-web-server-port/index.html. It contains a simple form where you'll enter the URL of an image you want used. Note that this doesn't currently support loading panoramic videos, but only because I have yet to add a control where you can specify whether the URL points to an image or a video (or logic to figure it out on its own).
