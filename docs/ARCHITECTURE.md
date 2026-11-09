# BBS TACZ v1.0.0 架构

## 对照源码和发布包

| 项目 | 分支 | 阅读提交 |
| --- | --- | --- |
| [TACZ-Refabricated](https://github.com/Sh1roCu/TACZ-Refabricated/tree/1.20.1) | 1.20.1 | `9f1425f21a0dd7a524e771bc65e3da3dc22bad95` |
| [BBS FS](https://github.com/Wemppy4/bbs-fs/tree/1.20.1) | 1.20.1 | `998c9d585427b4cf0061940496ae1f6a7df1bb03` |

构建锁定 BBS `tD7xHigj`、TACZ `wbgtQwxp`，实际调用还对发布包字节码进行验证，不能把分支的新 API 当作已发布 API。采用 Mojang 官方映射，避免 TACZ 的 Fabric `render` 与原版 `renderByItem` 在 Yarn 下的同签名冲突。

## 视觉与世界实体分开

```
TaczForm（配置/作者姿态）
  └─ TaczFormRenderer
       ├─ TaczVisualPlayer（从不加入世界）
       ├─ NativeRenderScope（当前演员身份、求值姿态、只读/绘制标志）
       ├─ 原版 PlayerRenderer + TACZ 原生装备层
       └─ 通用 BBS MobRig / MobPoseApplier / MatrixCache

ReplayKeyframes
  ├─ bbs_tacz:state → FrameSnapshot（不可变）
  └─ bbs_tacz:commands → 有序指令批次

服务端 BBS ActionPlayer / 实际 Actor
  ├─ TaczProjectileActionClip → 新的原生弹丸
  ├─ TaczFireActionClip → 击发通知，不再次发射弹丸
  └─ PhysicalCrawl → 实际姿态、碰撞箱、眼高
```

`TaczForm extends Form`。不注册 ModelForm/MobForm 修改器，不替换其 animator/renderer。复用它们所在目录中的**通用 rig 工具**不改变这一边界。

## 第三人称求值

真实玩家从其已有 PA 状态采样，不给视觉代理开第二个玩家动画时钟。实时非玩家 Actor 通过 `ActorAnimations` 拥有单独原生 PA 会话，客户端 tick 只推进一次，开火/换弹/近战通知调用原生动画 API，不广播假事件。绑定影片后会话停用，使用保存的姿态。

普通的六个 `ModelPart` TRS 不足以还原 Player Animator：

- `anchor`：原生整体位置/旋转；
- `bend/body`：上半身、装备层的整体弯曲矩阵；
- `bend/torso`：原生 body + torso 的合成弯曲；
- 四个肢体 bend：包括持枪腕部的弯曲；
- `item/right`、`item/left`：额外物品变换；
- `motion`：原生 swim/lean 过渡。

`EvaluatedPlayerAnimation` 是帧作用域 PA 层，没有 `tick` 状态。它让原生的上半身/装备层/腕部路径消费快照的 bend 和物品变换；普通骨骼 TRS 与整体 anchor 只应用一次。矩阵读取也包含 PA 上半身 bend 和原版 crouch 渲染偏移。

作者姿态通过 BBS 在原生 `setupAnim` 之后应用；BBS 的原生 rig 提供 picking、骨骼轨道和 body parts。只读矩阵遍历不调用实体渲染、装备层、粒子或动画 tick。

`PlayerModelRenderState`、`NativeGunModels` 和各作用域的 `finally` 恢复共享模型、枪械可见性/骨骼/摄像机、PA 层与原生 muzzle 标志。代理名字牌被隐藏，由 BBS 的演员名字系统负责显示。

## FP 录制与回放

在实际枪械绘制路径采集：完整求值枪骨骼、相对原生 FP pass 的最终枪矩阵、相机四元数、显示手臂状态、ADS/改装进度、有效光照/FOV，以及随机枪火、弹壳和 Snowstorm 粒子数据。

BBS 整数 tick 的保底数据与实际 FP 绘制采样可能先后竞争；一旦存在实际 FP 求值，匹配的 body-only 保底键通过 BBS 的 channel 删除 API 清理，不能插断第一人称时间线。

状态机触发、动画轨道运行/停止/过渡、改装打开/关闭/配件视角切换同时写入 `CommandJournal`。多个操作在同一时刻按 `seq` 保持顺序。画面重放不重新运行这些输入，因而不会重新触发 GUI、声音、物理或随机数。

`RecordedHandPassMixin` 接管实际 FP hand pass，不依赖观看者当前物品；`RecordedArmMixin` 指定 replay 的 TACZ 形态，不依赖当前 morph。回放时仍保持原生手臂中立变换和 snapshot 的枪械手臂挂点。

F5 使用 `OffscreenCapture` 和 `MirrorParticles`。镜像系统隔离原生 pending/bound/base-pose/光照容器，摄像机替换为虚拟 FP 眼睛（包括正面 F5），把本应投递到世界的烟雾留在镜像系统中。新的 take、模型切换、倒退和 FP 间隔会清理旧镜像状态。

## 原生皮肤与音效

`SkinTextureBridge` 将 BBS 纹理借用为原生纹理别名；`TaczVisualPlayer` 返回形态的所选别名。原生身体/手臂/袖层按各自材质绑定，无需全局“首个刷层” hook。别名每次绑定读取 BBS 当前 GL id，重载/动画由 BBS 负责，原生纹理关闭不删除 BBS 的纹理。

`RecordedAudio` 在原生最终 SoundData 解析后捕获声音实例，并监听停止。停录时按实际音高重采样，保留亚 tick 起点和打断长度，再创建普通 BBS 音频片段。重录只替换当前 replay 所有的音频区间；录制中不监听旧枪声轨道。世界音频 context 跟随 controller/游戏暂停，BBS 自己管理 OpenAL 的 seek/resume/shutdown。

生成 WAV 遵循 BBS 外部资产约定，不随模组分发或隐式复制进影片。存在录制音轨时，仅抑制重复的原生声音；真实射击/弹丸通知仍保留。

## F6 与控制器生命周期

世界播放优先于编辑器预览；隐藏或过期的影片标签不接管第一人称摄像机，可见预览使用 runner 时钟。F6 清理同影片的编辑器/旧播放，在权限校验后同一服务端任务中创建并暂停原生 NORMAL ActionPlayer；客户端构造事件也保持 tick 0，首次实际视频 readback 后才恢复双端。0 延迟也走准备阶段，不使用 goTo 倒退/重发动作补帧。`F6Starts` 跟踪原生按序到达的回复，取消旧请求不会丢弃后续重试或使晚到回复复活播放。

## 第三人称弹壳

`CapturedShells` 捕获原生 Lua 的真实 pop-shell 时刻、随机速度、origin 和发射枪的 SNBT，不随当前热栏切换丢失。回放弹壳更新由世界 pass 驱动，不依赖当前 gun draw 或人物是否被剔除。

`ShellHistory` 用 `sampleTick - ageMillis/50` 还原出生时刻。`HistoricalScene` 在隔离 stub 上求值各 replay 的历史属性、锚点和目标轨道；再通过原版玩家设置、PA body/wrist bend、物品变换、TACZ 手持定位与 shell 骨骼求出世界矩阵。缓存的是隔离求值对象，不是当前手腕的错误出生位置。

真实绘制路径的测试探针用于比较同一姿态下的 native shell mount 与历史求值矩阵，探针不发布。`FrameSampler` 使最后一帧之后的姿态保持与临时效果老化分离，不让枪火/弹壳永久停在最后一个 key。

## 真实弹道与爬行

记录器监听成功的原生 `EntityKineticBullet` 加入服务器世界，一个实际弹丸对应一个 action，而不是一个按键对应一批推测弹丸。原生 spawn 数据之外补保存伤害曲线/倍率、穿甲、爆头、击退、点燃、引信、爆炸与方块破坏参数。

回放为新实体、新 owner 和新起点，并按演员新朝向旋转发射偏移和速度；碰撞、伤害、重力、穿透和爆炸交给 TACZ 自身。网络击发通知单独保存，不再次执行原生发射。

`PhysicalCrawl` 借用并恢复 pose、forced pose、crawling 和 swimming 状态。干地爬行不是水中游泳；实际 Actor/Player 的维度和眼高受限制。构造期 TACZ holder 尚未初始化时，尺寸查询不会解引用空 holder。客户端与服务端 ownership 分侧清理。演员绑定、服务端 ActionPlayer 和摄像机各自拥有请求，最近仍存在的请求优先，最后一个请求退出时还原。状态与所有者使用弱对象身份键，不能用 Minecraft 按 ID 比较的 Entity.equals 合并集成客户端/服务端实体。
