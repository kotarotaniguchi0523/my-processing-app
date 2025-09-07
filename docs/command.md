# Maven での一般的な Java 実行方法まとめ

本プロジェクトや他の標準的な Maven Java アプリで利用できる実行パターンのチートシートです。

---
## 1. よく使うライフサイクル
```powershell
mvn clean        # 生成物 (target) 削除
mvn compile      # ソースをコンパイル (target/classes)
mvn test         # テスト実行 (JUnit など)
mvn package      # jar 生成 (target/<artifact>-<version>.jar)
mvn install      # ローカル ~/.m2 リポジトリへ配置
mvn deploy       # リモートリポジトリへデプロイ (設定時)
```

---
## 2. Main クラスの実行方法バリエーション
### 2-1. exec-maven-plugin（最も手軽 / 推奨）
`pom.xml` に以下のような設定:
```xml
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>exec-maven-plugin</artifactId>
  <version>3.1.0</version>
  <configuration>
    <mainClass>com.example.Main</mainClass>
  </configuration>
</plugin>
```
実行:
```powershell
mvn exec:java
```
一時的に他クラスを指定:
```powershell
mvn exec:java -Dexec.mainClass="com.example.OtherMain"
```
追加 JVM オプション:
```powershell
mvn exec:java -Dexec.mainClass="com.example.Main" -Dexec.args="arg1 arg2" -Dexec.jvmArgs="-Xms256m -Xmx512m"
```

### 2-2. 生成されたクラスパスを直接利用
```powershell
mvn compile
java -cp target/classes com.example.Main
```
依存 jar も必要な場合（後述 2-3 参照）。

### 2-3. 依存ライブラリを展開しつつ実行
```powershell
mvn dependency:copy-dependencies -DincludeScope=runtime
java -cp "target/classes;target/dependency/*" com.example.Main   # Windows は ; 区切り
```

### 2-4. 依存込み単一 (fat) JAR（Shade）
`pom.xml` 例:
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
            <mainClass>com.example.Main</mainClass>
          </transformer>
        </transformers>
      </configuration>
    </execution>
  </executions>
</plugin>
```
実行:
```powershell
mvn package
java -jar target/<artifact>-<version>-shaded.jar
```

### 2-5. Spring Boot / Quarkus 等 (参考)
フレームワーク毎に `spring-boot:run` / `quarkus:dev` など専用ゴールあり。

---
## 3. テストの制御
```powershell
mvn test                       # 全テスト
mvn -Dtest=ExactTest test       # 特定クラス
mvn -Dtest=ExactTest#method test # 特定メソッド
mvn package -DskipTests         # 実行をスキップ (コンパイルはする)
mvn package -DskipITs           # IT (integration-test) をスキップ
mvn verify -DfailIfNoTests=false # テストゼロでも成功扱い
```

---
## 4. プロファイル活用
`pom.xml` の `<profiles>` に環境別設定を定義し、必要なときに切替:
```powershell
mvn -Pdev package
mvn -Pprod exec:java
```
複数指定:
```powershell
mvn -Pdev,featureX package
```

---
## 5. 便利オプション
```powershell
mvn -q package   # quiet ログ最小
mvn -e package   # 例外スタックトレース表示
mvn -X package   # デバッグ出力フル
mvn -T 1C package # マルチスレッド (1C = CPU コア数)
```
プロキシ環境など:
```powershell
mvn -Dhttp.proxyHost=host -Dhttp.proxyPort=8080 clean install
```

---
## 6. 代表的トラブルと対処
| 現象 | 例のメッセージ | 主原因 | 対処 |
|------|----------------|--------|------|
| `Unknown lifecycle phase` | `".mainClass=..."` | `-Dexec.mainClass` の位置/全角スペース | コマンド構文を正確に再入力 |
| `ClassNotFoundException` | Main クラス見つからない | クラスパス不足 | `-cp` に `target/dependency/*` 追加 / シェード |
| 文字化け / フォント警告 | `Some characters not available` | フォント未設定 | `createFont` や `-Dfile.encoding=UTF-8` |
| Java バージョン差異 | `invalid target release` | `maven.compiler.release` と JDK不一致 | JDK 切替 / `toolchains.xml` 利用 |
| OutOfMemoryError | `Java heap space` | ヒープ不足 | `-Dexec.jvmArgs="-Xmx1G"` / 環境調整 |

---
## 7. クリーンな一連の流れ例
```powershell
# 1. 依存解決 + コンパイル + テスト + パッケージ
mvn clean package

# 2. (exec プラグイン設定済みで) 起動
mvn exec:java

# 3. シェード Jar で配布 & 起動
mvn package
java -jar target/your-artifact-1.0-SNAPSHOT-shaded.jar
```

---
## 8. ちょっと進んだ運用
- `maven-toolchains-plugin` で複数 JDK 切替
- `flatten-maven-plugin` で配布向けの簡潔 POM
- CI (GitHub Actions 等) では: `actions/setup-java` + キャッシュ + `mvn -B -ntp verify`

---
## 9. よくあるベストプラクティス
- 固定バージョン管理: `<dependencyManagement>` で揃える
- 再現性: `mvn -nsu` (snapshots 更新抑制), 依存キャッシュ活用
- セキュリティ: `versions:display-dependency-updates` や `OWASP Dependency Check` プラグイン

---
## 10. 使い分け早見
| 目的 | 使うコマンド / 方法 |
|------|---------------------|
| とりあえず動かす | `mvn exec:java` |
| 依存含めて単一 jar | Shade で `java -jar` |
| デバッグ引数付 | `mvn exec:java -Dexec.jvmArgs="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"` |
| 他クラスで試行 | `-Dexec.mainClass=...` |
| 本番向け最適化 | シェード + ProGuard/GraalVM (必要なら) |

---
## 11. 確認コマンド例 (PowerShell)
```powershell
# Maven / Java バージョン
mvn -v

# 依存ツリー確認
mvn dependency:tree -Dincludes=org.processing:core

# 最新への更新候補表示
mvn versions:display-dependency-updates
```

---
## 12. 参考
- Maven Lifecycle: https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html
- Exec Plugin: https://www.mojohaus.org/exec-maven-plugin/
- Shade Plugin: https://maven.apache.org/plugins/maven-shade-plugin/

---
## 13. 最小テンプレ Main クラス例
```java
package com.example;
public class Main {
  public static void main(String[] args) {
    System.out.println("Hello Maven");
  }
}
```

---
## まとめ
開発中は `mvn exec:java`、配布はシェード Jar、検証は `dependency:tree` / `versions:...` を組み合わせるのが基本パターンです。
