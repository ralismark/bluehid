# BlueHID

Bluetooth HID game controller for Android Oreo.
Requires Xposed, but no host-side software needed!

This is a proof of concept, so, even though it's mostly usable at its current stage, it has many rough edges.
This can also be taken as an example on how to use the BluetoothHidDevice API, as Oreo has this functionality already implemented, but with slight differences.

![Using BlueHID to play Titan Souls](media/bluehid.gif)

See my blog posts for a more detailed explanation, starting from [part 1](https://ralismark.github.io/2019/01/04/bluehid-1.html).

# Requirements

This project is set up for Android Oreo (specifically API level 27 - version 8.1), but you will need to to use the android.jar from [anggrayudi/android-hidden-api](https://github.com/anggrayudi/android-hidden-api) as this app uses hidden API.
You will also need Xposed (minimum version 83) on your phone to run this.

Android P has official support for this through BluetoothHidDevice.
The hidden Oreo API that is used here is very similar, but with a few differences (most notably, BluetoothHidDevice was named BluetoothInputHost in O), so it's possible to make this app support it with a few changes.

# Building

As described above, you need to replace your API 27 android.jar with one that exposes hidden API.
After that, you can open this in Android Studio and build it as you normally would.

# Running

If you haven't paired with the host device, you need to do so first.

To connect, open the app (enabling Bluetooth if prompted), press on `(disconnected)` and select the host device.
The status should change to `Connecting`, then `Connected` (Note that Connecting may not be visible for long).
After this, you should be able to use the app as a controller.
