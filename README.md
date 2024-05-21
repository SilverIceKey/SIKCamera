# SIKCamera
### 集成方式：

在项目的setting.gradle或者root下的build.gradle中找到

```groovy
repositories {
	maven { url 'https://jitpack.io' }
}
```

在app的build.gradle中进行依赖,版本：[![](https://jitpack.io/v/SilverIceKey/SIKExtension.svg)](https://jitpack.io/#SilverIceKey/SIKExtension)所有模块版本相同

```groovy
//这样会集成所有模块
implementation 'com.github.SilverIceKey:SIKExtension:Tag'
//如果想集成单个模块
implementation 'com.github.SilverIceKey.SIKExtension:模块名称:Tag'
```

### 项目介绍：

本项目是使用CameraX进行简单的摄像头调用

### 项目包含以下元素：

- #### 缝合怪
- #### 屎山代码
- #### 高耦合
- #### 拎不清代码一堆

