# 実行ドキュメント: my-processing-app

## 目的
Processing(Java版) のスケッチ `SimpleSketch` を Maven プロジェクトとしてビルド / 実行できるようにする手順と、今回発生した問題と判断ポイントをまとめます。

## 成果物概要
- メインクラス: `com.example.SimpleSketch`
- 使用ライブラリ: Processing Core 4.4.7 + JOGL( OpenGL ) 2.5.0
- ビルド/実行管理: Maven + `exec-maven-plugin`

## フォルダ構成 (主要部)
```
my-processing-app/
  pom.xml
  src/main/java/com/example/SimpleSketch.java
  src/main/java/com/example/App.java (任意/サンプル)
  docs/RUNNING.md (このファイル)
```

## 実行コマンド
### 1. 依存解決とコンパイル
```powershell
mvn compile
```

### 2. 実行 (pom.xml の `<mainClass>` を利用)
```powershell
mvn exec:java
```

### 3. 他のメインクラスを一時的に指定して実行したい場合
`pom.xml` の設定を使わずコマンド引数で上書き:
```powershell
mvn exec:java -Dexec.mainClass="com.example.SimpleSketch"
```
> 注意: PowerShell では `-Dexec.mainClass="..."` の形式。間にスペースや全角文字が入らないようにする。

## 今回遭遇したエラーと原因・対応
| 現象 | エラーメッセージ (要約) | 原因 | 対処 |
|------|--------------------------|------|------|
| コンパイル失敗 | `クラス SimpleSketch は public であり、ファイル SimpleSketch.java で宣言する必要があります` | `public class SimpleSketch` を `App.java` という異なるファイル名で定義していた | クラスを `SimpleSketch.java` に分離 |
| 実行時 `Unknown lifecycle phase ".mainClass=..."` | `Unknown lifecycle phase ".mainClass=com.example.SimpleSketch"` | `-Dexec.mainClass=...` の前に `exec:java` を正しく解釈できず、シェル/入力ミスで `-D` が Maven へ JVM 引数として渡らずライフサイクル扱いされた | コマンド形式を確認。`mvn exec:java -Dexec.mainClass="com.example.SimpleSketch"` の順序/構文を厳密化 |
| 文字表示警告 | `Some characters not available...` | 日本語テキスト描画時のフォント未指定 | `createFont("MS Gothic", 24, true)` でフォント指定 |

## 判断・実行の思考プロセス
1. まずビルドエラー内容からファイル名と public クラス名の不一致を特定 (Java 言語仕様)。
2. クラス分割後、Maven 実行設定を確認: `exec-maven-plugin` の `<mainClass>` が旧 `App` 指定のまま → `SimpleSketch` に変更。
3. ユーザーが `-Dexec.mainClass` を付けて実行するも `Unknown lifecycle phase` → Maven が `-D` オプションを認識していない形跡。入力解析: スペース位置/引用符崩れを疑い正しいコマンド提示。
4. 実行成功ログ後、日本語描画警告 → Processing はデフォルトフォントに日本語グリフ無し → 明示フォント生成コード追加。
5. 再コンパイル・再実行で警告軽減と機能動作確認。

## トラブルシューティング チェックリスト
- [ ] `pom.xml` の `<mainClass>` が現在実行したいクラスか
- [ ] パッケージ名と `PApplet.main("<完全修飾クラス名>")` が一致しているか
- [ ] Java ファイル名と `public class` 名が一致しているか
- [ ] `mvn -v` で Maven が想定バージョンか
- [ ] コマンドに全角スペースや全角引用符が混入していないか
- [ ] フォント警告が出る場合: `createFont` + `textFont` を設定

## コード抜粋 (フォント設定部)
```java
public void setup() {
    background(240);
    textAlign(CENTER, CENTER);
    try {
        font = createFont("MS Gothic", 24, true);
        textFont(font);
    } catch (Exception e) {
        textSize(24);
    }
}
```

## よくあるミス
- `mvn exec:java -D exec.mainClass=...` のように `-D` 前に余計なスペースを入れる
- PowerShell でダブルクォートを片方全角にしてしまう
- `PApplet.main("SimpleSketch")` としてパッケージ名を忘れる (`com.example.SimpleSketch` が必要)

## 追加改善案
- `README.md` に簡易手順を抜粋
- `profiles` で別スケッチを切り替え可能にする
- `mvn package` で実行用シェード jar を生成 ( `maven-shade-plugin` )

## シェード Jar 生成 (任意例)
```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-shade-plugin</artifactId>
  <version>3.5.0</version>
  <executions>
    <execution>
      <phase>package</phase>
      <goals><goal>shade</goal></goals>
      <configuration>
        <transformers>
          <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
            <mainClass>com.example.SimpleSketch</mainClass>
          </transformer>
        </transformers>
      </configuration>
    </execution>
  </executions>
</plugin>
```
→ 生成後の起動例:
```powershell
mvn package
java -jar target/my-processing-app-1.0-SNAPSHOT-shaded.jar
```

## まとめ
- Java の public クラス命名規則違反が最初の障害。
- `exec-maven-plugin` の mainClass 修正と正しいコマンド構文で実行成功。
- 日本語表示はフォント明示で解決。

この手順に従えば、Processing スケッチの Maven 実行環境を安定化できます。
