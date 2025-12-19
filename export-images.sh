#!/bin/bash
# 导出镜像为文件，便于传输到服务器

set -e

echo "======= 导出 Docker 镜像 ======="
echo "导出时间: $(date)"
echo ""

# 配置
IMAGE_PREFIX="oomall"
VERSION="1.0.0"
OUTPUT_DIR="exported-images"

# 创建导出目录
mkdir -p ${OUTPUT_DIR}

# 镜像列表
IMAGES=(
    "${IMAGE_PREFIX}-mysql:${VERSION}"
    "${IMAGE_PREFIX}-aftersale:${VERSION}"
    "${IMAGE_PREFIX}-service:${VERSION}"
)

# 导出每个镜像
for IMAGE in "${IMAGES[@]}"; do
    echo "导出镜像: ${IMAGE}"

    # 检查镜像是否存在
    if ! docker image inspect ${IMAGE} &> /dev/null; then
        echo "错误: 镜像 ${IMAGE} 不存在"
        echo "请先运行 ./build-all-images.sh 构建镜像"
        exit 1
    fi

    # 生成文件名（去掉冒号）
    FILENAME=$(echo ${IMAGE} | sed 's/:/_/g').tar

    # 导出镜像
    docker save -o ${OUTPUT_DIR}/${FILENAME} ${IMAGE}

    if [ $? -eq 0 ]; then
        echo "✓ 导出成功: ${OUTPUT_DIR}/${FILENAME}"
    else
        echo "✗ 导出失败: ${IMAGE}"
        exit 1
    fi
done

# 创建压缩包（可选）
echo ""
echo "创建压缩包..."
tar -czf oomall-images-${VERSION}.tar.gz ${OUTPUT_DIR}/
echo "✓ 压缩包创建完成: oomall-images-${VERSION}.tar.gz"

# 显示文件信息
echo ""
echo "导出完成！文件信息:"
ls -lh ${OUTPUT_DIR}/
echo ""
ls -lh oomall-images-${VERSION}.tar.gz

echo ""
echo "传输到服务器的方法:"
echo "1. SCP 命令:"
echo "   scp oomall-images-${VERSION}.tar.gz root@服务器IP:/opt/oomall/"
echo "2. 然后在服务器上解压并导入:"
echo "   tar -xzf oomall-images-${VERSION}.tar.gz"
echo "   cd exported-images"
echo "   for f in *.tar; do docker load -i \$f; done"