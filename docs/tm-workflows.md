# Taskmaster Workflows 深掘りガイド

`.claude/commands/tm/workflows/` に基づく実務向けのワークフロー運用手順を、意思決定の流れと具体コマンドで整理。Windows PowerShell (pwsh) 前提の例を掲載。

目的:
- 何を・いつ・どの順番で実行するかを明確化
- タグ(複数コンテキスト)の使い分け基準を提示
- イテラティブ実装(計画→実装→ログ→検証)を定型化

---

## このガイドとコマンドの対応表
- Workflow A → `.claude/commands/tm/workflows/auto-implement-tasks.md`
- Workflow B → `.claude/commands/tm/workflows/command-pipeline.md`
- Workflow C → `.claude/commands/tm/workflows/smart-workflow.md`


## 用語最短メモ
- タグ: タスクの“作業コンテキスト”分離(例: `master`, `feature-auth`)。
- 親タスク/サブタスク: 分解の単位。`1.2` はタスク1のサブタスク2。
- 研究(research): LLMを調査モードで利用(最新ベストプラクティス反映)。

---

## Workflow A: シンプル反復ループ(個人/小規模向け)
最短距離でタスクを前へ進める基本ループ。

1) 何をやるか決める
```pwsh
# 依存が満たされた“今やれる”タスクを提示
task-master next
# 詳細を確認(必要あれば)
task-master show <id>
```

2) 必要に応じて分解(曖昧・重いなら)
```pwsh
# 推奨サブタスクを自動生成(研究を併用推奨)
task-master expand --id <id> --research
```

3) 実装計画を“サブタスクの詳細”に記録
```pwsh
# 変更ファイル/関数・検証観点・潜在リスクを具体的に
task-master update-subtask --id <id.sub> --prompt "計画: ここに詳細"
```

4) 着手→進捗をログ化
```pwsh
task-master set-status --id <id.sub> --status in-progress
# 進捗・試行/失敗・決定事項を小まめに追記
task-master update-subtask --id <id.sub> --prompt "進捗: ここに事実ベースで"
```

5) 完了処理
```pwsh
# サブタスク/親タスクの順でDone化
task-master set-status --id <id.sub> --status done
```

Tips:
- `update-subtask` は“時系列ログ”。箇条書きで“何を・なぜ・結果”を残すと再現性が上がる。
- 迷ったら A へ戻る。1サイクルを小さく速く回すのが正解。

---

## Workflow B: コマンド・パイプライン(PRD→展開→文書化)
PRD(仕様)から一気通貫でタスク群を生成し、展開/文書化まで行う定型手順。作業をタグで分離すると安全。

```pwsh
# 1) 初期化(未実施なら)
task-master init -y

# 2) PRDからタスク生成(例: requirement/prd.md を専用タグへ)
task-master parse-prd requirement/prd.md --tag feature-core --num 12 --research

# 3) 複雑度分析→サブタスク展開
task-master analyze-complexity --tag feature-core --research
task-master expand --all --tag feature-core --research

# 4) 依存の健全性チェック
task-master validate-dependencies --tag feature-core
# (問題あれば) 自動修正
task-master fix-dependencies --tag feature-core

# 5) 文書化(Markdown出力)
task-master generate --tag feature-core
# 参考: 分析レポート
task-master complexity-report --tag feature-core
```

使い所:
- 初期ブートストラップ
- PRD更新で計画を再生成したいとき

---

## Workflow C: スマート・ワークフロー(タグ戦略と高度運用)
規模/組織化に応じて“タグ”で作業コンテキストを分離し、master を高レベルに保つ。

導入の目安(パターン):
1. Gitブランチを切った → ブランチ名と同名タグを作る
2. チーム作業 → 自分用タグ/機能別タグを作る
3. 実験/破壊的リファクタ → `experiment-*` を作る
4. 大型機能(PRD駆動) → `feature-*` を作る
5. バージョン軸 → `prototype/mvp` はスピード重視, `v1.0+` は堅牢性重視

手順例:
```pwsh
# ブランチに対応したタグを作成・切替
# (from-branch が使えない環境では add-tag 後 use-tag)
# 例: feature-user-auth で作業
# task-master add-tag feature-user-auth --from-branch
# 代替:
# task-master add-tag feature-user-auth --description "認証機能"
# task-master use-tag feature-user-auth

# PRDの投入→分析→展開
task-master parse-prd .taskmaster/docs/feature-auth-prd.txt --tag feature-user-auth
task-master analyze-complexity --tag feature-user-auth --research
task-master expand --all --tag feature-user-auth --research

# 実装は Workflow A をタグ内で回す
```

運用ルール例:
- master には“価値の高いエピック/マイルストン”のみ。詳細タスクはタグ側に置く。
- tag を跨ぐ作業は基本しない。混ぜると依存が壊れやすい。

---

## タグ導入の意思決定ツリー
- ひとり開発で小規模? → A(シンプル)で十分
- 複数人/ブランチ? → C(タグ)へ移行
- 仕様から一括生成したい? → B(パイプライン)
- 破壊的/試行? → C の `experiment-*`

---

## イテラティブ実装の“型” (品質担保向け)
実装のたびに以下の品質ゲートを軽量で通す。

1) 目的/スコープの明文化
```pwsh
task-master show <id.sub>
# 要点を update-subtask に追記
```
2) 影響範囲/リスクの洗い出し(短時間の research 推奨)
```pwsh
# 例: ライブラリの最新Best Practices
# task-master research "Processing 4.x でのフォント日本語対応"
```
3) 実装/検証 → ログ化
```pwsh
task-master update-subtask --id <id.sub> --prompt "検証方法/結果/次の一手"
```
4) ステータス更新/依存の見直し
```pwsh
task-master set-status --id <id.sub> --status done
# 必要なら update / add-dependency で計画を更新
```

---

## よくあるつまずきと対処
- PRD取り込み後に何も出ない → `--force` で上書き or タグ指定を確認
- 展開が粗い/過剰 → `expand --num` で調整, 既存が邪魔なら `--force`
- 依存がおかしい → `validate-dependencies` → `fix-dependencies`
- 変更履歴が薄い → `update-subtask` を“都度・短く”でOK
- 別作業が混ざる → タグを分ける(`add-tag`/`use-tag`)

---

## 1日の開始テンプレ(タグ作業時)
```pwsh
# 今日の作業タグへ
task-master use-tag feature-core
# 状態確認
task-master status --tag feature-core
# 着手タスク
task-master next --tag feature-core
```

## 完了前の仕上げテンプレ
```pwsh
# 依存健全性
task-master validate-dependencies --tag feature-core
# 文書化
task-master generate --tag feature-core
# (任意) README 同期
# task-master sync-readme --tag feature-core --with-subtasks
```

---

## 参考: 役割分担(ざっくり)
- A(反復): 実装速度優先。個人/少人数向け
- B(一括): 計画の自動生成と“見える化”。初期/再計画向け
- C(タグ): 並行開発と衝突回避。規模/チーム向け

---

最終更新: 本ガイドは `.claude/commands/tm/workflows/`(auto-implement-tasks.md / command-pipeline.md / smart-workflow.md) に準拠。状況に応じて加筆してください。
