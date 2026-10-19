# RideDeck Dash 0.8.10

The shared Velocity R and Roadline identity now appears in the launcher, cockpit and startup sequence. Light and Dark palettes match the RideDeck family. Recording remains amber; speed, GPS and stabilisation stay transparent over the camera view. Initial GPS speed is unavailable until measured, rather than briefly showing zero. Existing recording, storage, power and companion-authentication policies are unchanged. Live riding, call audio and long recording reliability are not established by visual checks.

## Previous shared menu update

The approved category list and section tabs now use the same navigation component across Standard, Yamaha and Dash.

General keeps Screen & layout, Headset & sound, Background & power, and Help & app in the same order. Ride contains each app's applicable features. Yamaha-specific bike connection, display/maps, data/ride book, and tests/logs are collected under Yamaha. Standard retains external map apps.

The old accordion and all-in-one Customization menus are removed. Existing working settings and notification, recording, navigation, background and audio policies are retained. Back returns to the category list; section, category and scroll position survive recreation. Setup supports portrait and landscape, using existing System, Light, Dark and accent preferences.

Validation includes the app unit tests and emulator navigation checks in light and dark mode, a 320 dp display, recreation and rotation. Broader lint reports existing errors in unchanged Standard and Yamaha files; no new menu errors were reported. Dash lint passes. Physical calls, motorcycle operation and outdoor recording are separate from this menu update.
