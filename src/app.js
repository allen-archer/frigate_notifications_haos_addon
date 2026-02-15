import fs from 'fs';
import fetch from 'node-fetch';
import mqtt from 'mqtt';

let config;
let mqttClient;
let lastNotificationDate;
let tagsMap;
let supervisorToken;
let disabledCameras;
let disabledObjects;
let debugLogging;

async function initialize() {
  const configFile = fs.readFileSync('./data/options.json', 'utf-8');
  config = JSON.parse(configFile);
  tagsMap = new Map();
  if (config.ntfy_user && config.ntfy_password) {
    config.ntfy_basicAuth = `Basic ${Buffer.from(config.ntfy_user + ':' + config.ntfy_password, 'utf8').toString('base64')}`;
  } else if (config.ntfy_token) {
    config.ntfy_token = `Bearer ${config.ntfy_token}`;
  }
  config.ntfy_tags?.forEach(tag => {
    tagsMap.set(tag.object, tag.tags);
  });
  supervisorToken = process.env.TOKEN;
  disabledCameras = new Map();
  config.disabled_cameras?.forEach(camera => {
    const objects = new Set();
    camera.disabled_objects?.forEach (object => {
      objects.add(object.toLowerCase())
    });
    disabledCameras.set(camera.camera_name.toLowerCase(), objects);
  });
  disabledObjects = new Set();
  config.disabled_objects?.forEach(disabledObject => {
    disabledObjects.add(disabledObject.toLowerCase())
  });
  debugLogging = config.debug_logging;
  if (debugLogging) {
    console.log('Disabled cameras: ' + JSON.stringify(Object.fromEntries(Array.from(disabledCameras, ([key, value]) => [key, [...value]])), null, 2));
    console.log('Disabled objects: ' + JSON.stringify(disabledObjects, null, 2));
  }
  try {
    const mqttOptions = {};
    mqttOptions.port = config.mqtt_port;
    if (config.mqtt_username && config.mqtt_username !== '') {
      mqttOptions.username = config.mqtt_username;
    }
    if (config.mqtt_password && config.mqtt_password !== '') {
      mqttOptions.password = config.mqtt_password;
    }
    mqttClient = mqtt.connect(config.mqtt_address, mqttOptions);
    mqttClient.on('connect', options => {
      mqttClient.subscribe([config.mqtt_topic], () => {
        console.log(`Connected to MQTT at '${config.mqtt_address}'`);
        console.log(`Subscribed to topic '${config.mqtt_topic}'`);
      });
    });
    mqttClient.on('message', (topic, payload) => {
      const event = JSON.parse(payload.toString());
      const before = event.before;
      const after = event.after;
      const camera = after.camera;
      const label = after.label;
      if (debugLogging) {
        console.log(`Event: camera=${camera}, label=${label}`);
      }
      if (!before?.has_snapshot && after?.has_snapshot) {
        let doSendNotification = true;
        if (disabledCameras.has(camera.toLowerCase())) {
          console.log(`Disabled cameras does have ${camera.toLowerCase()}`);
          const objects = disabledCameras.get(camera);
          if (objects.length === 0) {
            doSendNotification = false;
            console.log(`Disabled cameras -> disabled objects is empty`);
          } else {
            console.log(`Disabled cameras -> disabled objects =${JSON.stringify(objects, null, 2)}`);
          }
          if (objects.has(label.toLowerCase())) {
            doSendNotification = false;
            console.log(`Disabled cameras -> disabled objects does have ${label.toLowerCase()}`);
          } else {
            console.log(`Disabled cameras -> disabled objects does not have ${label.toLowerCase()}`);
          }
        } else if (debugLogging) {
          console.log(`Disabled cameras does not have ${camera.toLowerCase()}`);
        }
        if (disabledObjects.has(label.toLowerCase())) {
          if (debugLogging) {
            console.log(`Disabled objects does have ${label.toLowerCase()}`);
          }
          doSendNotification = false;
        } else if (debugLogging) {
          console.log(`Disabled objects does not have ${label.toLowerCase()}`);
        }
        if (doSendNotification) {
          sendNotification(camera, label, after.id);
        }
      }
    });
  } catch (e) {
    console.error(`Error connecting to MQTT broker at ${config.mqtt_address}: ${e}`);
  }
}

function sendNotification(camera, label, id) {
  if (config.ntfy_enabled) {
    sendNtfyNotification(camera, label, id);
  }
  if (config.ha_enabled) {
    config.ha_entity_ids.forEach(entityId => {
      sendHaNotification(camera, label, id, entityId);
    });
  }
}

function sendNtfyNotification(camera, label, id) {
  let priority = config.ntfy_normal_priority;
  if (config.grouping_enabled) {
    const now = new Date();
    if (lastNotificationDate) {
      const diff = now - lastNotificationDate
      const diffMinutes = Math.floor(diff / 60000);
      if (diffMinutes >= config.grouping_minutes) {
        lastNotificationDate = now;
      } else {
        priority = config.ntfy_lower_priority;
      }
    } else {
      lastNotificationDate = now;
    }
  }
  const options = {
    method: 'POST',
    headers: {
      'Title': capitalizeFirstLetter(label),
      'Attach': `${config.frigate_url}/api/events/${id}/snapshot.jpg${formatSnapshotOptions()}`,
      'Click': `${config.frigate_url}/api/events/${id}/clip.mp4`,
      'Tags': tagsMap.get(label),
      'Priority': priority
    },
    body: capitalizeFirstLetter(camera)
  };
  if (config.ntfy_basicAuth) {
    options.headers.Authorization = config.ntfy_basicAuth;
  } else if (config.ntfy_token) {
    options.headers.Authorization = config.ntfy_token;
  }
  fetch(`${config.ntfy_url}/${config.ntfy_topic}`, options)
      .then(response => {
        if (response.status !== 200) {
          console.error(`Non-successful response from ntfy request: ${response.status} ${response.statusText}`);
        }
      })
      .catch(error => console.error(`Error sending request to ntfy at ${config.ntfy_url}:`, error));
}

function sendHaNotification(camera, label, id, entityId) {
  const body = {
    title: capitalizeFirstLetter(label),
    message: capitalizeFirstLetter(camera),
    data: {
      image: `${config.frigate_url}/api/events/${id}/snapshot.jpg${formatSnapshotOptions()}`,
      clickAction: `${config.frigate_url}/api/events/${id}/clip.mp4`
    }
  }
  const options = {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${supervisorToken}`,
      'content-type': 'application/json'
    },
    body: JSON.stringify(body)
  };
  fetch(`http://supervisor/core/api/services/notify/${entityId}`, options)
      .then(response => {
        if (response.status !== 200) {
          console.error(`Non-successful response from Home Assistant API request: ${response.status} ${response.statusText}`);
        }
      })
      .catch(error => console.error(`Error sending request to Home Assistant:`, error));
}

function formatSnapshotOptions() {
  return config.snapshot_options ?
      '?' + Object.entries(config.snapshot_options)
          .map(([key, value]) => `${key}=${value}`)
          .join('&') :
      '';
}

function capitalizeFirstLetter(string) {
  return string.charAt(0).toUpperCase() + string.slice(1);
}

initialize().then();
