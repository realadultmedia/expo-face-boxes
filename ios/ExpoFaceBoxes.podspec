require 'json'

package = JSON.parse(File.read(File.join(__dir__, '..', 'package.json')))

Pod::Spec.new do |s|
  s.name           = 'ExpoFaceBoxes'
  s.version        = package['version']
  s.summary        = package['description']
  s.description    = 'Apple Vision face rectangles, normalised to the upright photo.'
  s.license        = package['license']
  s.author         = 'Real Adult Media'
  s.homepage       = package['homepage']
  s.platforms      = {
    :ios => '16.4'
  }
  s.source         = { git: 'https://github.com/realadultmedia/expo-face-boxes.git', tag: "v#{s.version}" }
  s.static_framework = true

  s.dependency 'ExpoModulesCore'
  s.frameworks = 'Vision', 'ImageIO', 'CoreML'

  s.pod_target_xcconfig = {
    'DEFINES_MODULE' => 'YES'
  }

  s.source_files = "**/*.{h,m,mm,swift}"
end
