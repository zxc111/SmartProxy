SmartProxy
==========

http2 proxy client for android
服务器配合[nghttpx](https://github.com/nghttp2/nghttp2)食用。（自行编译）

简单粗暴将nghttpx打包进apk中

#### 已知问题
~~1. 要按保存+on才能开始运行。保存开启nghttpx转发，on开启vpnService截取包。~~
2. 性能略差。100k/s吧大概
3. 切换配置麻烦
4. 远端服务器暂时只支持ip
5. 有点费电，暂时无解。
6. 目前放进去的是nghttpx是编译的arm平台的，不能跑在x86下，也就是说emulator下无法真正的测试通信
7. 文件编码有些问题
8. 先这么多吧
