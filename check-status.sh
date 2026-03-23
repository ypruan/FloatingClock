#!/bin/bash
# 检查 GitHub Actions 状态脚本

REPO="ypruan/FloatingClock"

echo "========================================"
echo "  悬浮时钟 GitHub Actions 状态检查"
echo "========================================"
echo ""

# 获取最新的 workflow run
API_URL="https://api.github.com/repos/$REPO/actions/runs?per_page=1"

echo "正在检查最新构建状态..."
echo ""

# 使用 curl 获取状态
RESPONSE=$(curl -s -H "Accept: application/vnd.github.v3+json" "$API_URL" 2>/dev/null)

# 检查是否有 workflow runs
if echo "$RESPONSE" | grep -q '"total_count": 0'; then
    echo "❌ 暂无构建记录"
    echo ""
    echo "可能原因："
    echo "  1. 代码刚刚推送，Actions 还未触发"
    echo "  2. Actions 未启用"
    echo ""
    echo "解决方法："
    echo "  1. 等待 1-2 分钟后再次检查"
    echo "  2. 访问 https://github.com/$REPO/actions 查看状态"
    exit 1
fi

# 解析状态
STATUS=$(echo "$RESPONSE" | grep -o '"status": "[^"]*"' | head -1 | cut -d'"' -f4)
CONCLUSION=$(echo "$RESPONSE" | grep -o '"conclusion": "[^"]*"' | head -1 | cut -d'"' -f4)
RUN_URL=$(echo "$RESPONSE" | grep -o '"html_url": "[^"]*"' | head -1 | cut -d'"' -f4)

echo "========================================"
echo ""

if [ "$STATUS" = "completed" ]; then
    if [ "$CONCLUSION" = "success" ]; then
        echo "✅ 构建成功！"
        echo ""
        echo "下载链接："
        echo "  https://github.com/$REPO/actions"
        echo ""
        echo "或者访问上面的链接，在 Artifacts 部分下载 app-debug"
    else
        echo "❌ 构建失败（$CONCLUSION）"
        echo ""
        echo "查看日志："
        echo "  $RUN_URL"
    fi
else
    echo "⏳ 构建进行中（$STATUS）..."
    echo ""
    echo "查看进度："
    echo "  $RUN_URL"
fi

echo ""
echo "========================================"
