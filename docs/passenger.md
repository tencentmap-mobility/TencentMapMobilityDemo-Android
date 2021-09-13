# 司乘同显SDK乘客端（Android）

## 1. 初始化配置

首先需要初始化配置，包括乘客id和设备标识，代码如下：

```java
    /**
     * 司乘SDK 乘客端的初始化
     *
     * 配置信息类{@code TLSConfigPreference}可参考接口文档
     */
    final TSLPassengerManager mPassengerSync = TSLPassengerManager.newInstance();
    mPassengerSync.init(getApplicationContext(), TLSConfigPreference.create()
         .setAccountId(psgId) // 乘客ID
     .setDeviceId(deviceId)); // 设备ID
```

## 2. 司乘同显乘客端主流程

2.1 司机接单后，乘客端可开启司乘同显，需要设置对应的订单号和对应的订单类型和状态，如接驾的快车单：

```java
    mPassengerSync.getOrderManager().editCurrent()
            .setOrderId(orderId) // 订单ID
            .setSubOrderId(subOrderID) // 没有则为""
            .setOrderType(TLSBOrderType.TLSDOrderTypeNormal)
            .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusPickUp);
    mPassengerSync.start();// 开启司乘
```

2.2 通过回调获取相应的司机路线，轨迹和订单信息：

```java

    mPassengerSync.addTLSPassengerListener(new MyPullDriverInfo());

    class MyPullDriverInfo extends SimplePsgDataListener {

        @Override
        public void onPullLsInfoSuc(TLSDFetchedData fetchedData) {
            // 拉取司机信息成功，可进行乘客端地图界面展示，并可根据轨迹进行小车平滑运动。
        }

        @Override
        public void onPullLsInfoFail(int errCode, String errMsg) {
            // 拉取司机信息失败
        }

        @Override
        public void onPushPositionSuc() {
            // 上传定位点成功，只支持快车单司机展示乘客位置
        }

        @Override
        public void onPushPositionFail(int errCode, String errMsg) {
            // 上传定位点失败
        }
    }
```

2.3 快车单乘客端可上报定位点，来供司机端展示乘客位置（可选）：

```java
    mPassengerSync.getOrderManager().editCurrent()
            .setOrderId(orderId) // 订单ID
            .setOrderType(currOrderType)
            .setOrderStatus(currOrderState)
        .setCityCode(currCityCode); // 当前城市编码
    mPassengerSync.uploadPosition(TLSBPosition); // 上报定位点
```

2.4 接到乘客后，将订单状态改为送驾

```java
    mPassengerSync.getOrderManager().editCurrent()
            .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusTrip);
```

2.5 结束司乘同显

```java
    mPassengerSync.stop(); // 结束司乘
```

## 3. 接力单

如果当前乘客的订单是接力单，那么这个订单的路线会分为两段：司机送其他乘客的送驾路线route和司机送其他乘客后接驾路线relayRoute。路线需要拼接route+relayRoute， 剩余时间和里程也需要拼接，可参考demo 。

其中司机送其他乘客的送驾路线route:

```java
    final Route route = mPassengerSync.getRouteManager().getUsingRoute();
```
如果当前是接力单，则relayRoute存在:

```java
    if (mPassengerSync.getOrderManager().isRelay()) {
        TLSBRoute relayRoute = mPassengerSync.getRouteManager()
                .getRouteByOrderId(manager.getOrderManager().getOrderId());
    }
```

## 4. 乘客选路

乘客可以在送驾前和送驾中去提前选择或切换送驾路线。

### 4.1 送驾前选路

送驾前包括乘客等待接单时段和接驾时段。调用TSLPassengerManager相关方法获取路线多方案，最多3条路线

```java
    mPassengerSync.mPassengerSync.searchRoutes(final TLSLatlng from, final TLSLatlng to,
                     DrivingParam.Policy policy, DrivingParam.Preference[] preferences,
                     final OnSearchResultListener listener);
```
开发者自定义选路页面，当乘客确定路线后，调用TSLPassengerManager选择送驾路线方法:

```java
    mPassengerSync.routeSelectByIndex(integer); // 选择第几条路线
    mPassengerSync.addTLSPassengerListener(new SimplePsgDataListener() {
        @Override
        public void onRouteSelectSuccess() {
            // 这是路线选择成功回调，收到回调直接上报选中路线
            mPassengerSync.uploadUsingRoute();
        }
     });

```

### 4.2 送驾中选路

送驾中，在SimplePsgDataListener#onPullLsInfoSuc 拉取路线成功后，当前路线和备选路线都会同步更新至RouteManager内，开发者可获取全部路线：

```java
    mPassengerSync.getRouteManager().getRoutes(); // 获取全部路线
```

开发者自定义选路页面，当乘客确定切换路线后，调用TSLPassengerManager切换路线方法:

```java
    mPassengerSync.routeSelectByIndex(integer); // 选择第几条路线
    mPassengerSync.addTLSPassengerListener(new SimplePsgDataListener() {
        @Override
        public void onRouteSelectSuccess() {
            // 这是路线选择成功回调，收到回调直接上报选中路线
            mPassengerSync.uploadUsingRoute();
        }
     });

```

## 5. 司乘同显乘客端回调

```java
    /**
     * 上传定位点成功
     */
    void onPushPositionSuc();
    /**
     * 上传定位点失败
     */
    void onPushPositionFail(int errCode, String errMsg);
    /**
     * 拉取司机信息成功
     */
    void onPullLsInfoSuc(TLSBRoute route, TLSBOrder order, ArrayList<TLSBDriverPosition> pos);
    /**
     * 拉取司机信息失败
     */
    void onPullLsInfoFail(int errCode, String errMsg);
    /**
     * 发起选路请求成功回调
     */
    void onPushRouteSuc();
    /**
     * 发起选路请求失败回调
     */
    void onPushRouteFail(int errCode, String errStr);
```
