# RikkaHub Desktop

RikkaHub Desktop 管理用户与助手之间的对话，并协调模型响应、工具调用和持久化的对话记录。

## Language

**Conversation execution**:
一次对话响应从用户请求开始，到模型输出、工具调用和可见结果结束的完整生命周期。
_Avoid_: generation loop, chat flow

**Settings edit session**:
设置被打开到保存或丢弃之间的一次可撤销编辑过程。
_Avoid_: settings draft, settings flow
