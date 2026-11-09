# BBS TACZ

Minecraft **1.20.1 / Fabric** 的 **BBS FS × TACZ-Refabricated** 附属模组。

- 名称：**BBS TACZ**
- 作者：**McAxolotl**
- Java 包：`mcaxolotl.bbstacz`
- 模组 ID：`bbs_tacz`
- 独立形态 ID：**`bbs_tacz:player`**
- 版本：**v1.0.0**

## 现在的方案：只使用独立 TACZ 形态

根据最新需求，本版的 `TaczForm` **直接继承 BBS 的 `Form`**，不是 `ModelForm` 或 `MobForm` 的子类，也不再为这两种形态安装兼容层。

形态提供原生玩家骨架、皮肤、宽/细手臂、作者姿态、骨骼轨道和 body parts。渲染器使用未加入世界的玩家代理，经原版 `PlayerRenderer`、TACZ 装备层和 Player Animator 绘制；不维护另一套近似的持枪/爬行动作。BBS 的通用骨架、姿态和矩阵工具仍会复用，这不等于依赖 MobForm。

**这是玩家骨架形态，不是任意自定义模型的导入器。** 原来的 ModelForm/MobForm 不会自动转换；已有影片请把对应 replay 的形态改为 BBS TACZ 形态。已有 `frame_v1` 录制通道仍能读取。

## 安装

本版开发与回归所用版本：

| 前置 | 验证版本 |
| --- | --- |
| Minecraft | 1.20.1 |
| Fabric Loader | 0.17.2 |
| Fabric API | 0.92.6+1.20.1 |
| BBS FS | 2.7-1.20.1，Modrinth `tD7xHigj` |
| TACZ-Refabricated | 1.0.0-forge1.2.0+mc1.20.1，Modrinth `wbgtQwxp` |
| Forge Config API Port | 8.0.3，Fabric 1.20.1 版 |
| Player Animator | **1.0.2-rc1+1.20，必需** |

安装 `bbs-tacz-v1.0.0.jar`。`forge1.2.0` 是 TACZ 发布版本名的一部分，这里用的是 **Fabric 移植版**，不能替换成 Forge TACZ。

本 JAR 不内嵌 BBS、TACZ、Player Animator 或枪包。真实弹丸和爬行回放需要服务端也安装 BBS、TACZ、本附属模组及其前置。不同/更新版本的兼容性不等同于已验证版本。

## 使用

1. 在 BBS 形态列表的 **BBS TACZ** 分类中选择 **TACZ Player**。
2. 在形态编辑器中设置皮肤纹理和 `slim`，选择与皮肤相符的手臂宽度。
3. 用实际玩家持有 TACZ 枪械，按 BBS 的正常方式录制 take。
4. 第一人称 replay 开启 BBS 的第一人称选项；需要真实射击时使用 BBS 服务端 action/actor 回放，而不是仅观看客户端编辑器预览。
5. 骨骼名称采用原生 rig 的 `body`、`head`、`right_arm`、`left_arm` 等，按编辑器显示选择。录制数据内部的 `torso` 只是状态字段，不是另一种模型骨架。

主手、副手及九个热栏槽位进入玩家代理。服务端 Actor 自身没有九格玩家背包时，渲染器从 replay 热栏通道读取装备，不会只显示主手。腰/背武器遵循枪包的 `offhand_show`、`hotbar_show` 和 TACZ 自身的开关；关闭原版玩家腰背枪的 TACZ 选项，同样会关闭此形态的腰背枪。

## 实现范围

