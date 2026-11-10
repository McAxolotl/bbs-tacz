# 回放声音、第三人称动作与动态场景覆盖（v1.0.0）

本轮日期：2026-11-10。仅支持独立 **TACZ 形态**；不要求 ModelForm 或 MobForm。

## 验证口径

“所有场景”必须有可核对的范围。本项目按**当前加载枪包的全部枪械、声明的开火模式与动画资源**枚举，同时测试移动、原生输入、序列化、回放、GPU 绘制、音频和视频导出。

这不是“任意第三方枪包 × 任意光影 × 任意输入排列均已验证”的承诺。下面严格区分：

- **受控原生求值**：使用 TACZ/PA/SBM 的真实模型、动画、编解码与 GPU，但直接指定动画帧；不能证明某个游戏输入组合合法。
- **原生游戏流程**：真实 Minecraft 客户端及集成服务器、键位状态或 TACZ 公开操纵接口、原生接受事件；不绕过状态锁。
- **不适用（N/A）**：加载的数据没有声明该能力，例如没有默认近战、蓄力或过热；不能算成播放失败，也不能伪造为已播放。
- **未验证**：额外光影、其他枪包、独立多人服务器，以及所有组合的笛卡尔积。

**焦点前置**：自动化原生专项在独立生成的测试世界内，用测试专用适配器固定桌面焦点／鼠标捕获条件，以免用户切回日志窗口造成假失败。GUI、加载界面和 TACZ 的游戏状态检查不变。该适配器不进入发布 JAR；这些是原生集成测试，而不是物理键盘／桌面焦点的端到端验收。

可执行清单是 `scripts/native-review-scenarios.json`；`scripts/verify-replays.sh` 串行运行，不并行启动游戏。所有开发探针均排除在发布 JAR 外。仅定位单枪可用 `./gradlew runClient -Preview=DefaultGunGameplayReview -PreviewGun=tacz:m320`；它写独立的 filtered 报告，不计为全枪包覆盖。

## 本轮根因与修复

| 问题 | 修复前证据／根因 | 改动与验证入口 |
| --- | --- | --- |
| 编辑器及非 FP 播放无枪声 | BBS `StubEntity` 在移动，但旧音频只接受 `MCEntity` 或本地 FP 玩家；编辑器不经过旧 World-only hook | 共用 `BaseFilmController.startRenderFrame`，编辑器跟随可见 runner；虚拟演员使用只渲染、不加入世界的原生玩家声源。`EditorNativeAudioReview`、`CtrlThirdPersonAudioReview` |
| 视角切换、删轨、禁用后声音失效／残留 | 声音列表只缓存一次，删除整个通道被 `continue` 跳过；已启动集合阻止恢复 | 通道修订检测、源对象／FP 模式切换重建、删轨和关闭编辑器清理。`EditorAudioLifecycleReview`、`EditorAudioModesReview` |
| 导演镜头和玩家分离时衰减错误 | TACZ 原构造器用本地玩家而非影片镜头的位置计算初始音量 | 仅回放空间声音改用实际 MainCamera，保留原生衰减公式。`MovingAudioSpatialReview`、`DetachedCameraAudioReview` |
| 第三人称换弹／近战被旧动作吞掉 | 实际行走时近战事件被接受，ONCE_UPPER 对象却仍是旧换弹；仅看“手臂有变化”会误判 | 仅 TACZ 形态／录制对象允许已接受事件替换旧 one-shot；保留原生 fade，记录所选 TP 动画，回放仍用烘焙姿态。`MovingThirdPersonRecordReview` |
| 非 FP Ctrl/F6 启动不完整 | 原启动握手限于 FP TACZ；F6 取消整个原生启动回调还跳过帧缓冲尺寸重应用 | 扩展为 enabled TACZ replay，保留保存／权限／首帧／取消逻辑；只延迟恢复，不取消其余原生回调。`MovingThirdPersonWorkflowReview`、`RecordedThirdPersonF6Review` |
| 快速取消／重试偶发丢失影片 | 原生包线程正在覆盖 gzip，服务端启动读取同文件抛 EOFException，取消回复队列缺一条回复 | 同一 FilmManager 的完整原生 load/save 串行化，保留权限、备份及存储格式。`F6CancelReview`、`FilmStorageRaceReview` |
| 视频读回污染渲染状态 | 原生读回前纹理绑定 1，读回后遗留捕获纹理 52，绕过 Minecraft GL 缓存 | finally 恢复纹理单元/绑定/PBO/pack alignment。`VideoReadbackStateReview` 修复后 1→1；实际输出另检查演员取景及像素 |
| 滑翔姿态及转向不等价 | 真实滑翔原版已 24 tick，可视玩家仍为 0；视线及上一 tick 速度与身体局部坐标不一致 | 现有 motion 元数据保存飞行阶段，复制原生视线／速度插值并转到身体局部坐标。`LiveEnvironmentPoseReview`、`MotionMetadataTest` |

