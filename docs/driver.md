# 司乘同显SDK司机端（Android）

## 1.初始化配置

1.1 初始化导航视图和导航：

```java
    /**
     * 导航SDK 管理类。关于导航SDK的具体使用，可查看导航接入文档。
     *
     * <p>推荐使用单例模式
     */
    TencentCarNaviManager naviManager = new TencentCarNaviManager(getApplicationContext());
    /**
     * 导航视图
     */
    carNaviView = findViewById(R.id.navi_car_view);
```

1.2 司乘SDK需要配置司机id和设备标识，并关联导航和导航视图：

```java
    /**
     * 司乘SDK 管理类
     */
    TSLDExtendManager mDriverSync = TSLDExtendManager.newInstance();

    /**
     * 司乘SDK 的初始化
     *
     * 配置信息类{@code TLSConfigPreference}可参考接口文档
     */
    mDriverSync.init(getApplicationContext(), TLSConfigPreference.create()
         .setDeviceId(DeviceUtils.getImei(getApplicationContext())) // 设备id
         .setAccountId(driverId)); // 司机id

    /**
     * 司乘SDK 关联导航SDK和导航视图。
     *
     * <p>帮助用户处理导航过程中的定位点、路线的上报逻辑，
     * 关联导航视图可为用户设置途经点图标。
     */
    mDriverSync.setNaviManager(naviManager);
    mDriverSync.setCarNaviView(carNaviView);
```

## 2. 司机端主流程

### 2.1 听单状态

2.1.1 司机上线进入听单状态，需要开发者开启司乘同显：

```java
    // 需要保证当前订单状态正确，以快车听单为例：
    mDriverSync.getOrderManager().editCurrent()
            .setOrderId("-1") // 听单状态，SDK默认-1，不能为空
            .setOrderType(TLSBOrderType.TLSDOrderTypeNormal) // 快车
            .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusNone) // 初始状态
            .setDrvierStatus(TLSDDrvierStatus.TLSDDrvierStatusListening); // 听单中

    mDriverSync.start(); // 开启司乘
```

2.1.2 司机需要上报定位点，以便接单。只有听单状态需要开发者上报，导航过程中司乘同显自动上报：

```java
    // 定位点可由腾讯定位sdk获取，主要上报前要保证2.1.1当前订单状态正确
    mDriverSync.uploadPosition(TLSBPosition); // 上报定位点
```

### 2.2 接到订单，进入接驾状态

开发者服务端需要调用订单同步接口（/order/sync），将订单切换为接驾状态，将司机id也做订单同步

2.2.1 设置接到的订单

```java
    mDriverSync.getOrderManager().editCurrent()
            .setOrderId(currOrderID) // 真实订单ID
            .setOrderType(TLSBOrderType.TLSDOrderTypeNormal) // 快车
            .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusPickUp) // 接驾中
            .setDrvierStatus(TLSDDrvierStatus.TLSDDrvierStatusServing); // 服务中
```

2.2.2 路径规划+上报路线+开启导航

