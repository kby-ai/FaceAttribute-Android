<p align="left">
  <img src="https://user-images.githubusercontent.com/125717930/225975240-24b9a8ad-8cc6-4d5f-9a91-1435951b0bd7.png" width="120" alt="Nest Logo" />
</p>

# FaceAttribute-Android

## Introduction

This demo project integrates several facial recognition technologies, including 3D passive face liveness detection, face recognition, automatic face capture, and analysis of various face attributes such as age, gender, face quality, facial occlusion, eye closure, and mouth opening.

The system utilizes Face Liveness Detection technology to generate a real-time liveness score based on a single image captured by the camera. 

Additionally, the demo offers Face Recognition capabilities, enabling enrollment from a gallery and real-time identification of faces captured by the camera.

The demo also features an automatic Face Capture function that verifies various facial attributes, such as face quality, facial orientation (yaw, roll, pitch), facial occlusion (e.g., mask, sunglass, hand over face), eye closure, mouth opening, and the position of the face within the region of interest (ROI).

Moreover, the demo can compute scores for different face attributes from a gallery image, including liveness, face orientation (yaw, roll, pitch), face quality, luminance of the face, facial occlusion, eye closure, mouth opening, age, and gender.

> The demo is integrated with KBY-AI's Premimum Face SDK.

  | Basic      | Standard | Premimum |
  |------------------|------------------|------------------|
  | Face Detection        | Face Detection    | Face Detection |
  | Face Liveness Detection        | Face Liveness Detection    | Face Liveness Detection |
  | Pose Estimation        | Pose Estimation    | Pose Estimation |
  |         | Face Recognition    | Face Recognition |
  |         |         | 68 points Face Landmark Detection |
  |         |         | Face Quality Calculation |
  |         |         | Face Occlusion Detection |
  |         |         | Eye Closure Detection |
  |         |         | Age, Gender Estimation |


## Try the APK

### Google Play

<a href="https://play.google.com/store/apps/details?id=com.kbyai.facerecognition" target="_blank">
  <img alt="" src="https://user-images.githubusercontent.com/125717930/230804673-17c99e7d-6a21-4a64-8b9e-a465142da148.png" height=80/>
</a>

### Google Drive

https://drive.google.com/file/d/14Gf4MlUZTCHlIJvEB7TemuqsDZR-vL17/view?usp=sharing

## Screenshots

<p float="left">
  <img src="https://user-images.githubusercontent.com/125717930/235572284-dd230e36-ec37-42aa-9482-35c78a8a7ce7.png" width=240/>
  <img src="https://user-images.githubusercontent.com/125717930/235572299-23989a6e-d6cc-41d3-b75c-a235018ee464.png" width=240/>
  <img src="https://user-images.githubusercontent.com/125717930/235572304-41714fd2-b117-4bcf-999d-32c6d5c1a0c5.png" width=240/>
</p>

<p float="left">
  <img src="https://user-images.githubusercontent.com/125717930/235572987-c8cdfa0a-34cc-4f29-8b1a-f58288c71eb0.png" width=240/>
  <img src="https://user-images.githubusercontent.com/125717930/235573098-8a15bf1a-3930-4b47-b79b-ffb63e9940f2.png" width=240/>
  <img src="https://user-images.githubusercontent.com/125717930/235573303-de38b7e1-e962-4057-886e-1dec5f75a5b5.png" width=240/>
</p>

<p float="left">
  <img src="https://user-images.githubusercontent.com/125717930/235573436-6944cd3b-3b93-404d-b8c7-c02fc83d4bdc.png" width=240/>
  <img src="https://user-images.githubusercontent.com/125717930/235573468-7bd0ad22-16aa-4c7f-bf56-32915a4a842c.png" width=240/>
  <img src="https://user-images.githubusercontent.com/125717930/235573974-413e09cd-d822-46bb-b0dc-cb0e95a5babc.png" width=240/>
</p>

## SDK License

The face recognition project relies on kby-ai's SDK, which requires a license for each application ID.

- The code below shows how to use the license: https://github.com/kby-ai/FaceAttribute-Android/blob/db2f1134af4ce947c318d5213f1b1e703c400dbf/app/src/main/java/com/kbyai/faceattribute/MainActivity.kt#L33-L43

- To request a license, please contact us:
  ```
  Email: contact@kby-ai.com

  Telegram: @kbyai

  WhatsApp: +19092802609

  Skype: live:.cid.66e2522354b1049b
  ```

## About SDK

### 1. Set up
- Copy the SDK (libfacesdk folder) to the root folder of your project.

- Add SDK to the project in settings.gradle
  ```
  include ':libfacesdk'
  ```

- Add dependency to your build.gradle
  ```
  implementation project(path: ':libfacesdk')
  ```

