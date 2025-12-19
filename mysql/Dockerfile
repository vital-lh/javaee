FROM mysql:8.0

LABEL maintainer="oomall-team"
LABEL version="1.0.0"

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 复制配置文件
COPY my.cnf /etc/mysql/conf.d/
COPY init.sql /docker-entrypoint-initdb.d/

EXPOSE 3306

CMD ["mysqld"]