```java
    /**
     * 导航的起点
     * 按场景设置PoiId
     */
    NaviPoi from = new NaviPoi(40.041032,116.27245);
    /**
     * 导航的终点
     * 按场景设置PoiId
     */
    NaviPoi to = new NaviPoi(39.868699,116.32198);
    /**
     * 导航的途经点，按场景设置PoiId
     * 顺⻛⻋和拼车需要，快⻋可不添加
     */
    ArrayList<TLSDWayPointInfo> ws = new ArrayList<>();

    TLSDWayPointInfo w1 = new TLSDWayPointInfo();
    w1.setpOrderId("test_passenger_order_000011"); // 乘客1订单id
    w1.setWayPointType(TLSBWayPointType.TLSDWayPointTypeGetIn); // 乘客1的上⻋点
    w1.setLat(39.940080); // 上车点的纬度
    w1.setLng(116.355257); // 上车点的经度

    TLSDWayPointInfo w2 = new TLSDWayPointInfo();
    w2.setpOrderId("test_passenger_order_000011"); // 乘客1订单id
    w2.setWayPointType(TLSBWayPointType.TLSDWayPointTypeGetOff); // 乘客1的下⻋点
    w2.setLat(39.923890); // 下车点的纬度
    w2.setLng(116.344700); // 下车点的经度

    ws.add(w1);
    ws.add(w2);

    /**
     * 开始算路，这是司乘SDK提供的算路接口
     * 与导航SDK的算路接口无区别，只是司乘为用户封装了部分处理逻辑
     *
     * <p>在拼车场景中，因为没有导航终点，所以需要使用{@link TSLDExtendManager#searchCarRoutes
     * (NaviPoi, ArrayList, CarRouteSearchOptions, ISearchCallBack)}方法。而在顺风车场景下，存在导航终点，所以需要
     * 使用重载方法{@link TSLDExtendManager#searchCarRoutes
     * (NaviPoi, NaviPoi, ArrayList, CarRouteSearchOptions, ISearchCallBack)}
     *
     * <p>下面以顺风车场景的最优算路接口，五个参数的方法为例。
     */
    mDriverSync.searchCarRoutes(from, to, ws, CarRouteSearchOptions.create()
        , new DriDataListener.ISearchCallBack() {
            @Override
            public void onParamsInvalid(int errCode, String errMsg) {
                ToastUtils.INSTANCE().Toast("参数不合法!!");
            }

            @Override
            public void onRouteSearchFailure(int i, String s) {
                ToastUtils.INSTANCE().Toast("算路失败!!");
            }

            @Override
            public void onRouteSearchSuccess(ArrayList<RouteData> arrayList) {
                ToastUtils.INSTANCE().Toast("算路成功");
		// 上报路线，注意此时要保证2.2.1订单状态正确
		mDriverSync.uploadRouteWithIndex(curRouteIndex); // curRouteIndex 要上报的路线索引
		// 开始导航
		naviManager.startNavi(curRouteIndex);
            }
    });
```

在算路之前，用户可根据需要是否使用**最优送驾顺序接口，顺风车场景与拼车场景区别见注释**，代码示例：

```java
    /**
     * 在拼车场景中，因为没有导航终点，所以需要使用{@link TSLDExtendManager#requestBestSortedWayPoints
     * (NaviPoi, ArrayList, ISortedWayPointsCallBack)}方法。而在顺风车场景下，存在导航终点，所以需要
     * 使用重载方法{@link TSLDExtendManager#requestBestSortedWayPoints
     * (NaviPoi, NaviPoi, ArrayList, ISortedWayPointsCallBack)}
     *
     * <p>下面以顺风车场景的最优算路接口，四个参数的方法为例。
     */
    mDriverSync.requestBestSortedWayPoints(from, to, sorts, new DriDataListener.ISortedWayPointsCallBack() {
        @Override
        public void onSortedWaysSuc(ArrayList<TLSDWayPointInfo> sortedWays) {
            /**
	     * {@code sortedWays}是排序好的途经点
	     * 用户可以拿{@code sortedWays}使用 searchCarRoutes 再直接开启算路
	     *
	     * @see TLSDWayPointInfo
	     */
        }

        @Override
        public void onSortedWayFail(int errCode, String errMsg) {
            Log.e(LOG_TAG, ">>>errCode : " + errCode + ", errMsg : " + errMsg);
        }
    });
```

2.2.3 到达接驾点

```java
    // 停止导航
    naviManager.stopNavi();
```

### 2.3 接到乘客，进入送驾状态

开发者服务端需要调用订单同步接口（/order/sync），将订单切换为送驾状态

2.3.1 设置订单状态

```java
    mDriverSync.getOrderManager().editCurrent()
            .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusTrip); // 接驾中
```

2.3.2 路径规划+上报路线+开启导航，与接驾过程一样：

