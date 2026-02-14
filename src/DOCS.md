# Documentation

## Configuration

### ntfy

`ntfy_enabled`: When toggled on, the addon will send notifications to ntfy. Will work in conjunction with Home Assistant notifications if they are enabled.

`ntfy_url`: The URL to your ntfy server.  For example, "http://ntfy.sh" or "http://127.0.0.1:80". HTTPS works as well if you have that configured.

`ntfy_user`: Optional. To authenticate with ntfy, you can either use a user name and password, or a token.

`ntfy_password`: Optional. Only used if both user and password are set.

`ntfy_token`: Optional. Only used if user and password are not set.

`ntfy_topic`: The topic to send the notification to.  For example, "frigate".

`ntfy_tags`: Optional. These are used to add emojis to the notification and are configured by the object type detected by Frigate. In the example config, car is set to car and adds 🚗 when a car is detected. You can have multiple tags per object, separated by commas, for example "walking,standing_person". A full list of available tags can be found [here](https://docs.ntfy.sh/emojis/).

`grouping_enabled`: If set to true, subsequent notifications during the 'grouping_minutes' time frame will be set to a lower priority. See notification priority documentation [here](https://docs.ntfy.sh/publish/#message-priority).

`grouping_minutes`: The number of minutes to group notifications for. For example, if set to 5, notifications will be grouped together if they are within 5 minutes of each other.

`ntfy_normal_priority`: The default priority of notifications.

`ntfy_lower_priority`: The lower priority used when notifications are "grouped" together.

### Home Assistant companion app

`ha_enabled`: When toggled on, the addon will send notifications to the Home Assistant companion app. Will work in conjunction with ntfy notifications if they are enabled.

`ha_entity_ids`: The entity IDs of the devices you want to send notifications to. For example, "mobile_app_pixel_8". A notification will be sent to each device in the list.

### Frigate

`frigate_url`: The URL to your Frigate instance. For example, "http://frigate.com" or "http://127.0.0.1:5000". HTTPS works as well if you have that configured.

`disabled_cameras`: Optional. A list of cameras to ignore or filter. Each entry requires a `camera_name`. You can optionally provide a list of `disabled_objects` (e.g., "car", "person") to disable only specific objects for that camera. If the labels list is empty, all objects for that camera will be ignored.

`disabled_objects`: Optional. A list of objects to ignore or filter completely (e.g., "car", "person").

`snapshot_options`: Optional. The options sent to the Frigate API when requesting a snapshot. Documentation can be found [here](https://docs.frigate.video/integrations/api#get-apieventsidsnapshotjpg).

### MQTT

`mqtt_address`: The address of your MQTT broker. For example, "tcp://127.0.0.1".

`mqtt_port`: The port of your MQTT broker. For example, "1883".

`mqtt_topic`: The topic to subscribe to for Frigate events. Frigate's default is "frigate/events".

`mqtt_username`: Optional. The username to connect to the MQTT broker with.

`mqtt_password`: Optional. The password to connect to the MQTT broker with.

## Running the addon

Once you have configured the addon, click "Start" to start the addon. You can view the logs by clicking on the "Logs" tab. The logs should show any errors the addon encountered.

## Potential issues

All of these services need to be accessible from the Home Assistant instance running the addon. All of these services will need to be accessible wherever you are viewing these notifications. For instance, if you're viewing them on your phone, your phone needs to be able to access your ntfy instance and your Frigate instance. If those are not accessible from outside your network, they will only work when you are on your home network.