**声音来源不变**：影片保存 TACZ 资源名、参数和停止事件；播放通过原生 `SoundPlayManager`。不生成枪声 `AudioClip`，不把枪声提取成影片素材。F4/F6 的临时混音文件是 BBS 视频封装过程的中间文件，不是影片音频剪辑。

## 覆盖矩阵

| 层级 | 场景入口 | 实际检查 |
| --- | --- | --- |
| 全枪包 TP 资源 | `DefaultGunMotionCatalogReview` | 每把加载枪械 × 所有声明 PA clip × stand/walk/sprint/crouch_walk/crawl_move/airborne/ride/swim；0/12/24 帧；移动演员、codec、真实 GPU、双臂六轴 |
| 全枪包 FP 资源 | `DefaultGunFirstPersonCatalogReview` | 每把枪的所有原生 animation prototype；0/25/50/90% 采样，骨骼回读及原生／恢复 GPU 可见性 |
| 全声明开火模式 | `DefaultGunGameplayReview` | 枚举原生 gun/mode；移动时使用 ShootKey；客户端／服务器接受，连发、已声明蓄力／过热、原生近战、配件视角请求；每个 take 的 FP 状态和原生声音回读 |
| Alt→Ctrl→F6（FP） | `MovingGunWorkflowReview` | 6 个约 10 秒阶段：站立、行走、奔跑转瞄准、潜行移动、爬行移动、跳跃；每阶段真实开火、换弹、近战，检视／改装／多配件切换；每阶段实际绘制、声音、首帧和时钟 |
| Alt→Ctrl→F6（TP） | `MovingThirdPersonWorkflowReview` | 同一动态矩阵，F5 录制且 replay.fp=false；Ctrl 不依赖 FP 模式；F6 跟随镜头与实际双臂六轴比较 |
| 实时 F4 | `MovingF4WorkflowReview` | 同一动态矩阵，原生服务器射击、世界／枪械同一时间、实际视频和无重复弹道 |
| 旧录制重新打开 | `RecordedThirdPersonF6Review` | 从真实 Alt 录制文件读取全部 6 阶段，重新同步并 F6；不是合成帧替代；完整视频、原声和时钟 |
| 扩展环境 | `LiveEnvironmentPoseReview` | 后退、转向侧移、攀梯、游泳、乘船、转向鞘翅滑翔、创造飞行；真实键位／物理、原生射击／换弹／近战、codec／GPU。滑翔在 0/0.5/1 子帧比较完整根矩阵 |
| 编辑器多模式 | `EditorAudioModesReview` | FP/TP POV、FP 配置、replay.fp 开关、通道清空／恢复、多个演员独立声源、删除演员及关闭编辑器 |
| 声音生命周期 | `PlaybackAudioReview`、`AudioRetakeReview`、`NativeInterruptionExportReview` | OpenAL 播放／暂停／恢复／seek／停止、重录隔离、打断后 PCM 清零、旧音轨兼容 |
| 弹丸／榴弹／抛壳 | `ProjectileReplayReview`、完整 smoke | 多 yaw/pitch、真实出生／方向／重力／爆炸参数、回拖不重发、同帧去重、历史枪械与抛壳锚点 |
| 状态组合及 F4/F6 | `MultiGunWorkflowReview`、`F4MultiGunExportReview` | 五类枪械与多状态组合；部分低层 trigger 是受控状态机测试，不冒充每项均为原生 gameplay 接受 |
| 外观／装备 | `FirstPersonSkinReview`、`F6AudioExportReview`、完整 smoke | 所选宽／细手臂和袖层、编码后皮肤像素、九槽装备及原生腰背层、状态恢复 |
| 异常／边界 | `F6CancelReview`、`F6DraftExportReview`、`F6NoDelayExportReview` 等 | 取消重试、草稿同步、零延迟、断线、末尾动作、停止回调重入、版本和图标 |

