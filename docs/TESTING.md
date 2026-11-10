> 2026-11-10 最终：71 项 JUnit、40 项原生专项、smoke PASS 66、8 个媒体及覆盖审计通过。完整口径和证据见 [SCENARIO_COVERAGE.md](SCENARIO_COVERAGE.md)。下方 22 项/68 测试为前次历史。

# 验证与回归（独立 TACZ 形态）

## 前次交付历史（2026-11-09）

2026-11-09：**68 项 JUnit、22 个原生专项、完整 smoke `PASS 66`**，发布 JAR 审计通过。完整脚本发现空影片弹丸边界后进行了修复；最终代码又完整运行 smoke，并重测弹道与两条五枪 F4/F6 导出链路，所有关联复测及实际 MP4 解码通过。具体日志、媒体数据及验证边界见 [专项交付记录](REPLAY_EXPORT_REVIEW.md)。

## 完整串行复测入口

开发环境使用 Java 21 启动 Gradle，Minecraft/LWJGL 使用 Java 17；需要真实显示环境与 `/usr/bin/ffmpeg`、`/usr/bin/ffprobe`。固定发布包为 BBS 2.7、TACZ Refabricated 1.0.0-forge1.2.0+mc1.20.1、PA 1.0.2-rc1+1.20（TACZ 内嵌 SBM 2.6.2）。

```bash
scripts/verify-replays.sh
```

脚本先干净构建，再**串行**运行 `scripts/native-review-scenarios.json` 中的原生专项、完整 smoke、实际 MP4 解码和覆盖账本审计。锁定同一宿主上的测试脚本，并拒绝在已有开发 Minecraft 运行时重编译/启动第二个客户端。日志写入 `run/native-review-logs/<时间>/`，不会被 `clean` 或 `/tmp` 清理删除。单项入口：

```bash
./gradlew runClient -Preview=场景类名
./gradlew runClient -Psmoke
python3 scripts/audit-replay-media.py
```

报告缺失或非 `PASS` 会使 Gradle 失败。`verifyReview -Preview=场景类名` 仅检查已有报告，**不等于重新运行测试**。本轮最终数据、根因及提交范围见 [回放导出专项审查](REPLAY_EXPORT_REVIEW.md)；形态/皮肤原始复现见 [历史审查记录](REVIEW_FIXES.md)。

## 原生专项覆盖

| 场景类名 | 重点 |
| --- | --- |
| `ThirdPersonArmsReview` | 实际 ModelPart 绘制时的左右臂；未 tick / 空身体快照 |
| `FirstPersonSkinReview` | 有/无待刷新外部材质、手臂与袖层、重载与 GL 所有权 |
| `PlaybackAudioReview` | 原生声音序列化、不产生 AudioClip；OpenAL play/pause/seek/resume/stop；同 tick 暂停保留 voice |
| `AudioRetakeReview` | 禁止监听旧 take，仅覆盖自身区间并保留头/尾、用户及其他 replay 音轨 |
| `NativeSoundExportReview` | 固定 BBS 发布包的 TACZ OGG 路径与 48 kHz 原生混音对照 |
| `NativeInterruptionExportReview` | 原生 one-shot 停止帧；被打断换弹声音的停止后 PCM 为零 |
| `HiddenPreviewReview` | 隐藏标签不接管世界摄像机，可见暂停预览正常 |
| `CrawlOwnershipReview` | 无关 controller 关闭不站起；多 owner 与集成客户端/服务端身份隔离 |
| `F4ClockReview` | F4 固定视频时间 / held frames；世界与原生枪械时钟一致 |
| `F4BurstClockReview` | 160ms 慢帧三连发；66ms 原生节奏不坍缩，服务器实际射击成功 |
| `CtrlStartupReview` | 客户端首帧与服务端动作的 Ctrl 启动握手 |
| `ActionTailDurationReview` | 姿态之后的 TACZ 动作计入时长；序列化 / 禁用动作 |
| `ActionLifecycleReview` | 原生 tick 中断线/停止回调重入；停止期间创建替代播放；原生借用装备归还 |
| `HotkeyWorkflowReview` | 默认右 Alt→右 Ctrl→F6 链路；停录后不手动调用测试专用 apply/save |
| `F6ExportReview` | F6 首帧 tick 0 / 完整时长 / 双端预热暂停 / FFmpeg 导出 |
| `F6AudioExportReview` | 所选皮肤像素、H.264、AAC 非零解码与原生声音后混音 |
| `F6NoDelayExportReview` | 视频延迟 0 的异步启动 |
| `F6DraftExportReview` | 服务端旧片 fp=false，只修改当前编辑器 fp=true 而不手动保存，仍正确导出 |
| `F6CancelReview` | 回复前取消、立即重试、播放后取消、首帧读取失败，无 client/server 残留 |
| `ProjectileReplayReview` | 四组 yaw/pitch 实射；Alt/Ctrl/F6 数量一致、动作游标、seek 清理、实时输入隔离、每帧弹道唯一及朝向 |
| `MultiGunWorkflowReview` | AK47/M4A1/Glock17/AA12/RPG7，多状态打断与改装组合；Alt/Ctrl/F6、原生声音及各阶段弹道绘制 |
| `F4MultiGunExportReview` | 同一五枪组合在实时 F4 中实际射击；服务器成功、固定时钟、原生音源、捕获帧内弹道唯一 |

