# 司乘同显SDK接⼊⽂档（安卓）

## 概述

司乘同显SDK是在⽹约⻋接驾送驾场景中，帮助司机和乘客两端实时了解⾏程信息，可以同步展示司机端的路线、路况、剩余⾥程和剩余时间以及双⽅的实时位置和⾏驶轨迹。

乘客端使⽤司乘同显SDK时，需要依赖地图SDK，通过地图SDK中进⾏路线绘制、添加覆盖物等功能。
使⽤司乘同显SDK时需配置订单ID、司机ID和乘客ID来建⽴三者的关联关系。另外，订单有三个属性，
分别是订单ID，订单类型和订单状态，订单类型包括快⻋和顺⻛⻋，订单状态包括未派单、已派单、计费中。

⽤户可使⽤Deme查看司乘SDK的使⽤效果。准备两个⼿机，⼀个打开司机端，另⼀个打开乘客端。测
试时，司机端已开启同步功能并且已经进⼊导航界⾯，乘客端点击“启动同步”按钮，可看到司机端当前
路线和⼩⻋的平滑移动效果。

## 准备⼯作

### 申请开发密钥

司乘SDK使⽤前需要先配置APIKey进⾏鉴权，具体可联系对应的商务同学来开通。

### ⼯程配置

#### ⼀、配置地图SDK（必须）

司乘同显SDK（司机端&乘客端）需要依赖3D地图SDK，可在官⽹进⾏3D地图SDK的下载和⼯程配置
（地图⼯程配置指引：https://lbs.qq.com/android_v1/index.html 地图key申请成功后，可在
gradle⽂件中引⽤，如：

```java
    implementation 'com.tencent.map:tencent-map-vector-sdk:4.4.5.1'
```

并在AndroidMainfest⽂件中，配置key，如：

```java
    <meta-data
        android:name="TencentMapSDK"
        android:value="官⽹申请的地图key" />
```

#### ⼆、配置导航SDK（必须）

司乘同显SDK司机端需配合导航SDK使⽤，导航权限可联系对应的商务同学来开通。开通权限后，可在
gradle⽂件中引⽤，如：

```java
    // 需与地图SDKv4.3.3.9后版本搭配使⽤
    implementation 'com.tencent.map:tencent-map-nav-sdk:5.3.1.2'
```

需要注意的是，导航SDK需要依赖支持库SDK，具体如下：

```java
    // 基础工具库
    implementation 'com.tencent.map:tencent-map-nav-surport:1.0.2.6'
```

#### 三、司乘SDK

开通权限后，需在AndroidMainfest⽂件中配置key（如果此key与地图sdk key相同，可不配置），如下：

```java
    <meta-data
        android:name="com.tencent.map.ls.api_key"
        android:value="申请的司乘key" />
```

#### 四、配置定位SDK

定位SDK的使⽤可以参考官⽹：https://lbs.qq.com/geo/index.html ，注：具体配置可参考gitHub demo

## 快速接入

1. [司乘同显司机端流程](https://github.com/tencentmap-mobility/TencentMapMobilityDemo-Android/blob/release/2.0/docs/driver.md)
2. [司乘同显乘客端流程](https://github.com/tencentmap-mobility/TencentMapMobilityDemo-Android/blob/release/2.0/docs/passenger.md)