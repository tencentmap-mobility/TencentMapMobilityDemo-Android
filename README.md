# 出行SDK接入文档（安卓）

## 概述

出行SDK是针对于出行场景开发的多个SDK，包括:周边车辆SDK,推荐上车点SDK，出行检索SDK和司乘同显SDK。每个SDK功能独立、接入方便，开发者可以根据自己的需求进行选择。

## 接口文档地址
https://tencentmap-mobility.github.io/

## 依赖项
司乘同显司机端依赖地图SDK+导航SDK。建议使用版本地图SDK v4.5.6.2，导航SDK v5.3.9.3

注意:
在导航SDK v5.3.9以及地图SDK v4.5.6以后需要调用相应接口告知用户已同意隐私协议，使用方式参考demo中的MyApplication中（在使用导航和地图功能前调用即可）。 隐私协议内容: https://lbs.qq.com/userAgreements/agreements/privacy

### 腾讯导航、地图SDK
详细信息和使用方式可以在官网 https://lbs.qq.com 上获得。

## SDK文档列表
1. [出行周边车辆SDK](app/src/main/assets/readme/%E5%91%A8%E8%BE%B9%E8%BD%A6%E8%BE%86.md)
2. [出行推荐上车点SDK](app/src/main/assets/readme/%E6%8E%A8%E8%8D%90%E4%B8%8A%E8%BD%A6%E7%82%B9.md)
3. [出行检索SDK](app/src/main/assets/readme/%E6%A3%80%E7%B4%A2.md)

## 司乘SDK版本变更说明
已不在github维护，请到官网链接查看详细的版本发布和接入指南：https://lbs.qq.com/mobile/AndroidLSSDK/ReleaseNode
