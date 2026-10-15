VangtiChai - CSE 489 Mobile Application Development, Assignment 1
==================================================================

VangtiChai is an Android app (Kotlin, XML layouts) that calculates change for an amount in
Taka notes: 500, 100, 50, 20, 10, 5, 2 and 1. The amount is typed on a numeric keypad built
from scratch with ConstraintLayout.

Minimum SDK: API 21 (Android 5.0 Lollipop), as the assignment suggests. Target/compile SDK: API 37.


What the app does
-----------------
- Each digit you tap is added to the right of the amount: 2, 3, 4 shows 2, then 23, then 234.
- The change table lists how many of each note make up the amount, using as few notes as
  possible (500: 1, 100: 1, 50: 1, ... for 688).
- CLEAR resets the amount and the table.
- A backspace key at the end of the amount row deletes the last digit, like the one beside the
  number in a phone dialer: tap it to remove one digit, or hold it to keep removing digits until
  it is let go. It only shows once something is typed. It sits in the header, so the keypad keeps
  exactly the configuration from the assignment (10 digits and CLEAR).
- Rotating the device keeps the amount and the change list. Both are saved in
  onSaveInstanceState() and restored in onCreate(); the activity is recreated so that the
  landscape (or portrait) layout is loaded.
- Amounts of up to 9 digits (999,999,999 Taka) are accepted. Typing more shakes the amount and
  shows "Up to 9 digits". Long amounts and counts shrink to fit, so everything stays on one
  screen in full.

Extras
- Material 3 design with a dark green "Taka" theme. The app is always dark, whatever the
  phone's own theme setting, so there is no theme switching.
- Smooth motion: the change table and keypad rise into place when the app opens; notes that
  are part of the change fade up in their own colour and the rest fade back; counts pop when
  they change; keys shrink slightly when pressed; haptic feedback on key presses.
- Edge-to-edge drawing that keeps content clear of the status bar, navigation bar and
  display cutouts.
- Hardware keyboard support (handy on an emulator): digits type, Backspace deletes the last
  digit, Esc or C clears.
- Screen reader support: the amount is announced when it changes and each table row has a
  spoken description (e.g. "1 note of 500 Taka").
- Custom adaptive launcher icon, including a themed (monochrome) version.


Layouts
-------
Four layouts, all built on ConstraintLayout (chains, guidelines and percentage sizing):

  res/layout/activity_main.xml               Phone, portrait     Pixel XL, 411 x 731 dp
  res/layout-land/activity_main.xml          Phone, landscape    Pixel XL, 731 x 411 dp
  res/layout-sw600dp/activity_main.xml       Tablet, portrait    Nexus 10, 800 x 1280 dp
  res/layout-sw600dp-land/activity_main.xml  Tablet, landscape   Nexus 10, 1280 x 800 dp
  res/layout/view_note_row.xml               One row of the change table (included 8 times)

Portrait: the amount ("Taka: 688") is on top, the change table on the left and a 3-column
keypad (1-9, then 0 and a wide CLEAR) on the right.
Landscape: the change table is split into two columns (500/100/50/20 and 10/5/2/1) and the
keypad becomes 4 columns (1-4, 5-8, then 9, 0 and a wide CLEAR).
Tablet layouts use the same arrangements, scaled up, with a subtitle in the app bar.

No values are hardcoded in the layout files. Text sizes, margins, paddings, corner radii and
layout proportions are defined in sizes.xml for each configuration:

  res/values/sizes.xml               phones, portrait (the defaults)
  res/values-land/sizes.xml          phones, landscape
  res/values-sw600dp/sizes.xml       tablets, portrait
  res/values-sw600dp-land/sizes.xml  tablets, landscape

All text is in res/values/strings.xml and all colours in res/values/colors.xml.


Devices and screen types tested
-------------------------------
Everything below was tested on the Android Emulator. On each device the on-screen keys were
tapped to enter 2, 3 and 4 (showing 2, then 23, then 234), CLEAR, 688, 6880 and the 9-digit
maximum 999999999, and the device was rotated portrait -> landscape -> portrait. The amount
and the change list were kept on every rotation, and the counts match the assignment's
screenshots (688: one of each note; 6880: 13, 3, 1, 1, 1, 0, 0, 0).

Required devices, on Android 17 (API 37) emulator images:

  Pixel XL    1440 x 2560 px, 560 dpi = 411 x 731 dp    layout/ and layout-land/
  Nexus 10    1600 x 2560 px, 320 dpi = 800 x 1280 dp   layout-sw600dp/ and layout-sw600dp-land/

Other screen types, emulated on those two devices by changing the screen size and density
(adb shell wm size / wm density), each in portrait and landscape:

  Nexus 5, small phone          1080 x 1920 px, 480 dpi = 360 x 640 dp
  Medium Phone (Pixel 7 size)   1080 x 2400 px, 420 dpi = 411 x 914 dp
  Nexus 7, 7-inch tablet        1200 x 1920 px, 320 dpi = 600 x 960 dp
  Pixel C, large tablet         1800 x 2560 px, 320 dpi = 900 x 1280 dp

Oldest supported version: a Pixel XL emulator running Android 5.0 (API 21), with the phone
layouts and the tablet layouts (at 800 x 1280 dp), in portrait and landscape.

The 7 Espresso UI tests (see Tests below) pass on all three emulators: Pixel XL, Nexus 10 and
Android 5.0.


Tests
-----
  app/src/test/.../ChangeCalculatorTest.kt  JVM unit tests for the change logic, including
                                            the four examples from the assignment
                                            screenshots and a check that the fewest notes
                                            are always used. Run: gradlew test
  app/src/androidTest/.../MainActivityTest.kt  Espresso tests for typing, CLEAR, backspace
                                            (tap and hold) and keeping state across
                                            recreation. Run on a device or emulator:
                                            gradlew connectedAndroidTest


Project structure
-----------------
  app/src/main/java/com/example/vangtichai/MainActivity.kt      UI, keypad, state saving
  app/src/main/java/com/example/vangtichai/ChangeCalculator.kt  Change calculation logic
