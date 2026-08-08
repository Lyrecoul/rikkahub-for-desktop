# RikkaHub Desktop

RikkaHub Desktop 管理用户与助手之间的对话，并协调模型响应、工具调用和持久化的对话记录。

## Language

**Conversation execution**:
一次对话响应从用户请求开始，到模型输出、工具调用和可见结果结束的完整生命周期。
_Avoid_: generation loop, chat flow

**Settings edit session**:
设置被打开到保存或丢弃之间的一次可撤销编辑过程。
_Avoid_: settings draft, settings flow

**Background model task**:
不阻塞对话的辅助生成请求——标题生成、回复建议、历史压缩、消息翻译——共享同一套生命周期管理（防重入、取消、错误定位、完成清理）。
_Avoid_: background job, helper generation

**Conversation workspace**:
对话编辑与执行编排的模块——生成期间拒绝编辑（门控）、ask_user 答案的路由（pending 注入或续跑）。
_Avoid_: chat controller, chat service

**Model discovery**:
从配置的 provider 实时获取模型列表的过程（live /models 优先，保留 display name）。
_Avoid_: model catalog, model list
