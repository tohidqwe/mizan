FROM node:24-bookworm
ARG ANDROID_SDK_ROOT=/opt/android-sdk
ARG CMDLINE_TOOLS=15859902
ARG GRADLE_VERSION=8.13
ENV ANDROID_HOME=${ANDROID_SDK_ROOT} ANDROID_SDK_ROOT=${ANDROID_SDK_ROOT} PATH=${PATH}:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools:${ANDROID_SDK_ROOT}/build-tools/36.0.0:/opt/gradle/bin GRADLE_USER_HOME=/tmp/gradle-home JAVA_TOOL_OPTIONS="-Xmx512m -XX:MaxMetaspaceSize=256m"
RUN apt-get update && apt-get install -y --no-install-recommends openjdk-17-jdk-headless ca-certificates wget unzip && rm -rf /var/lib/apt/lists/* && mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools /opt/gradle && wget -q https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS}_latest.zip -O /tmp/tools.zip && unzip -q /tmp/tools.zip -d ${ANDROID_SDK_ROOT}/cmdline-tools && mv ${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools/latest && wget -q https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip -O /tmp/gradle.zip && unzip -q /tmp/gradle.zip -d /opt && ln -s /opt/gradle-${GRADLE_VERSION}/* /opt/gradle/
RUN yes | sdkmanager --licenses >/dev/null 2>&1 || true
RUN sdkmanager "platform-tools" "platforms;android-36" "build-tools;36.0.0" && rm -f /tmp/tools.zip /tmp/gradle.zip
WORKDIR /app
COPY cloud/package.json ./package.json
RUN npm install --omit=dev --no-audit --no-fund
COPY cloud/server.mjs ./server.mjs
ENV NODE_ENV=production PORT=8080 DATA_DIR=/tmp/aifactory
EXPOSE 8080
CMD ["node","server.mjs"]
