# com.endpoint.lg.panoviewer
Liquid Galaxy Panorama Viewer

An Interactive Spaces activity for displaying panoramas

## Configuration

* **space.activity.panoviewer.slave** Set this to exactly "false" for a master pano viewer, of which there should probably only be one in a space. The other activities should be set to anything else, and they'll be slaves.
* **space.activity.panoviewer.yaw** The yaw offset angle, in degrees, this particular slave should use
* **lg.window.viewport.target** The viewport name this activity should use

## Input routes

These routes are configured to sensible defaults in activity.conf, and might not need to be touched

* **EV_ABS** In order to use the space navigator, this activity must receive messages from the evdev demuxer activity, on this route
* **pano_viewsync** The route on which the slaves should expect viewsync input

## Output routes

This route is configured to a sensible default in activity.conf, and might not need to be touched

* **pano_viewsync** The route on which the master should send viewsync messages to the slaves

## Specifying the image to load

The viewsync messages' format is determined by the ViewSyncEffect library, but includes a section of "extra" data. The pano viewer expects this section to include the URL of the current image it is displaying. You can change it by sending your own viewsync message with only this extra information.

I accomplish this as follows. One day the touchscreen will hopefully do this automatically, but right now it's kinda hard. First, create a file of messages, formatted like this:

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