## 原生限制与测试修正

- 原生 TACZ 的 `canPlayAnimation` 会拒绝游泳、攀爬、滑翔与睡眠的持枪 PA。环境测试验证这些状态的原生回退；资源目录测试强制求值某个 clip 不能覆盖这条 gameplay 规则。
- 过热边界测试以已预热到阈值附近的枪械为夹具，必须由新的真实原生击发跨过阈值；不把预设热量称为自然升温。冷枪持续射击曾观察到 261 次原生击发触发过热，但此长时间压力样本不代替全部模式的边界测试。
- 无默认近战数据的枪械，空配件配置下不会发出默认近战事件；应记 N/A，不给它强塞近战事件。
- 原生射击可能因奔跑、换弹、切枪、近战冷却被拒绝。动态流程通过 ShootKey 前端协调奔跑停止，记录返回值；不能把固定 tick 直接调用失败当成附属模组错误。
- 录制回调第一次动作可能从 `time=1` 开始，阶段初始化不能只检查 `step==0`。已改为检测阶段身份，且必须观察真实移动／跳跃及接受事件。
- BBS 影片镜头 yaw 与 Minecraft 玩家 yaw 相差 180°；跟随镜头夹具已修正，并增加实际相机内取景断言。绘制次数和成功编码都不能替代成片可见性。
- 五枪组合的实时录制可能没有采到短寿命弹道的可见帧；以实际弹丸动作录入证明录制，并保留可见性计数，不把 0 次画面样本称为已验证。四方向弹道专项独立要求正的实时绘制暴露，组合回放/导出仍要求实际弹道绘制与重复数为 0。
- GPU 验证等待实际绘制，不用 client tick 数代替 render frame 数。目录测试为受控采样，不冒充真实输入全排列。
- TACZ 原包中不存在、原生也无法播放的声音资源不会被凭空合成。电影声音按相应原生资源与停止事件播放。

## 手动复核步骤

1. 使用本轮构建、固定 BBS/TACZ/PA 依赖和同一枪包，在隔离世界创建 TACZ 形态并选择明显不同的皮肤。
2. 依次进行各运动状态和动作组合，右 Alt 开始／结束录制。关闭第一人称标志后右 Ctrl 播放；再切回 FP 播放。
3. 在影片编辑器播放，切换各 POV、暂停／恢复／拖动，禁用再启用演员，修改／清空声音通道，添加或删除另一位演员。
4. 原生不允许的操作先记录拒绝原因，等待合法状态再操作；检查合法的换弹／近战确实替换旧 TP 动作。
5. 开启 BBS“导出音频”和“导出 Minecraft 声音”，分别 F4 实时录制及 F6 导出整片。检查时间一致、枪火／抛壳／弹道、皮肤和枪声来源。
6. 用 `scripts/audit-replay-media.py` 完整解码实际 MP4；不能只检查文件存在。多人与光影需另建独立验收记录。

## 本轮最终结果（2026-11-10）

- **71 项 JUnit**，0 failures / errors / skipped；Mixin 目标/签名、版本、图标和 JAR 内容检查通过。
- **40 个原生专项各自最终 PASS**，完整 smoke **`PASS 66`**。回归过程中发现失败后修复并重跑相关项，不声称初次完整脚本一遍通过。
- 当前默认枪包 **70 枪、110 个声明 gun/mode 组合**：164 次客户端/服务端使用、214 次原生击发、677 条原声事件；3 个蓄力案例、2 个过热模式边界。
- 受控原生目录：**13,248 个 TP 测试项、2,350 个 FP prototype**；TP 双臂六轴最大误差 **0**。
- 真实环境检查包含后退、转向侧移、攀梯、游泳、乘船、转向滑翔、创造飞行。六阶段 FP/TP Alt→Ctrl→F6 及实时 F4 均完成；不只验证站立。
- **8 个实际 H.264/AAC 视频**完整解码通过；带 `--since` 的覆盖/媒体审计通过，拒绝使用 2026-11-09 的旧结果。
- 版本保持 **`v1.0.0`**。图标与指定 `/home/mcaxolotl/桌面/icon.png` 原字节一致，SHA-256：`d8d28b0a8a2ef472cec082097fb612a79dfecece665cb8e6e41ba9e97287dd6d`。

