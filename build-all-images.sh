#!/bin/bash
# OOMALL 本地镜像构建脚本
# 执行：./build-all-images.sh

set -e

echo "======= OOMALL 本地镜像构建开始 ======="
echo "构建时间: $(date)"
echo ""

# 配置
IMAGE_PREFIX="oomall"
VERSION="1.0.0"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 函数：构建单个镜像
build_image() {
    local name=$1
    local context=$2

    echo -e "${YELLOW}[构建中] ${name}...${NC}"

    if [ -d "$context" ]; then
        docker build -t ${IMAGE_PREFIX}-${name}:${VERSION} $context

        if [ $? -eq 0 ]; then
            echo -e "${GREEN}[成功] ${name} 镜像构建完成${NC}"
        else
            echo -e "${RED}[失败] ${name} 镜像构建失败${NC}"
            exit 1
        fi
    else
        echo -e "${RED}[错误] 目录不存在: $context${NC}"
        exit 1
    fi
}

# 1. 检查前置条件
echo "1. 检查前置条件..."
if ! command -v docker &> /dev/null; then
    echo -e "${RED}[错误] Docker 未安装，请先安装 Docker${NC}"
    exit 1
fi


# 2. 构建 MySQL 镜像
echo ""
echo "2. 构建 MySQL 镜像..."
build_image "mysql" "mysql"

# 3. 构建售后模块镜像
echo ""
echo "3. 构建售后模块镜像..."

# 检查售后模块JAR文件
if [ ! -f "aftersale/app.jar" ]; then
    echo -e "${RED}[错误] 找不到售后模块 JAR 文件: aftersale/app.jar${NC}"
    echo -e "${YELLOW}[提示] 请将编译好的 JAR 文件复制到 aftersale/ 目录并重命名为 app.jar${NC}"
    exit 1
fi

build_image "aftersale" "aftersale"

# 4. 构建服务模块镜像
echo ""
echo "4. 构建服务模块镜像..."

# 检查服务模块JAR文件
if [ ! -f "service/app.jar" ]; then
    echo -e "${RED}[错误] 找不到服务模块 JAR 文件: service/app.jar${NC}"
    echo -e "${YELLOW}[提示] 请将编译好的 JAR 文件复制到 service/ 目录并重命名为 app.jar${NC}"
    exit 1
fi

build_image "service" "service"

# 5. 构建完成，显示镜像列表
echo ""
echo "======= 构建完成 ======="
echo "已构建的镜像:"
docker images | grep ${IMAGE_PREFIX}


echo ""
echo "使用说明:"
echo "1. 导出镜像: ./export-images.sh"
echo "2. 部署到服务器: 复制镜像文件到服务器，运行 ./import-images.sh"
echo "3. 启动服务: docker stack deploy -c docker-stack.yml oomall"