# react-native-fitness-watcher

## Getting started

`$ npm install react-native-fitness-watcher --save`

### Mostly automatic installation (Skip this step for React Native 0.60 and above.)
`$ react-native link react-native-fitness-watcher`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-fitness-watcher` and add `FitnessWatcher.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libFitnessWatcher.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android (Skip this step for React Native 0.60 and above.)

1. Open up `android/app/src/main/java/[...]/MainApplication.java`
  - Add `import com.reactlibrary.FitnessWatcherPackage;` to the imports at the top of the file
  - Add `new FitnessWatcherPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-fitness-watcher'
  	project(':react-native-fitness-watcher').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-fitness-watcher/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-fitness-watcher')
  	```


## Usage
```javascript
import FitnessWatcher from 'react-native-fitness-watcher';

// TODO: What to do with the module?
FitnessWatcher;
```
