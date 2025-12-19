#!/bin/bash
# 在服务器上导入镜像

set -e

echo "======= 导入 Docker 镜像 ======="
echo "服务器: $(hostname)"
echo "导入时间: $(date)"
echo ""

# 配置
IMPORT_DIR="exported-images"

# 检查目录
if [ ! -d "${IMPORT_DIR}" ]; then
    echo "错误: 目录 ${IMPORT_DIR} 不存在"
    echo ""
    echo "使用方法:"
    echo "1. 从本地复制镜像文件到服务器"
    echo "2. 解压文件（如果有压缩包）"
    echo "3. 运行此脚本"
    exit 1
fi

# 检查 Docker 是否运行
if ! docker info &> /dev/null; then
    echo "错误: Docker 服务未运行"
    exit 1
fi

# 导入所有镜像文件
echo "开始导入镜像..."
for IMAGE_FILE in ${IMPORT_DIR}/*.tar; do
    if [ -f "${IMAGE_FILE}" ]; then
        echo "导入: $(basename ${IMAGE_FILE})"

        # 导入镜像
        docker load -i ${IMAGE_FILE}

        if [ $? -eq 0 ]; then
            echo "✓ 导入成功"
        else
            echo "✗ 导入失败"
            exit 1
        fi
    fi
done

# 显示导入的镜像
echo ""
echo "已导入的镜像:"
docker images | grep oomall

echo ""
echo "======= 导入完成 ======="
echo "下一步: 使用 docker stack deploy 部署服务"