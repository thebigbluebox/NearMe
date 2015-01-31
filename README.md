Beacon Scanner & Logger
=======================

*** Note that the app's development environment has been migrated from Eclipse to Android Studio, the Eclipse folder contains artefacts from versions 1.3 and earlier, version 1.4 onwards can be found in the Android Studio folder ***

This Android app detects BLE Beacons and logs data to a file for some unspecified future use.

The app scans for Beacons and writes the details of any Beacons discovered to a file on the device. The app features functionality to select which properies should be logged and to email files from the device to an email address of the user's choice.

Scanning can be enabled and disabled via a button and the file produced will overwrite any previous versions of the same file.

Currently uses the free Android Beacon SDK from Radius Networks and portions of code adapted from David G Young's IBeacon Reference Application.

A pre-compiled version of the app is available for download via Google Play: https://play.google.com/store/apps/details?id=net.jmodwyer.ibeacon.ibeaconPoC

The source code for this application is made available under the terms of the MIT license.
