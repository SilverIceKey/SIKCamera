# SIKCamera
### 集成方式：

在项目的setting.gradle或者root下的build.gradle中找到

```groovy
dependencyResolutionManagement {
		repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
		repositories {
			mavenCentral()
			maven { url 'https://jitpack.io' }
		}
	}
```

在app的build.gradle中进行依赖,版本：[![](https://jitpack.io/v/SilverIceKey/SIKCamera.svg)](https://jitpack.io/#SilverIceKey/SIKCamera)所有模块版本相同

```groovy
dependencies {
	implementation("com.github.SilverIceKey:SIKCamera:Tag")
}
```

### 项目介绍：

本项目是使用CameraX进行简单的摄像头调用

### 项目包含以下元素：

- #### 缝合怪
- #### 屎山代码
- #### 高耦合
- #### 拎不清代码一堆

