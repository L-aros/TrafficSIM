# CLAUDE

该文件用于 Claude 在 `TrafficManager` 仓库中的执行约定。

## 1. 快速上下文
- 先阅读：
  - `AGENTS.md`
  - `DEVELOPMENT.md`
  - `README.md`
- 仅在必要时扩展读取 `docs/` 和具体源码文件，避免无关扫描。

## 2. 默认执行策略
1. 先确认需求与影响范围。
2. 优先做最小可行改动，避免大规模重构。
3. 修改后执行可行的本地验证命令。
4. 输出简洁变更说明、验证结果和剩余风险。

## 3. 必守约束
- 不执行破坏性命令，除非用户明确授权。
- 不回退用户已有改动。
- 不改动无关文件。
- 无法验证时必须明确说明原因。

## 4. 推荐命令（PowerShell）
- 构建 Debug：
  - `.\gradlew.bat :app:assembleDebug`
- 安装 Debug：
  - `.\gradlew.bat :app:installDebug`
- 清理：
  - `.\gradlew.bat clean`

## 5. 提交前检查清单
- 代码可编译。
- 关键路径逻辑未回归（规则匹配、切卡、日志）。
- 用户可见行为变更已更新文档。

## 6. 分支与提交规范
- 默认在非 `main` 分支实施改动。
- 提交信息使用 Conventional Commits：
  - `feat(scope): summary`
  - `fix(scope): summary`
  - `docs(scope): summary`
  - `refactor(scope): summary`

## 7. PR 最低要求
- 必须给出：
  - 改动动机
  - 影响模块
  - 验证步骤与结果
  - 风险与回滚方式
- 涉及切卡行为时，必须说明至少一个失败场景如何处理。

## 8. 响应模板（建议）
- `Summary`: 本次改动做了什么
- `Validation`: 运行了哪些命令、结果如何
- `Risk`: 仍存在哪些不确定性
- `Next`: 建议后续动作（如需要）
