FROM openjdk:11-jre-slim

LABEL maintainer="oomall-team"
LABEL version="1.0.0"
LABEL description="OOMALL 服务模块"

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 安装 curl 用于健康检查
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# 创建应用目录
WORKDIR /app

# 复制 JAR 文件
COPY target/service-0.0.1-SNAPSHOT.jar app.jar

# 复制配置文件
COPY config/application.yml /app/config/
COPY config/application-prod.yml /app/config/

# 创建日志目录
RUN mkdir -p /app/logs && chmod 755 /app/logs

# 设置 JVM 参数
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs/heapdump.hprof -Dfile.encoding=UTF-8 -Dspring.config.location=/app/config/"

# 暴露端口
EXPOSE 8081

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]