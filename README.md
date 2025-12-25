# G-Banque Frontend

Angular 17 application with PrimeNG for cash flow management.

## 🚀 Quick Start

### Development

```bash
cd Frontend
npm install
npm start
# Opens at http://localhost:4200
```

### Production (Docker)

```bash
cd G-Banque
docker-compose up -d frontend
# Opens at http://localhost
```

## 📋 Tech Stack

- Angular 17.3.0
- PrimeNG 17.18.15
- TypeScript 5.4.2
- Chart.js 4.4.9
- WebSocket (SockJS + Stomp)

## 📁 Project Structure

```
Frontend/
├── src/
│   ├── app/           # Components, services, models
│   ├── assets/        # Images, fonts
│   ├── environments/  # API configuration
│   └── styles.scss    # Global styles
├── Dockerfile         # Docker build
├── nginx.conf         # Nginx config
└── package.json       # Dependencies
```

## ⚙️ Configuration

**Development API:**
```typescript
// src/environments/environment.ts
apiUrl: 'http://localhost:8080/gBanque/api/'
```

**Production API:**
```typescript
// src/environments/environment.prod.ts
apiUrl: '/gBanque/api/'  // Proxied by Nginx
```

## 🛠️ Commands

```bash
# Install dependencies
npm install

# Start dev server
npm start

# Build for production
npm run build

# Run tests
npm test
```

## 🐳 Docker

```bash
# Build image
docker-compose build frontend

# Start container
docker-compose up -d frontend

# View logs
docker-compose logs -f frontend

# Stop container
docker-compose stop frontend
```

## 🌐 Access URLs

| Service | URL |
|---------|-----|
| Frontend (Dev) | http://localhost:4200 |
| Frontend (Docker) | http://localhost |
| Backend API | http://localhost/gBanque/api |

## 🔧 Common Issues

**Port 4200 in use:**
```bash
ng serve --port 4300
```

**Node modules issue:**
```bash
rm -rf node_modules package-lock.json
npm install
```

**Docker build fails:**
```bash
docker-compose build --no-cache frontend
```

## 📦 Build Output

- **Development**: `dist/gbanque/`
- **Docker**: Served by Nginx from `/usr/share/nginx/html`

## 🎯 Features

- ✅ Angular Material & PrimeNG UI
- ✅ Real-time updates (WebSocket)
- ✅ JWT Authentication
- ✅ Responsive design
- ✅ Charts & Reports
- ✅ Production-ready Docker setup

---

**Version**: 0.0.0  
**Angular**: 17.3.0