### 实际媒体

| 原生场景 | 帧数 | FPS | 视频秒数 | 非零音频样本（16 kHz mono） | 结果 |
| --- | ---: | ---: | ---: | ---: | --- |
| `F4MultiGunExportReview` | 1245 | 30 | 41.5 | 512776 | PASS |
| `MultiGunWorkflowReview` | 1246 | 30 | 41.533333 | 508109 | PASS |
| `F6AudioExportReview` | 67 | 30 | 2.233333 | 30273 | PASS |
| `MovingGunWorkflowReview` | 1891 | 30 | 63.033333 | 808207 | PASS |
| `MovingF4WorkflowReview` | 1890 | 30 | 63.0 | 803392 | PASS |
| `MovingThirdPersonWorkflowReview` | 1891 | 30 | 63.033333 | 138420 | PASS |
| `RecordedThirdPersonF6Review` | 1891 | 30 | 63.033333 | 154120 | PASS |
| `RecordedFirstPersonF6Review` | 1891 | 30 | 63.033333 | 837308 | PASS |

TP 视频除模型绘制与相机夹角检查外，另外检查 5/15/25/35/45/55 秒的实际人物皮肤像素。旧的错误取景视频被该审计以 0 个目标像素拒绝；修正后的 TP 成片已抽帧目视检查。FP 五枪视频也已抽帧检查。

### 证据位置与性能边界

- 最终原生结果与机器审计：`run/native-review-logs/20261110-125413/`，其中 `scenario-results.json`、`media-audit.json`、`coverage-audit.json` 为汇总。
- 本轮先复现/定位日志：`run/native-review-logs/editor-thirdperson/`、`run/native-review-logs/20261110-full/`、`run/native-review-logs/20261110-final/`。失败、超时和负对照保留，不能把这些日志中的早期 PASS 混作最终验收。
- 本机为 Linux Wayland/XWayland、NVIDIA GTX 1650。观察到 `glGetIntegerv`/`glMapBuffer` 的长等待与不稳定墙钟耗时；有过中止/超时，最终同进程 FP 六阶段流程重跑完成。GL 状态泄漏是独立复现并修复的问题，**不宣称已消除所有驱动/桌面合成器慢帧**。
- 长 FP 连续流程最后一次约 3m47s 完成，重开 FP 长片约 9m41s 完成；输出均约 63.033s。导出耗时不等于输出视频速度；以输出时钟和帧数验收。
- 默认枪包以外的资源、独立多人服务器、光影/Sodium 组合、每个环境下全部操作排列，仍不在本轮全部通过的声明中。

### 原子提交范围

| 提交 | 范围 |
| --- | --- |
| `ede2796` | feat: add native input and render evidence helpers |
| `9d1ce19` | feat: restore native audio for editor and virtual replay actors |
| `f9ddbfc` | feat: refresh native audio when replay tracks change |
| `2e36d94` | feat: preserve accepted third-person animation interruptions |
| `a6b0b5a` | feat: preserve native gliding phase and local view transforms |
| `5e114a0` | feat: synchronize third-person Ctrl and F6 playback startup |
| `8b8b264` | feat: attenuate replay gun audio from the film camera |
| `d70e73e` | feat: serialize native film storage during playback retries |
| `1867034` | feat: restore GL state after native video readback |
| `bc9d554` | feat: enumerate native animations across the loaded gun catalog |
| `55ee1eb` | feat: verify every declared stock gun firing mode |
| `f2ac3fe` | feat: validate actual replay rendering at matching sample times |
| `87a814b` | feat: verify moving gun workflows through recording and export |
| `95bad95` | feat: audit finite scenario coverage and visible video output |

本文件及 README/架构说明的最终更新另作独立文档提交。所有提交均保持 `v1.0.0`；没有推送远程仓库。