```java
    /**
     * 导航的起点
     * 按场景设置PoiId
     */
    NaviPoi from = new NaviPoi(40.041032,116.27245);
    /**
     * 导航的终点
     * 按场景设置PoiId
     */
    NaviPoi to = new NaviPoi(39.868699,116.32198);
    /**
     * 导航的途经点，按场景设置PoiId
     * 顺⻛⻋和拼车需要，快⻋可不添加
     */
    ArrayList<TLSDWayPointInfo> ws = new ArrayList<>();

    TLSDWayPointInfo w1 = new TLSDWayPointInfo();
    w1.setpOrderId("test_passenger_order_000011"); // 乘客1订单id
    w1.setWayPointType(TLSBWayPointType.TLSDWayPointTypeGetIn); // 乘客1的上⻋点
    w1.setLat(39.940080); // 上车点的纬度
    w1.setLng(116.355257); // 上车点的经度

    TLSDWayPointInfo w2 = new TLSDWayPointInfo();
    w2.setpOrderId("test_passenger_order_000011"); // 乘客1订单id
    w2.setWayPointType(TLSBWayPointType.TLSDWayPointTypeGetOff); // 乘客1的下⻋点
    w2.setLat(39.923890); // 下车点的纬度
    w2.setLng(116.344700); // 下车点的经度

    ws.add(w1);
    ws.add(w2);

    /**
     * 开始算路，这是司乘SDK提供的算路接口
     * 与导航SDK的算路接口无区别，只是司乘为用户封装了部分处理逻辑
     *
     * <p>在拼车场景中，因为没有导航终点，所以需要使用{@link TSLDExtendManager#searchCarRoutes
     * (NaviPoi, ArrayList, CarRouteSearchOptions, ISearchCallBack)}方法。而在顺风车场景下，存在导航终点，所以需要
     * 使用重载方法{@link TSLDExtendManager#searchCarRoutes
     * (NaviPoi, NaviPoi, ArrayList, CarRouteSearchOptions, ISearchCallBack)}
     *
     * <p>下面以顺风车场景的最优算路接口，五个参数的方法为例。
     */
    mDriverSync.searchCarRoutes(from, to, ws, CarRouteSearchOptions.create()
        , new DriDataListener.ISearchCallBack() {
            @Override
            public void onParamsInvalid(int errCode, String errMsg) {
                ToastUtils.INSTANCE().Toast("参数不合法!!");
            }

            @Override
            public void onRouteSearchFailure(int i, String s) {
                ToastUtils.INSTANCE().Toast("算路失败!!");
            }

            @Override
            public void onRouteSearchSuccess(ArrayList<RouteData> arrayList) {
                ToastUtils.INSTANCE().Toast("算路成功");
		// 上报路线，注意此时要保证2.2.1订单状态正确
		mDriverSync.uploadRouteWithIndex(curRouteIndex); // curRouteIndex 要上报的路线索引
		// 开始导航
		naviManager.startNavi(curRouteIndex);
            }
    });
```

2.3.3 到达送驾终点 开发者服务端需要调用订单同步接口（/order/sync），将订单切换为结束状态

```java
    naviManager.stopNavi(); // 结束导航

    mDriverSync.getOrderManager().editCurrent()
            .setOrderId("-1") // 听单状态，SDK默认-1，不能为空
            .setOrderType(TLSBOrderType.TLSDOrderTypeNormal) // 快车
            .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusNone) // 初始状态
            .setDrvierStatus(TLSDDrvierStatus.TLSDDrvierStatusListening); // 听单中
```

### 2.4 结束司乘同显服务

```java
    lsManager.stop();// 结束司乘
```

## 接力单

3.1 如果司机送驾过程中接到了接力单，需要在OrderManager中创建RelayOrder，然后再进行算路获取接力单路线：

```java
    mDriverSync.getOrderManager().addRelayOrder()
           .setOrderId(order.getId()) // 接力单订单id
           .setOrderStatus(TLSBOrderStatus.TLSDOrderStatusPickUp); // 送驾

    // 开始算路
    mDriverSync.searchCarRoutes(
            ConvertUtils.toNaviPoi(orderA.getEnd()), // 当前单送驾点
            ConvertUtils.toNaviPoi(orderB.getBegin()), // 接力单接驾点
            new ArrayList<>(), // 途径点
            OrderRouteSearchOptions.create(orderB.getId()), // 算路策略
            new DriDataListener.ISearchCallBack() {
                @Override
                public void onParamsInvalid(int errCode, String errMsg) {
                     // 算路参数异常
                }

                @Override
                public void onRouteSearchFailure(int i, String s) {
                     // 算路失败
                }

                @Override
                public void onRouteSearchSuccess(ArrayList<RouteData> arrayList) {
                     // 算路成功，获取接力单路线
                }
           }
    );
```