`ReviewHealthFailureReview` 是**故意失败的负对照**，不在正常 22 项串行列表中：它写入标明为测试注入的崩溃标记。测试入口及 Gradle 必须拒绝结果，不能因为先前写了 PASS 就忽略退出阶段的新崩溃。普通专项也会在提前断线时失败，并在退出测试世界后确认结果。

## 自动化测试覆盖什么

### `./gradlew test`

- 版本化 BBS 数据、不可变数组、随机值与 UV 的回读。
- 枪械/相机变换、跨 ±π 与四元数符号、奇异缩放、矩阵正交性。
- 未来击发不提前出现；枪械切换不冻结弹壳年龄；保存发射枪；末尾帧之后的临时效果老化。
- PA body bend、物品变换、光照和 world/model FOV 字段的回读。
- 对安装发布包检查所有注册 Inject/Redirect/ModifyArg/Accessor/Invoker 的目标、回调描述符和重定向调用点。
- 独立形态直接继承 `Form`、身份/必需 PA 版本。
- `v1.0.0` 版本锁、指定图标的原字节 SHA-256。
- 8 项 PCM 重采样/声道/亚 tick/停止/溢出、3 项弱对象身份键、4 项 F6 回复/取消、3 项首帧门控检查。
- 3 项导出时钟边界/held frames、3 项时钟弹簧积分、4 项任务周期/取消/异常/清理检查。
- 3 项原生声音 JSON 参数/停止/组合/坏数据、3 项瞬时动作回拨隔离、5 项声音暂停/定位游标检查。
- 6 项动作注册表的回调重入、并发清理、条目身份、迭代器删除和列表基本操作检查；无影片上下文弹丸生命周期回归。

### `./gradlew runClient -Psmoke`

测试只新建隔离的平坦开发世界。集成**服务端线程**检查真实弹丸/榴弹、新 owner/方向、伤害/引信回读、重力 tick，以及实际 Actor 的干地趴下、碰撞箱、眼高、跨 tick 保持与恢复 swimming 状态。

**客户端/实际 GPU**检查：

- 形态工厂、序列化/复制、宽/细模型、完整九格装备同步。
- 原生骨骼矩阵、身体绘制与共享 player/gun 状态恢复。
- 原生 PA 实时 Actor 会话，影片绑定后停用第二个动画时钟。
- 世界 Actor 的热栏/腰背武器；不只复制实际 Actor 的六格装备。
- shell 出生时刻稳定；与实际持枪 GPU 路径的抛壳挂点矩阵比较。
- 换枪/收枪/剔除后的 shell 寿命；历史跨 Actor 骨骼锚点；暂停时电影时钟不推进。
- 原生 FP 录制的检视、改装进入/关闭、配件视角、换弹、近战、击发触发和枪火采样。
- F5 正/背面镜像的 native 粒子快照和 codec；镜像烟雾不进入实际 particle engine。
- 回放时主手为空、当前 morph 为空，仍从影片形态画枪和中立手臂。
- 暂停 GPU 回放和原生 FOV 修改器之后的保存值优先级。
- 整数 tick 的 body-only 保底帧不得插入已求值 FP 区间（含录制/绘制时钟不同步）。
- 可检查的原生身体/装备/爬行 PNG，写入 `run/bbs-tacz-*.png`。

