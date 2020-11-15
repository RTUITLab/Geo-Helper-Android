# GeoHelper

![](https://img.shields.io/github/v/release/RTUITLab/Geo-Helper-Android?include_prereleases)

Android app for [GeoHelper](https://geohelper.rtuitlab.dev) –– the service for AR tips based on geo position

## APK download
* [Download on GitHub](https://github.com/RTUITLab/Geo-Helper-Android/releases)

## Building
* Clone repository (or download *.zip):
    * `git clone --recursive https://github.com/RTUITLab/Geo-Helper-Android.git`
* The project can be built with Android Studio 4.1+

## How to start
By default, app use `wss://geohelper.rtuitlab.dev/api` as API URL. If you want to use your own API URL you need to provide it in the [`gradle.properties`](gradle.properties#L23) file like so:
```groovy
api.url="your_url_here"
```