3.2 获得到接力路线后，将接力路线上传:

```java
    // 上报接力单路线和当前路线
    mDriverSync.uploadRoutes();
```

3.3 当前订单送驾结束后，清理接力单信息，并将接力单信息设置为当前订单

```java
    // 待补充
```

## 4. 乘客选路

乘客可以在送驾前和送驾中去提前选择或切换送驾路线。 司机端需开启选路功能

```java
    // 待补充
```

### 4.1 送驾前选路

那么司机端在开始送驾时，调用如下方法进行路径规划，算路结束后会在 SimpleDriDataListener 提供路线是否变更回调。如果乘客进行了行前选路，selectedRoute即为乘客行前选路的路线id，司机端可以选择该路线发起送驾导航：

```java
    // 开始算路
    TSLDExtendManager#searchCarRoutes(NaviPoi from, NaviPoi to, final List<TLSDWayPointInfo> ws,
                          final CarRouteSearchOptions searchOptions,
                          final DriDataListener.ISearchCallBack callback);

    // 在算路结束后，会有路线变更回调
    mDriverSync.addTLSDriverListener(new SimpleDriDataListener() {
            @Override
            public void onSelectedRouteWantToChangeNotify(TLSBRoute selectedRoute) {
                super.onSelectedRouteWantToChangeNotify(selectedRoute);
                mDriverPanel.print("线路发生变更：" + selectedRoute.getRouteId());
		// 使用乘客选择的路线
                mDriverSync.getRouteManager().useRouteId(selectedRoute.getRouteId());
            }

            @Override
            public void onSelectedRouteNotFoundNotify(String selectedRouteId) {
                super.onSelectedRouteNotFoundNotify(selectedRouteId);
                mDriverPanel.print("线路未找到：" + selectedRouteId);
            }
   });
```

### 4.2 送驾中选路

司乘同显自动会切换导航路线，并将信息回调给开发者

```java
    // 在行程中，会有路线变更回调
    mDriverSync.addTLSDriverListener(new SimpleDriDataListener() {
            @Override
            public void onSelectedRouteWantToChangeNotify(TLSBRoute selectedRoute) {
                super.onSelectedRouteWantToChangeNotify(selectedRoute);
                mDriverPanel.print("线路发生变更：" + selectedRoute.getRouteId());
		// 使用乘客选择的路线
                mDriverSync.getRouteManager().useRouteId(selectedRoute.getRouteId());
            }

            @Override
            public void onSelectedRouteNotFoundNotify(String selectedRouteId) {
                super.onSelectedRouteNotFoundNotify(selectedRouteId);
                mDriverPanel.print("线路未找到：" + selectedRouteId);
            }
   });
```

## 5. 司乘同显司机端回调

```java
    /**
     * 定位点上传成功
     */
    void onPushPositionSuc();
    /**
     * 定位点上传失败
     */
    void onPushPositionFail(int errCode, String errMsg);
    /**
     * 拉取乘客端轨迹点成功
     */
    void onPullLsInfoSuc(ArrayList<TLSBPosition> los);
    /**
     * 拉取乘客端轨迹点失败
     */
    void onPullLsInfoFail(int errCode, String errMsg);
    /**
     * 上传路线成功
     */
    void onPushRouteSuc();
    /**
     * 上传路线失败
     */
    void onPushRouteFail(int errCode, String errStr);
    /**
     * ⽤户⼿动剔除途经点的回调
     *
     * <p>移除途经点后，⽤户需停⽌导航->重新算路->开启导航
     */
    void onRemoveWayPoint(ArrayList<TLSBWayPoint> wayPoints);
    /**
     * 乘客选路线路发生变更回调。
     */
    void onSelectedRouteWantToChangeNotify(TLSBRoute selectedRoute);
    /**
     * 路线选择之后，在当前的路线列表中未找到
     */
    void onSelectedRouteNotFoundNotify(String selectedRouteId);
```