| 能力 | 实现方式 |
| --- | --- |
| 持枪、ADS、移动、疾跑、爬行、换弹/近战等第三人称姿态 | 原生玩家求值姿态；完整保存身体/肢体 bend、整体变换及物品变换 |
| 腰部、背部和副手武器 | 原生 TACZ 装备层；不把原版位置直接套到未知自定义骨骼上 |
| 第一人称开火、换弹、检视、改装、配件视角切换、打断和物理晃动 | 原生求值后的枪械骨骼、最终矩阵、手部摄像机、光照/FOV，加有序命令日志 |
| 枪火贴图、Snowstorm 粒子、弹壳 | 保存发射年龄、随机旋转/速度、粒子位置/UV/透明度；回放不重新抽随机数 |
| 所选第一人称皮肤 | 原生纹理别名绑定；手臂、袖层、宽/细模型使用影片形态所选皮肤，不依赖刷新顺序 |
| TACZ 音效 | 保存实际资源、音高/音量与停止时间，烘焙普通 BBS 音频轨道，支持暂停/拖动/重录和 F6 音轨导出 |
| F6 播放并录制 | 清理同 ID 的编辑器预览、等待服务端回复；零延迟也等待实际播放和 tick 0 首帧读取；服务端初始化暂停，不通过倒退动作补帧；取消/重试不会复活旧请求 |
| 暂停与跳转 | 根据影片时间采样；不依赖真实时间推进 Lua/粒子模拟 |
| F5 录制时补充 FP 数据 | 离屏求值和独立 FP 粒子系统；不会把镜像烟雾再次投递到实际世界 |
| 第三人称抛壳 | 根据出生时刻还原历史人物、作者姿态、锚点、原生物品及抛壳骨骼矩阵；换枪或人物被剔除后仍更新寿命 |
| 实际弹丸/榴弹 | 逐个捕获成功生成的原生 `EntityKineticBullet`，服务端恢复 owner、方向、伤害、重力和爆炸参数 |
| 实际干地爬下 | 操作实际演员的强制姿态，限制爬行碰撞箱/眼高，停止时恢复借用状态 |

第一人称回放不要求观看者当前拿着枪，也不要求观看者当前变成 TACZ 形态；枪械手臂由 replay 的 TACZ 形态渲染。它不会为了播放画面重新打开改装 GUI、重发输入事件或生成第二批弹丸。

## 数据与边界

- `bbs_tacz:state` 是注册的 BBS `bbs_tacz:frame_v1` 通道；`bbs_tacz:commands` 保存同一时刻的有序 JSON 指令批次。数据可保存/读取/同步，不使用 Java 对象序列化。
- `frame_v1` 是**求值后视觉快照**，不是完整 Lua VM 存档。命令日志保存触发与轨道操作，但画面以快照为准，避免状态机重算造成改装/打断差异。
- 真正的 FP 绘制默认最多记录 60 次/秒，可在开发/游戏配置目录的 `bbs_tacz.json` 修改 `recordingFps`（20–240）。实际采样不会超过游戏真实绘制帧率；F5 镜像以客户端 tick 和击发采样为主，不等价于每个高帧率 FP 渲染子帧。
- TACZ 音轨遵循 BBS 外部资产约定，位于 `assets/audio/bbs_tacz/`；分享影片时一并分享该目录。旧影片未保存的声音无法精确推断原音高/停止时刻，应重新录制该轨道。F6 导出音轨需启用 BBS 视频“导出音频”设置；首帧同步功能需客户端/服务端安装同一 BBS TACZ 构建。
- 回放需要对应枪包和粒子定义；保存影片不会把模型、皮肤纹理或枪包复制进影片/JAR。
- 客户端暂停/拖动不会反复生成弹丸；**已进入服务端世界的真实弹丸遵循 Minecraft 物理时钟**，不会因客户端编辑器暂停自动冻结/倒退。
- 自动化测试验证原生接口、数据、矩阵、实际绘制和服务器物理，**并不意味着用户列举的每个复杂动作组合、每个枪包和每种光影已逐像素验收**。完整验收清单见 `docs/TESTING.md`。

## 构建与测试

Gradle 使用 Java 21；开发 Minecraft 进程使用 Java 17。

```bash
./gradlew build
./gradlew runClient -Psmoke
./gradlew verifySmoke
./gradlew runClient -Preview=F6AudioExportReview
./gradlew verifyReview -Preview=F6AudioExportReview
```

**本轮验证：40 项单元测试通过，完整原生 smoke 为 `PASS 66`。专项原生复现/修复与 F6 音视频验证见 `docs/REVIEW_FIXES.md`。**

构建产物位于 `build/libs/`，包含发布 JAR 和源码 JAR。`auditJar` 检查身份、Mixin、独立形态及不内嵌依赖/测试代码。

原生 smoke 使用新建的隔离平坦世界，结果写入 `run/bbs-tacz-smoke.txt`。测试用客户端入口、探针 Mixin 和截图代码不进入发布 JAR；不会读取已有存档，也不上传测试影片。

- 架构：`docs/ARCHITECTURE.md`
- 独立形态设计说明：`docs/NATIVE_PLAYER_BACKEND.md`
- 验证记录与未验收项：`docs/TESTING.md`
- 本轮逐项复现、根因、修复与验证：`docs/REVIEW_FIXES.md`

本项目使用 MIT 许可证。前置模组和资源包遵循各自的许可证，见 `THIRD_PARTY_NOTICES.md`。