### 2. Initializing an SDK

- Step One

  To begin, you need to activate the SDK using the license that you have received.
  ```
    FaceSDK.setActivation("...")
  ```
  If activation is successful, the return value will be SDK_SUCCESS. Otherwise, an error value will be returned.

- Step Two

  After activation, call the SDK's initialization function.
  ```
  FaceSDK.init(getAssets());
  ```
  If initialization is successful, the return value will be SDK_SUCCESS. Otherwise, an error value will be returned.

### 3. SDK Classes

  - FaceBox
  
    This class represents the output of the face detection function and can be utilized in template creation functions.

    | Feature| Type | Name |
    |------------------|------------------|------------------|
    | Face rectangle        | int    | x1, y1, x2, y2 |
    | Face angles (-45 ~ 45)        | float    | yaw, roll, pitch |
    | Liveness score (0 ~ 1)        | float    | liveness |
    | Face quality (0 ~ 1)        | float    | face_quality |
    | Face luminance (0 ~ 255)       | float    | face_luminance |
    | Face occlusion (0 ~ 1)       | float    | face_occlusion |
    | Eye closure (0 ~ 1)       | float    | left_eye_closed, right_eye_closed |
    | Mouth opening (0 ~ 1)       | float    | mouth_opened |
    | Age, gender        | int    | age, gender |
    | 68 points facial landmark        | float[]    | landmarks_68 |
  
    > 68 points facial landmark
    
    <img src="https://user-images.githubusercontent.com/125717930/235560305-ee1b6a39-5dab-4832-a214-732c379cabfd.png" width=500/>

  - FaceDetectionParam
  
    This class serves as the input parameter for face detection, enabling various processing functionalities such as face liveness detection, eye closure checking, facial occlusion checking, mouth opening checking, and age and gender estimation.

    | Feature| Type | Name |
    |------------------|------------------|------------------|
    | Check liveness        | boolean    | check_liveness |
    | Check eye closure        | boolean    | check_eye_closeness |
    | Check face occlusion        | boolean    | check_face_occlusion |
    | Check mouth opening        | boolean    | check_mouth_opened |
    | Estimate age, gender        | boolean    | estimate_age_gender |
    
### 4. SDK APIs
#### - Face Detection

  The Face SDK provides a unified function for detecting faces, enabling multiple functionalities such as liveness detection, face orientation (yaw, roll, pitch), face quality, facial occlusion, eye closure, mouth opening, age, gender, and facial landmarks.

  The function can be used as follows:

  ```
  FaceSDK.faceDetection(bitmap, param)
  ```

  This function requires two parameters: a Bitmap object and a FaceDetectionParam object that enables various processing functionalities.

  The function returns a list of FaceBox objects.

### - Create Templates

  The FaceSDK provides a function that can generate a template from a bitmap image. This template can then be used to verify the identity of the individual captured in the image.

  ```
  byte[] templates = FaceSDK.templateExtraction(bitmap, faceBox);
  ```

  The SDK's template extraction function takes two parameters: a bitmap object and an object of FaceBox. 

  The function returns a byte array, which contains the template that can be used for person verification.

### - Calculation similiarity

  The "similarityCalculation" function takes a byte array of two templates as a parameter. 

  ```
  float similarity = FaceSDK.similarityCalucation(templates1, templates1);
  ```

  It returns the similarity value between the two templates, which can be used to determine the level of likeness between the two individuals.

### - Yuv to Bitmap
  The SDK provides a function called yuv2Bitmap, which converts a yuv frame to a bitmap. Since camera frames are typically in yuv format, this function is necessary to convert them to bitmaps. The usage of this function is as follows:
  ```
  Bitmap bitmap = FaceSDK.yuv2Bitmap(nv21, image.getWidth(), image.getHeight(), 7);
  ```
  The first parameter is an nv21 byte array containing the yuv data. 

  The second parameter is the width of the yuv frame, and the third parameter is its height. 

  The fourth parameter is the conversion mode, which is determined by the camera orientation.

  To determine the appropriate conversion mode, the following method can be used:
  ```
  1        2       3      4         5            6           7          8

  888888  888888      88  88      8888888888  88                  88  8888888888
  88          88      88  88      88  88      88  88          88  88      88  88
  8888      8888    8888  8888    88          8888888888  8888888888          88
  88          88      88  88
  88          88  888888  888888
  ```

# References

- [Face Liveness Detection - Android](https://github.com/kby-ai/FaceLivenessDetection-Android)
- [Face Liveness Detection - iOS](https://github.com/kby-ai/FaceLivenessDetection-iOS)
- [Face Recognition - Android](https://github.com/kby-ai/FaceRecognition-Android)
- [Face Recognition - iOS](https://github.com/kby-ai/FaceRecognition-iOS)

