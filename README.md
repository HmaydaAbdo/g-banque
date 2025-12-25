# G-Banque Backend

Spring Boot application for cash flow management system.

## 📋 Technology Stack

- **Java**: 17
- **Framework**: Spring Boot 3.3.1
- **Database**: MySQL 8.0
- **Build Tool**: Maven 3.9.6
- **Container**: Docker

## 🏗️ Architecture

### Dependencies

- **Spring Boot Starter Web**: REST API endpoints
- **Spring Boot Starter Data JPA**: Database operations
- **Spring Boot Starter Validation**: Input validation
- **Spring Boot Starter Actuator**: Health monitoring
- **Spring Boot Starter Batch**: Scheduled batch jobs
- **Spring Boot Starter Mail**: Email notifications
- **Spring Boot Starter WebSocket**: Real-time communication
- **Spring Security OAuth2**: Authorization server
- **MySQL Connector**: Database driver
- **MapStruct**: Object mapping
- **OpenAPI/Swagger**: API documentation
- **Apache POI**: Excel file processing
- **OpenPDF**: PDF generation

## 🚀 Getting Started

### Prerequisites

- Docker & Docker Compose
- Java 17 (for local development)
- Maven 3.9+ (for local development)

### Environment Variables

Required environment variables (defined in `.env` at project root):

```bash
# Database
DB_HOST=mysql
DB_PORT=3306
MYSQL_DATABASE=Gbanque
MYSQL_USER=gbanque_user
MYSQL_PASSWORD=userpass123

# Server
SERVER_ADRESSE=0.0.0.0
SERVER_PORT=8080
CONTEXT_PATH=/gBanque/api

# JPA
JPA_DDL_AUTO=none
JPA_SHOW_SQL=false

# Mail (optional)
MAIL_HOST=mail.staport.ma
MAIL_PORT=587
MAIL_USERNAME=gbanque@staport.ma
MAIL_PASSWORD=your_password
```

### Running with Docker (Recommended)

```bash
# From project root (G-Banque/)
cd C:\Users\Hp\Desktop\G-Banque

# Build and start
docker-compose up -d backend

# View logs
docker-compose logs -f backend

# Stop
docker-compose stop backend
```

### Running Locally (Development)

```bash
# Navigate to backend directory
cd Backend

# Build
mvn clean package -DskipTests

# Run
java -jar target/gBanque-0.0.1-SNAPSHOT.jar

# Or use Maven
mvn spring-boot:run
```

## 📡 API Endpoints

### Base URL
```
http://localhost:8080/gBanque/api
```

### Health Check
```bash
GET /actuator/health
```

### API Documentation (Swagger)
```
http://localhost:8080/gBanque/api/swagger-ui.html
```

### Actuator Endpoints
```
http://localhost:8080/gBanque/api/actuator
```

Available endpoints:
- `/actuator/health` - Application health status
- `/actuator/info` - Application information
- `/actuator/metrics` - Application metrics

## 🗄️ Database

### Connection Details

- **Host**: localhost (or `mysql` in Docker)
- **Port**: 3306
- **Database**: Gbanque
- **Username**: gbanque_user
- **Password**: userpass123

### Schema Management

The application uses `ddl-auto: none` for production safety. Database schema is managed via:
- Initial schema: `schema-mysql.sql`
- Data backup: `backup_21_12_2025.sql`

### Accessing Database

**Via PHPMyAdmin:**
```
http://localhost:8081
Username: root
Password: rootpass123
```

**Via MySQL Client:**
```bash
# From Docker
docker-compose exec mysql mysql -u root -prootpass123 Gbanque

# From local
mysql -h localhost -u gbanque_user -puserpass123 Gbanque
```

## ⏰ Scheduled Jobs

### Functional Lines Job
- **Schedule**: Every Sunday at 12:00 PM
- **Purpose**: Check line expirations and send alerts

### Leasing Main Levée Job
- **Schedule**: Every Sunday at 2:00 PM
- **Purpose**: Process leasing releases

### CMT Main Levée Job
- **Schedule**: Every Sunday at 2:30 PM
- **Purpose**: Process CMT releases

## 📧 Email Notifications

The application sends email notifications for:
- Batch job completions
- Line expiration alerts (2 months threshold)
- Line expiration warnings (3 months threshold)

Recipients configured in `application.yml`:
```yaml
recipients: hmayda@staport-sa.ma
```

## 🔧 Configuration Files

### application.yml
Main configuration file located at `src/main/resources/application.yml`

Key sections:
- **Server**: Port and context path configuration
- **Database**: Connection pool settings
- **JPA/Hibernate**: ORM configuration
- **Mail**: SMTP settings
- **Batch**: Job configuration
- **Custom**: Business logic configuration (functional, leasing, cmt)

