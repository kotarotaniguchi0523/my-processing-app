#!/bin/bash
echo "========================================"
echo "Java自動修正ツール実行スクリプト"
echo "========================================"

echo ""
echo "[1/3] Google Java Format実行中..."
find src/main/java -name "*.java" -type f -exec java -jar google-java-format-1.17.0-all-deps.jar --replace {} \;
if [ $? -eq 0 ]; then
    echo "Google Java Format完了"
else
    echo "Google Java Format失敗"
fi

echo ""
echo "[2/3] Checkstyle違反チェック中..."
mvn checkstyle:check > checkstyle-report.txt 2>&1
echo "Checkstyleレポート: checkstyle-report.txt"

echo ""
echo "[3/3] PMD違反チェック中..."
mvn pmd:check > pmd-report.txt 2>&1
echo "PMDレポート: pmd-report.txt"

echo ""
echo "========================================"
echo "自動修正完了！"
echo "========================================"
echo ""
echo "レポート確認:"
echo "- checkstyle-report.txt"
echo "- pmd-report.txt"
echo "- target/checkstyle-result.xml"
echo "- target/pmd.xml"