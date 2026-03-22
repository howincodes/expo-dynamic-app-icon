# @howincodes/expo-dynamic-app-icon

Easily **change your app icon dynamically** in **Expo SDK 53+**!

## What's New in v3:

- **Expo SDK 53/54/55 support**
- **Android crash fix** — no more unexpected app closures after icon change
- **`setAppIcon(null)` fix** — resetting to default now works correctly on Android
- **Proper error handling** — `setAppIcon` now returns `false` when it actually fails
- **`setAppIcon` is now async** — returns a Promise
- **Thread-safe Android** — no more race conditions on rapid icon changes
- **Image path validation** — clear errors when icon files are missing
- **Deep link preservation** — activity aliases now inherit all intent filters
- **iOS private API fallback** — gracefully falls back to public API if private API unavailable

## Features:

- Reset icon to default
- Support for **round icons**
- Different icons for **iOS and Android**
- Dynamic icon variants for **iOS** (light, dark, tinted)
- iOS icon update **with or without alert popup**
- **Simple API** to get and set the app icon
- **Android icon change without app restart**

## Demo

![dynamic-icon-demo-5](https://github.com/user-attachments/assets/3dced15a-8d4e-4eb9-b76c-4c7c8fc9f59a)

---

## Installation

```sh
npx expo install @howincodes/expo-dynamic-app-icon
```

---

## Setup

Add the plugin to your `app.json`:

```json
"plugins": [
  [
    "@howincodes/expo-dynamic-app-icon",
    {
      "defaultLight": {
        "ios": "./assets/ios_icon_light.png",
        "android": {
          "foregroundImage": "./assets/android_icon_light_fg.png",
          "backgroundColor": "#FFFFFF"
        }
      },
      "defaultDark": {
        "ios": "./assets/ios_icon_dark.png",
        "android": {
          "foregroundImage": "./assets/android_icon_dark_fg.png",
          "backgroundColor": "#121212"
        }
      },
      "legacyRed": {
        "ios": "./assets/ios_icon_red.png",
        "android": "./assets/android_icon_red_legacy.png"
      },
      "dynamicTheme": {
        "ios": {
          "light": "./assets/ios_icon_themed_light.png",
          "dark": "./assets/ios_icon_themed_dark.png",
          "tinted": "./assets/ios_icon_themed_tinted.png"
        }
      }
    }
  ]
]
```

**Note on Android Adaptive Icons:**
For Android, you can provide an object with `foregroundImage` (path to your foreground asset) and `backgroundColor` (hex string) to generate proper adaptive icons. If you provide a direct string path, it will be treated as a legacy icon.

---

## Android Setup

Run the following command:

```sh
expo prebuild
```

Then, check if the following lines have been added to `AndroidManifest.xml`. The `android:icon` and `android:roundIcon` attributes will point to different resources based on your configuration:

**For Adaptive Icons (example: `defaultDark`):**

```xml
<activity-alias
  android:name="expo.modules.dynamicappicon.example.MainActivitydefaultDark"
  android:enabled="false"
  android:exported="true"
  android:icon="@mipmap/ic_launcher_adaptive_defaultdark"
  android:roundIcon="@mipmap/ic_launcher_adaptive_defaultdark"
  android:targetActivity=".MainActivity">
  <intent-filter>
    <action android:name="android.intent.action.MAIN"/>
    <category android:name="android.intent.category.LAUNCHER"/>
  </intent-filter>
</activity-alias>
```

**For Legacy Icons (example: `legacyRed`):**

```xml
<activity-alias
  android:name="expo.modules.dynamicappicon.example.MainActivitylegacyRed"
  android:enabled="false"
  android:exported="true"
  android:icon="@mipmap/legacyred"
  android:roundIcon="@mipmap/legacyred_round"
  android:targetActivity=".MainActivity">
  <intent-filter>
    <action android:name="android.intent.action.MAIN"/>
    <category android:name="android.intent.category.LAUNCHER"/>
  </intent-filter>
</activity-alias>
```

---

## Usage

### **Set App Icon**

```typescript
import { setAppIcon } from "@howincodes/expo-dynamic-app-icon";

// Change app icon to 'red' (returns a Promise in v3)
const result = await setAppIcon("red");

// Reset to default icon
await setAppIcon(null);
```

#### Parameters:

```typescript
setAppIcon(
  name: IconName | null,
  isInBackground?: boolean
): Promise<IconName | "DEFAULT" | false>
```

| Parameter        | Type               | Default | Description                                                                                                                    |
| ---------------- | ------------------ | ------- | ------------------------------------------------------------------------------------------------------------------------------ |
| `name`           | `IconName \| null` | `null`  | The icon name to switch to. Pass `null` to reset to the default icon.                                                          |
| `isInBackground` | `boolean`          | `true`  | - `true`: Icon changes silently in the background (no alert on iOS).<br>- `false`: Immediate change, with system alert on iOS. |

#### Returns (Promise):

- `"DEFAULT"` if reset to the original icon.
- The **new icon name** on success.
- `false` if an error occurs.

---

### **Get Current Icon**

```typescript
import { getAppIcon } from "@howincodes/expo-dynamic-app-icon";

// Get the current app icon name (async in v3)
const icon = await getAppIcon();
console.log(icon); // "red" (or "DEFAULT" if not changed)
```

---

### Platform Behavior:

- **iOS:** `await setAppIcon("dark")` resolves **after** the icon change completes (or fails). The return value accurately reflects success/failure.
- **Android:** `await setAppIcon("dark")` resolves **immediately** after queuing the change. The actual icon switch happens when the app enters the background. This means the promise always resolves with the icon name, even if the change hasn't been applied yet.

### Notes:

- **Android limitations:**
  Android does **not** support icon changes while the app is running in the foreground.
  To work around this, the icon is changed when the app enters the **Pause state** (background).

- **Pause state** can also trigger during events like permission dialogs.
  To avoid unwanted icon changes, a **5-second delay** is added to ensure the app is truly in the background.

- To disable the delay and apply the icon change immediately (with the risk of it running during permission dialogs or other pause events), set:

  ```typescript
  await setAppIcon("red", false);
  ```

  - On **iOS**, `isInBackground: false` triggers the system alert immediately.
  - On **Android**, it applies the icon change right away without waiting.

---

## Migration from v2 to v3

### Breaking Changes:

1. **`setAppIcon` and `getAppIcon` are now async** — wrap calls with `await`:
   ```typescript
   // v2
   const result = setAppIcon("dark");
   const icon = getAppIcon();

   // v3
   const result = await setAppIcon("dark");
   const icon = await getAppIcon();
   ```

2. **Minimum Expo SDK is now 53** — update your project if on SDK 52 or below.

3. **Error handling improved** — `setAppIcon` now returns `false` when the icon change actually fails (previously it returned the icon name even on failure).

---

## Support the Original Author

A huge shoutout to [outsung](https://github.com/outsung) for the original package!

If you find this useful, consider **buying him a coffee**:

<a href="https://www.buymeacoffee.com/outsung" target="_blank">
  <img src="https://www.buymeacoffee.com/assets/img/custom_images/orange_img.png" alt="Buy Me A Coffee" height="41" width="174" />
</a>

---

## About Us

This package is maintained by **[HowinCloud](https://howincloud.com/)** – delivering powerful cloud-based solutions for modern app development.
