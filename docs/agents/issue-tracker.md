# 问题追踪器：GitHub

本仓库的需求、缺陷和 PRD 使用 GitHub Issues 管理，并通过 `gh` CLI 操作。

- 创建：`gh issue create --title "..." --body "..."`
- 查看：`gh issue view <编号> --comments`
- 列表：`gh issue list --state open`
- 评论：`gh issue comment <编号> --body "..."`
- 标签：`gh issue edit <编号> --add-label "..."` 或 `--remove-label "..."`
- 关闭：`gh issue close <编号> --comment "..."`

在仓库中运行时，`gh` 会自动识别 `xybigdata/yu-bi`。

## Pull Request 作为分诊入口

**Pull Request 不是需求分诊入口。** 如需将外部 Pull Request 纳入分诊范围，可在后续将此约定改为启用。

## 技能操作约定

- 当技能需要“发布到问题追踪器”时，创建 GitHub Issue。
- 当技能需要“获取相关工单”时，运行 `gh issue view <编号> --comments`。
