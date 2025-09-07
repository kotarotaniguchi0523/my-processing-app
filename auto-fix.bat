@echo off
echo ========================================
echo Java自動修正ツール実行スクリプト
echo ========================================

echo.
echo [1/3] Google Java Format実行中...
setlocal EnableDelayedExpansion
set GF_JAR=google-java-format-1.17.0-all-deps.jar
REM AOSPスタイルにすることで4スペースインデントに合わせる
set GF_OPTS=--aosp
set FORMAT_ERRORS=0

REM src/main/java を再帰整形
if exist src\main\java (
    for /R "src\main\java" %%f in (*.java) do (
        echo   Formatting: %%f
        java -jar %GF_JAR% %GF_OPTS% --replace "%%f" || set /A FORMAT_ERRORS+=1 >nul 2>&1
    )
)

REM src/test/java も再帰整形
if exist src\test\java (
    for /R "src\test\java" %%f in (*.java) do (
        echo   Formatting: %%f
        java -jar %GF_JAR% %GF_OPTS% --replace "%%f" || set /A FORMAT_ERRORS+=1 >nul 2>&1
    )
)

if %FORMAT_ERRORS% GTR 0 (
    echo Google Java Formatで !FORMAT_ERRORS! 件のファイルでエラーが発生しました
) else (
    echo Google Java Format完了
)
endlocal

echo.
echo [2/3] Checkstyle違反チェック中...
mvn checkstyle:check > checkstyle-report.txt 2>&1
echo Checkstyleレポート: checkstyle-report.txt

echo.
echo [3/3] PMD違反チェック中...
mvn pmd:check > pmd-report.txt 2>&1
echo PMDレポート: pmd-report.txt

echo.
echo ========================================
echo 自動修正完了！
echo ========================================
echo.
echo レポート確認:
echo - checkstyle-report.txt
echo - pmd-report.txt
echo - target/checkstyle-result.xml
echo - target/pmd.xml