开发测试入口、测试探针 Mixin 和截图工具**不进入发布 JAR**。由于测试客户端可能被窗口限帧，GPU 检查等待实际绘制，不能把固定数量 client tick 当成固定数量 render frame。

### `./gradlew auditJar` / `build`

检查 McAxolotl / BBS TACZ / 包和版本、新形态与必需后端；检查每个 Mixin class 存在；拒绝旧兼容 backend、上游/嵌入依赖或 smoke 代码。

### `python3 scripts/audit-replay-media.py`

默认读取本次 F4/F6 五枪组合和 F6 所选皮肤/音频的 `*-video.txt`，完整解码视频并检查 H.264/AAC、30 FPS、帧数/时长、音视频时长差与非零 PCM 音轨。**仅有 AAC 头部不能证明存在声音**。音源身份由原生专项检查 `tacz_sounds/` 资源及没有新增 AudioClip；媒体脚本不以音频波形反推来源。

## 必须区分：数据/矩阵/绘制成功 ≠ 所有场景逐像素验收

以下表是进一步的人工/影片导出验收矩阵，**不是全部已经通过的名单**。对每一项固定同一枪包、枪械、配件和手臂宽度，对比实时游玩与录制；检查正常播放、暂停、回拖/前拖和从中途开始。

| 场景 | 比较重点 |
| --- | --- |
| 持枪、ADS、行走、疾跑、转向 | 原生 PA 混合与物理延滞，不按回放真实时钟重算 |
| 干地爬下、起身、趴下移动 | 视觉、真实碰撞箱与眼高；body bend 与腕部挂载 |
| 腰背与副手枪 | 九个槽位、排除选中槽、枪包原生显示规则、弯曲层 |
| 单发、自动、Burst、霰弹 | 每个实际弹丸；无重播按键生成额外弹丸；枪火时机 |
| 榴弹与火箭 | 新方向、新 owner、原生碰撞/引信/爆炸，而非只画粒子 |
| 战术/空仓换弹 | 弹匣/枪机可见性和插值 |
| 改装进入/退出与配件切换 | 过渡、手部隐藏、每类配件视角、相邻状态连续性 |
| 检视→改装→往返切配件→反复开关界面 | 同时刻顺序、动画轨道与退出状态 |
| 各种动画中近战/开火打断 | 换弹、检视、改装、瞄准分别被打断；事件年龄不提前 |
| FP default / renew 枪火 | 贴图旋转，粒子位置/UV/透明度，世界与 hand FOV 比例 |
| 多人同枪的 TP 抛壳 | 出生原点、随机速度、暂停/中途跳转，人物剔除/换枪 |
| 30/60/更高帧率的导出 | 求值采样频率、暂停不老化、重复绘制不触发 gameplay |
| shader / Sodium / AR 等组合 | 单独验证 stencil、透明、VBO/手部 pass；无光影成功不代替光影验收 |
| TACZ 声音与重录 | 开火/换弹/检视/配件与打断；原生资源命令、导出音频开关、旧 WAV 兼容、仅覆盖本 replay 区间 |
| F6 取消与多影片 | 等待回复时取消、零视频延迟、关闭无关影片、重新启动，不覆盖其他影片 |
| 结束/断线/切世界 | 恢复 pose/swimming、共享模型、PA 层、muzzle 与 FOV cache |

## 已知验证边界

- 非完整 Lua VM 恢复；使用保存的求值姿态与特效，而非重新运行输入。
- F5 是 tick/击发时刻的 FP 镜像，不声称与所有高 FPS 子帧、所有枪包逐像素相同。
- 单位/GPU/集成服务端检查不能代替多人独立服务器、所有复杂打断组合和各光影的视觉比较。
- 真实服务器弹丸不会随客户端编辑器暂停/倒退，重播真实 action 应在受控世界进行。
- 只验证独立 TACZ 玩家形态，不再列 ModelForm、MobForm、BOBJ 或未知自定义骨架作为支持后端。
