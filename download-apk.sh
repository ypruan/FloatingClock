#!/bin/bash
# 下载 GitHub Actions Artifact 脚本

REPO="ypruan/FloatingClock"
API_URL="https://api.github.com/repos/$REPO"

echo "=========================================="
echo "  悬浮时钟 APK 下载助手"
echo "=========================================="
echo ""

# 获取最新的成功构建
echo "正在查询最新的成功构建..."
RUNS_URL="$API_URL/actions/runs?status=success&per_page=1"
RUNS_RESPONSE=$(curl -s "$RUNS_URL")

if [ -z "$RUNS_RESPONSE" ]; then
    echo "❌ 无法获取构建信息"
    echo ""
    echo "请手动访问："
    echo "  https://github.com/$REPO/actions"
    exit 1
fi

# 解析最新的 run ID
LATEST_RUN_ID=$(echo "$RUNS_RESPONSE" | grep -o '"id": [0-9]*' | head -1 | grep -o '[0-9]*')

if [ -z "$LATEST_RUN_ID" ]; then
    echo "❌ 未找到成功的构建记录"
    echo ""
    echo "可能原因："
    echo "  1. GitHub Actions 工作流尚未运行"
    echo "  2. 最近的构建都失败了"
    echo ""
    echo "请访问以下链接查看状态："
    echo "  https://github.com/$REPO/actions"
    exit 1
fi

echo "✅ 找到最新构建: Run #$LATEST_RUN_ID"
echo ""

# 查询该 run 的 artifacts
ARTIFACTS_URL="$API_URL/actions/runs/$LATEST_RUN_ID/artifacts"
ARTIFACTS_RESPONSE=$(curl -s "$ARTIFACTS_URL")

# 检查是否有 artifacts
ARTIFACT_COUNT=$(echo "$ARTIFACTS_RESPONSE" | grep -o '"total_count": [0-9]*' | grep -o '[0-9]*' | head -1)

if [ -z "$ARTIFACT_COUNT" ] || [ "$ARTIFACT_COUNT" -eq 0 ]; then
    echo "❌ 该构建没有生成可下载的 Artifact"
    echo ""
    echo "可能原因："
    echo "  1. APK 文件生成失败"
    echo "  2. Artifact 已被自动清理（保留期30天）"
    echo ""
    echo "建议操作："
    echo "  1. 访问 GitHub Actions 页面查看详细日志："
    echo "     https://github.com/$REPO/actions/runs/$LATEST_RUN_ID"
    echo "  2. 在页面上查看 'Build APK' 步骤的输出"
    echo "  3. 如果需要，可以重新触发构建"
    echo ""
    echo "💡 手动下载方式："
    echo "  如果构建成功但 artifact 未生成，可以："
    echo "  1. Fork 这个仓库到你自己的账号"
    echo "  2. GitHub Actions 会自动运行"
    echo "  3. 从 Fork 的仓库下载 artifact"
    exit 1
fi

echo "✅ 找到 $ARTIFACT_COUNT 个 Artifact"
echo ""

# 解析 artifact 信息并下载
ARTIFACT_ID=$(echo "$ARTIFACTS_RESPONSE" | grep -o '"id": [0-9]*' | grep -o '[0-9]*' | head -1)
ARTIFACT_NAME=$(echo "$ARTIFACTS_RESPONSE" | grep -o '"name": "[^"]*"' | head -1 | cut -d'"' -f4)
ARTIFACT_SIZE=$(echo "$ARTIFACTS_RESPONSE" | grep -o '"size_in_bytes": [0-9]*' | grep -o '[0-9]*' | head -1)

echo "Artifact 信息:"
echo "  名称: $ARTIFACT_NAME"
echo "  ID: $ARTIFACT_ID"
echo "  大小: $(($ARTIFACT_SIZE / 1024)) KB"
echo ""

# 下载 artifact
DOWNLOAD_URL="$API_URL/actions/artifacts/$ARTIFACT_ID/zip"
OUTPUT_FILE="${ARTIFACT_NAME}.zip"

echo "正在下载..."
echo "  保存为: $OUTPUT_FILE"

# 使用 curl 下载
curl -s -L -o "$OUTPUT_FILE" -H "Accept: application/vnd.github+json" "$DOWNLOAD_URL"

if [ -f "$OUTPUT_FILE" ]; then
    FILE_SIZE=$(stat -f%z "$OUTPUT_FILE" 2>/dev/null || stat -c%s "$OUTPUT_FILE" 2>/dev/null)
    echo ""
    echo "✅ 下载成功！"
    echo "  文件名: $OUTPUT_FILE"
    echo "  大小: $(($FILE_SIZE / 1024)) KB"
    echo ""
    echo "解压命令:"
    echo "  unzip $OUTPUT_FILE -d extracted/"
    echo ""
    echo "解压后会得到 app-debug.apk 文件"
else
    echo ""
    echo "❌ 下载失败"
    echo "Artifact 可能需要 GitHub 认证才能下载"
    echo ""
    echo "替代方法："
    echo "  1. 直接访问 GitHub Actions 页面下载："
    echo "     https://github.com/$REPO/actions/runs/$LATEST_RUN_ID"
    echo ""
    echo "  2. 在页面底部的 'Artifacts' 部分点击下载"
    exit 1
fi
