import { requireOptionalNativeModule } from 'expo-modules-core';

/** A face's box as fractions (0-1) of the photo the right way up, top-left origin. */
export type FaceBox = { x: number; y: number; width: number; height: number };

type ExpoFaceBoxesModule = {
  /** Finds every face in the photo at `uri` (a file:// or content:// URI). Runs on the device. */
  detectFaces(uri: string): Promise<FaceBox[]>;
};

/**
 * The native module, or null when the running build doesn't include it
 * (Expo Go, or an app built before the module was added). Check for null and
 * fall back to letting people cover faces by hand.
 */
export const Faces = requireOptionalNativeModule<ExpoFaceBoxesModule>('ExpoFaceBoxes');
