---
layout: default
title: LTE Bands Lock Privacy Policy
---

# Privacy Policy for LTE Bands Lock

Last updated: October 4, 2026

LTE Bands Lock (package name: `com.ltebandslock`, also displayed as LTE Lock or LTE Band Manager) is an independently developed tool for managing compatible Huawei routers. This policy describes how the app handles information.

## Information used by the app

The app uses the router address, administrator username, and password you provide to connect to and authenticate with your router. Saved router profiles, including these credentials, are stored in the app's local storage on your Android device. Appearance, language, and speed-unit preferences are also stored locally.

Depending on the features you use and your router's capabilities, the app reads router and network information, including device identifiers, signal measurements, frequency bands, carrier and cell identifiers, traffic statistics, and connected-device information. This information is used to display status and perform the router operations you request.

When you use SMS management, the app accesses messages held by the router, including phone numbers, message contents, and timestamps. Sending or deleting a message sends the corresponding request to your router. This feature uses the router's SMS interface rather than accessing the Android phone's SMS inbox. Your mobile operator processes SMS messages sent through the router.

The app does not operate a developer-hosted service for receiving your router credentials, router information, or SMS contents. The current version does not include advertising, analytics, or automatic crash-reporting services.

## Optional connections to external services

- **Speed tests:** When you start a speed test, the app connects to Cloudflare's speed-test service at `speed.cloudflare.com` and transfers test data. Cloudflare receives your connection's public IP address and network request information. Upload tests send generated test data, not your personal files. These tests consume network data. See [Cloudflare's Privacy Policy](https://www.cloudflare.com/privacypolicy/).
- **Cell information links:** If you open CellMapper or OpenCellID, your browser connects to that service. The CellMapper link includes the carrier codes (MCC and MNC) shown by the app. These services receive normal browser connection information, such as your public IP address, and apply their own privacy policies.
- **Copying information:** If you choose to copy cell information, the selected information is placed on Android's clipboard. Clipboard handling is controlled by Android and may make the copied information available to other apps under the system's rules.

## Local storage, backups, and diagnostic logs

Saved router passwords are stored with the router profile; the current app does not separately encrypt these passwords before storing them. Protect your device and avoid sharing app data or backups containing credentials.

Android backup is enabled in the app. Depending on your device and backup settings, Android or your device's backup service may back up and restore app data, including saved profiles. Manage this through your device's backup settings.

The app writes some router responses and errors to Android diagnostic logs. These may contain router information, connected-device information, and SMS contents. The app does not automatically upload these logs to the developer. Review and remove sensitive information before sharing logs or screenshots.

Router connections may use HTTP, depending on the address and router configuration. HTTP does not encrypt network traffic. Use the app on a trusted network and do not assume every router connection is encrypted.

## Retention and deletion

Saved profiles and preferences remain on your device until you change or delete them, clear the app's storage, or uninstall the app. You can remove saved router profiles using the app's profile-management controls. Deleting a profile does not change the router's administrator credentials.

Clearing app storage removes the app's local saved data. Separately manage any copies held by your device's backup service. Removing app data does not delete messages or settings stored on the router; those are managed separately through the router or the app's relevant controls.

## Permissions

The app requests internet access to communicate with routers and speed-test services, network-state access to check connectivity, and vibration access for interface feedback. Its current Android manifest does not request phone SMS, contacts, or location permissions.

## Children

The app is a router-management utility and is not designed specifically for children. The developer does not receive personal information through an in-app account or developer-hosted data-collection service.

## Policy changes

This policy may be updated when the app's functionality or data handling changes. The latest version will be published on this page with an updated date.

## Contact

For privacy questions, contact the developer through the [LTE Bands Lock GitHub repository](https://github.com/zzshcool/ltebandslock). If Issues are enabled, you may open an issue there. Do not post passwords, private SMS messages, or other sensitive information in a public issue.