### Dockerfile
Multi-stage build optimized for production:
- **Stage 1**: Maven build with dependency caching
- **Stage 2**: Minimal JRE runtime with security hardening

### .dockerignore
Excludes unnecessary files from Docker build context for faster builds.

## 🐛 Debugging

### View Application Logs

```bash
# Docker logs
docker-compose logs -f backend

# Last 100 lines
docker-compose logs --tail=100 backend

# Save logs to file
docker-compose logs backend > backend.log
```

### Common Issues

**Issue**: Backend can't connect to database
```bash
# Check if MySQL is running
docker-compose ps mysql

# Verify network connectivity
docker-compose exec backend ping mysql

# Check environment variables
docker-compose exec backend env | grep DB_HOST
```

**Issue**: Port 8080 already in use
```bash
# Find process using port
netstat -ano | findstr :8080

# Change port in .env
SERVER_PORT=8081
```

**Issue**: Application won't start
```bash
# Check detailed logs
docker-compose logs backend

# Verify JAR file was built
docker-compose exec backend ls -la /app/

# Rebuild without cache
docker-compose build --no-cache backend
```

## 🧪 Testing

### Run Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=YourTestClass

# Skip tests during build
mvn clean package -DskipTests
```

### Test API Endpoints

```bash
# Health check
curl http://localhost:8080/gBanque/api/actuator/health

# With authentication (if required)
curl -H "Authorization: Bearer YOUR_TOKEN" http://localhost:8080/gBanque/api/your-endpoint
```

## 📦 Building

### Build JAR

```bash
# Clean and build
mvn clean package

# Skip tests for faster build
mvn clean package -DskipTests

# Output: target/gBanque-0.0.1-SNAPSHOT.jar
```

### Build Docker Image

```bash
# From project root
docker-compose build backend

# Without cache
docker-compose build --no-cache backend

# View image
docker images | grep gbanque
```

## 🚢 Deployment

### Development
```bash
docker-compose up -d backend
```

### Production
```bash
# Use production profile
docker-compose -f docker-compose.prod.yml up -d backend

# Or set environment
export SPRING_PROFILES_ACTIVE=prod
docker-compose up -d backend
```

### Environment-Specific Configuration

Create additional application files:
- `application-dev.yml` - Development settings
- `application-prod.yml` - Production settings

Activate with:
```bash
SPRING_PROFILES_ACTIVE=prod
```

## 📊 Monitoring

### Health Check

```bash
curl http://localhost:8080/gBanque/api/actuator/health
```

Response:
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

### Metrics

```bash
curl http://localhost:8080/gBanque/api/actuator/metrics
```

### Application Info

```bash
curl http://localhost:8080/gBanque/api/actuator/info
```

## 🔐 Security

### Non-Root User
Application runs as non-root user `spring` in Docker for enhanced security.

### Database Credentials
- Never commit `.env` file to version control
- Use strong passwords in production
- Rotate credentials regularly

### OAuth2 Authorization Server
Application includes Spring OAuth2 Authorization Server for secure API access.

## 📝 Development Guidelines

### Code Style
- Follow Java coding conventions
- Use meaningful variable names
- Add comments for complex logic
- Keep methods small and focused

### Git Workflow
```bash
# Create feature branch
git checkout -b feature/your-feature

# Commit changes
git add .
git commit -m "feat: your feature description"

# Push to remote
git push origin feature/your-feature
```

### Adding Dependencies

Edit `pom.xml`:
```xml
<dependency>
    <groupId>org.example</groupId>
    <artifactId>your-dependency</artifactId>
    <version>1.0.0</version>
</dependency>
```

Then rebuild:
```bash
mvn clean install
docker-compose build backend
```

## 🔄 Updates & Maintenance

### Update Dependencies

```bash
# Check for updates
mvn versions:display-dependency-updates

# Update parent version
mvn versions:update-parent

# Update dependencies
mvn versions:use-latest-releases
```

### Database Migrations

When schema changes:
1. Create migration SQL file
2. Test locally first
3. Backup production database
4. Apply migration
5. Verify application startup

### Application Updates

```bash
# Pull latest code
git pull origin main

# Rebuild
docker-compose build backend

# Restart with zero downtime (if using multiple instances)
docker-compose up -d --no-deps --build backend
```

## 📖 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Docker Documentation](https://docs.docker.com/)
- [Maven Documentation](https://maven.apache.org/guides/)

## 👥 Team

- **Project**: G-Banque
- **Organization**: Staport Finance
- **Contact**: gbanque@staport.ma

## 📄 License

Internal application - Proprietary

---

**Version**: 0.0.1-SNAPSHOT  
**Last Updated**: December 2025