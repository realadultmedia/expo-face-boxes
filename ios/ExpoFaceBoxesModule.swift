import CoreML
import ExpoModulesCore
import ImageIO
import Vision

/*
  Finds faces in a photo, on the phone. Nothing leaves the device.

  detectFaces(uri) answers with one box per face as fractions (0-1) of the
  photo the right way up, top-left origin, ready to draw over the photo as
  it is shown.
*/
public class ExpoFaceBoxesModule: Module {
  public func definition() -> ModuleDefinition {
    Name("ExpoFaceBoxes")

    AsyncFunction("detectFaces") { (uri: String) -> [[String: Double]] in
      let url = uri.hasPrefix("/") ? URL(fileURLWithPath: uri) : URL(string: uri)
      guard let url, let source = CGImageSourceCreateWithURL(url as CFURL, nil) else {
        throw FaceBoxesException("That photo could not be opened.")
      }

      // Brought down to a size Vision works quickly on; faces stay findable.
      let options: [CFString: Any] = [
        kCGImageSourceCreateThumbnailFromImageAlways: true,
        kCGImageSourceThumbnailMaxPixelSize: 2048,
        kCGImageSourceCreateThumbnailWithTransform: false,
      ]
      guard let image = CGImageSourceCreateThumbnailAtIndex(source, 0, options as CFDictionary) else {
        throw FaceBoxesException("That photo could not be read.")
      }

      // The pixels as stored, plus the EXIF orientation: Vision turns them
      // upright itself, so its boxes are measured on the upright photo.
      let properties = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any]
      let exif = (properties?[kCGImagePropertyOrientation] as? UInt32) ?? 1
      let orientation = CGImagePropertyOrientation(rawValue: exif) ?? .up

      let request = VNDetectFaceRectanglesRequest()
      #if targetEnvironment(simulator)
        // The simulator has no Neural Engine context for Vision: run it on the CPU.
        if #available(iOS 17.0, *) {
          let cpu = MLComputeDevice.allComputeDevices.first { device in
            if case .cpu = device { return true }
            return false
          }
          if let cpu { request.setComputeDevice(cpu, for: .main) }
        } else {
          request.usesCPUOnly = true
        }
      #endif
      let handler = VNImageRequestHandler(cgImage: image, orientation: orientation, options: [:])
      try handler.perform([request])

      // Vision's origin is bottom-left; ours is top-left.
      return (request.results ?? []).map { face in
        let box = face.boundingBox
        return [
          "x": Double(box.minX),
          "y": Double(1 - box.maxY),
          "width": Double(box.width),
          "height": Double(box.height),
        ]
      }
    }
  }
}

internal final class FaceBoxesException: GenericException<String> {
  override var reason: String {
    param
  }
}
