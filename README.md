# Stario

<img src="docs/representative.png" alt="Stario representative">

[![Discord](https://img.shields.io/discord/1110537583726964820?label=Discord&logo=discord&logoColor=white)](https://discord.gg/WuVapMt9gY)
[![Latest release](https://img.shields.io/github/downloads/albu-razvan/Stario/total?label=Download&logo=android&logoColor=white)](https://github.com/albu-razvan/Stario/releases/latest)
[![Last commit](https://custom-icon-badges.demolab.com/github/last-commit/albu-razvan/Stario?logo=history&logoColor=white&label=Latest%20commit)](https://github.com/albu-razvan/Stario/commit)

## Overview

Inspired by the minimalist phone concept, Stario aims to keep functionality and productivity at 
their peak in a simple and elegant format.

This repository contains the complete codebase for Stario, a full rewrite of the previous
Stario Launcher. This version offers significant improvements in both performance and usability.

> **Note:** This version supersedes the old Stario Launcher. The old Play Store version no longer
> supported and this repository represents the latest and actively maintained version.

## Features

- **Material You Support**  
  Integrates seamlessly with Android’s Material You dynamic theming system, adapting colors based on
  your wallpaper and device settings.

- **Application Customization**  
  Customize your home screen with various icon packs and shapes to personalize your experience.

- **Built-In Weather Widget**  
  Check current weather conditions and forecasts right from your home screen.

- **Global Search Integration**  
  Perform fast, privacy-respecting searches using Kagi directly from the launcher.

- **Minimalistic Media Player Controls**  
  Manage your media playback easily with integrated controls.

- **Application Categories**  
  Organize your app drawer with customizable categories for better app management.

- **RSS/Atom Reader**  
  Stay up-to-date with news and blog feeds via the integrated RSS/Atom reader.

- **Page Sorting**  
  Easily reorder your home screen pages to suit your workflow.

## Download

Get the latest release of Stario from the [GitHub Releases page](https://github.com/albu-razvan/Stario/releases/latest).

## Reproducibility

You can quickly set up the development environment using the provided Dockerfile:

```bash
docker build --platform linux/amd64 -t stario-dev .

docker run --platform linux/amd64 --rm -it \
  -v /path/to/output:/usr/local/stario/build \
  -v /path/to/keystore:/usr/local/stario/keystore \
  stario-dev
```

- Use `--rm` to automatically remove the container after use.
- The keystore volume is only necessary for building release APKs.

To build the app, run:

```bash
./build_all.sh
```

After building, verify the APK integrity by comparing hash codes:

```bash
sha256sum path/to/built.apk
```

> For signed APKs, strip signatures on both the built APK and the reference APK before hashing. Use
> the following command to strip signatures:

```bash
cp path/to/original.apk path/to/unsigned.apk
zip -d path/to/unsigned.apk "META-INF/*"
```

If successful, the hashes should match exactly.

## Compatibility

- Requires **Android SDK 28+** (Android 9.0 Pie or later)
- Compatible with AOSP and most major OEM devices
- Should work with custom ROMs, though these are not officially tested — user feedback is welcome

## Join the Community

Got questions or want to connect with other users and contributors? Join the [Stario Discord Server](https://discord.gg/WuVapMt9gY).
