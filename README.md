# pyLoad Android Client

The pyLoad Android Client connects to a running **pyLoad-ng** Download Manager over the network. It requires pyLoad Core **0.5.x** and Android **5.0 (API 21) or later**.

pyLoad is available as the [`pyload-ng` package on PyPI](https://pypi.org/project/pyload-ng/), or as a Docker image on [Linuxserver](https://docs.linuxserver.io/images/docker-pyload-ng/). Please report issues with the app on the [GitHub issue tracker](https://github.com/pyload/pyload-android/issues).

## Set up a server

The app needs the address of your pyLoad server and an API key to connect.

1. In pyLoad, open **Settings → Users**. Next to your username, choose **Actions → Manage API Keys → Generate**. Copy the key when it is shown; it cannot be viewed again after closing the dialog.
2. In the app, open **Settings → Servers** and add a server. Enter its name, host, port, and API key. You can also scan a QR code containing server configuration to fill in the server form.
3. Save the server to use it in the app.

You can configure multiple servers and switch between them. Server configuration, including the API key, is saved in the app's local preferences.

## Features

- View download status, progress, speed, and server status
- Browse the queue and collector; manage packages and downloads
- Add links and upload container files such as DLC and Torrent
- Enter captchas for downloads
- Toggle downloads and reconnect
- View accounts and edit server-side settings
- Configure multiple servers and switch between them
- Scan QR codes to fill in server configuration
- Connect over SSL, with certificate validation configurable per server
- Choose light, dark, or system theme; configure language and refresh behavior
- Use Click'N'Load to send links or DLC containers to a configured server

## Click'N'Load

Enable Click'N'Load in the app settings. When multiple servers are configured, choose whether to select the destination server for each request or use a selected server. The device running the app must be reachable by the Click'N'Load client over the network.

## Localization

The app is available in multiple languages. To help translate or proofread, visit the [pyLoad Crowdin project](https://crowdin.net/project/pyload).

## Contributing

The app is open source. Visit the [GitHub repository](https://github.com/pyload/pyload-android) to contribute.

Thanks to everyone who has helped improve the app:

- ollih - https://bitbucket.org/ollihh
- cimnine - https://github.com/cimnine
- jplitza - https://github.com/jplitza
