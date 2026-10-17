# expo-face-boxes

Find faces in a photo on the phone, so they can be covered before the photo is shared.

Uses Apple Vision on iOS and Google ML Kit on Android. The photo never leaves the device: no server, no network call, no API key.

```ts
import { Faces } from '@realadultmedia/expo-face-boxes';

const boxes = (await Faces?.detectFaces(photo.uri)) ?? [];
// [{ x: 0.21, y: 0.18, width: 0.24, height: 0.33 }, …]
```

## Install

```bash
npx expo install @realadultmedia/expo-face-boxes
```

This is a native module, so it needs a [development build](https://docs.expo.dev/develop/development-builds/introduction/). It doesn't run in Expo Go. iOS 16.4+, Android with ML Kit's bundled face model.

## The boxes

Each box is a fraction (0 to 1) of the photo **as it is seen**: turned upright using its EXIF orientation, with the origin at the top-left. Multiply by the width and height you draw the photo at, and the box sits on the face.

Both platforms scale big photos down to about 2048 px before detecting, which keeps it fast without losing faces.

## When the module isn't there

`Faces` is `null` when the running build doesn't include the native module, for example in Expo Go or in an app built before you added it. Check for `null` and fall back to letting people cover faces by hand.

## Security

Please report security issues privately. See [SECURITY.md](https://github.com/realadultmedia/.github/blob/main/SECURITY.md).

## License

MIT. Built and used in production by [Real Adult Media](https://www.realadult.media).
