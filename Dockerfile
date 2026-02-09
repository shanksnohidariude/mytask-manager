FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY . .

# ★ ここが重要（実行権限を付ける）
RUN chmod +x mvnw

RUN ./mvnw clean package -DskipTests

EXPOSE 8080

CMD ["java", "-jar", "target/*.jar"]
