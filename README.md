# 🎶 ZwingBackend

> Servicio backend para **Zwing** — DAW colaborativo en tiempo real para crear beats y secuencias de percusión con otros músicos. Construido con Spring Boot, WebSockets, PostgreSQL y Redis.

---

## 🧮 Tabla de Contenidos

- [Descripción General](#descripción-general)
- [Arquitectura](#arquitectura)
- [Tecnologías](#tecnologías)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [Primeros Pasos](#primeros-pasos)
  - [Requisitos Previos](#requisitos-previos)
  - [Variables de Entorno](#variables-de-entorno)
  - [Ejecución con Docker Compose](#ejecución-con-docker-compose)
  - [Ejecución Local](#ejecución-local)
- [Referencia de API](#referencia-de-api)
  - [Autenticación](#autenticación)
  - [Usuarios](#usuarios)
  - [Proyectos](#proyectos)
  - [Invitaciones](#invitaciones)
  - [Rack y Canales](#rack-y-canales)
  - [Sonidos](#sonidos)
- [Protocolo WebSocket](#protocolo-websocket)
  - [Conexión](#conexión)
  - [Sala de Proyecto](#sala-de-proyecto)
  - [Mensajes del Rack (Entrada)](#mensajes-del-rack-entrada)
  - [Eventos del Rack (Salida)](#eventos-del-rack-salida)
- [Modelo de Dominio](#modelo-de-dominio)
- [Flujo de Autenticación](#flujo-de-autenticación)
- [Modelo de Bloqueos](#modelo-de-bloqueos)
- [Migraciones de Base de Datos](#migraciones-de-base-de-datos)
- [CI/CD](#cicd)
- [Ejecución de Pruebas](#ejecución-de-pruebas)

---

## 📝 Descripción General

ZWING es una aplicación web de producción musical colaborativa que permite a múltiples usuarios trabajar simultáneamente en un mismo proyecto de beat. Cuenta con un **channel rack de 16 pasos**, reproducción sincronizada, bloqueo de canales por usuario, biblioteca de sonidos y un sistema de invitaciones para colaboradores. Este repositorio contiene el servidor de API REST y WebSocket.

Capacidades principales:

- Inicio de sesión con Google OAuth y gestión de sesión mediante JWT (cookies HTTP-only)
- Espacios de trabajo de proyecto con roles de propietario y colaborador
- Sistema de invitaciones por token almacenado en Redis
- Edición del rack en tiempo real a través de WebSockets STOMP con caché optimista en Redis
- Bloqueos de edición por canal y bloqueo de reproducción a nivel de proyecto
- Volcado automático del estado del rack a PostgreSQL cuando el último usuario abandona la sala
- Biblioteca de presets de sonido respaldada por Azure Blob Storage

---

## 🔨 Arquitectura

El proyecto sigue una **Arquitectura Hexagonal** (Puertos y Adaptadores) organizada por contexto delimitado:

```
src/main/java/org/eci/ZwingBackend/
├── auth/               # Autenticación y gestión de usuarios
├── project/            # Espacio de trabajo y colaboración en proyectos
├── rack/               # Rack de canales, canales y bloqueos
├── sound/              # Biblioteca de presets de sonido
└── shared/             # Transversal: eventos, infraestructura WebSocket, configuración
```

Cada contexto delimitado contiene:

```
<contexto>/
├── application/
│   ├── port/in/        # Interfaces de casos de uso
│   ├── port/out/       # Interfaces de repositorios y adaptadores externos
│   └── service/        # Servicios de aplicación (implementaciones de casos de uso)
├── domain/model/       # Objetos de dominio puros
└── infraestructure/
    ├── adapters/out/   # Implementaciones de adaptadores externos
    ├── events/         # Listeners de eventos de Spring
    ├── persistence/    # Entidades JPA, repositorios y mapeadores
    └── web/            # Controladores REST y DTOs
```

La comunicación entre contextos se realiza exclusivamente a través del `ApplicationEventPublisher` de Spring — no hay llamadas directas de servicio a servicio entre contextos delimitados.

---

## 👩🏼‍💻 Tecnologías

| Capa | Tecnología |
|---|---|
| Runtime | Java 21 |
| Framework | Spring Boot 3.5 |
| Web | Spring MVC + Spring WebSocket (STOMP) |
| Seguridad | Spring Security + JWT (jjwt 0.12) |
| OAuth | Google Identity (google-api-client) |
| Base de Datos | PostgreSQL 15 |
| ORM | Spring Data JPA / Hibernate |
| Migraciones | Flyway |
| Caché / Bloqueos | Redis 7 |
| Build | Maven 3.9 |
| Contenedores | Docker + Docker Compose |
| CI/CD | GitHub Actions → Azure Container Registry → Azure App Service |

---

## 📁 Estructura del Proyecto

```
.
├── .github/workflows/
│   ├── ci.yml              # Build + pruebas en develop / PR a main
│   └── cd.yml              # Build de imagen Docker + despliegue a Azure en main
├── src/
│   ├── main/
│   │   ├── java/org/eci/ZwingBackend/
│   │   └── resources/
│   │       ├── application.properties
│   │       └── db/migration/   # Migraciones SQL de Flyway
│   └── test/
├── docker-compose.yml
├── Dockerfile
└── pom.xml
```

---

## 🩴 Primeros Pasos

### 🪈 Requisitos Previos

- Java 21
- Maven 3.9+
- Docker y Docker Compose
- Un Client ID de Google OAuth 2.0
- Un secreto JWT (mínimo 32 caracteres recomendado)

### 🪗 Variables de Entorno

Crea un archivo `.env` en la raíz del proyecto (está incluido en el `.gitignore`):

```env
# Requeridas
JWT_SECRET=tu_secreto_jwt_aqui
GOOGLE_CLIENT_ID=tu_google_client_id.apps.googleusercontent.com

# Sobreescrituras opcionales (se muestran los valores por defecto)
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/zwing_db
SPRING_DATASOURCE_USERNAME=admin
SPRING_DATASOURCE_PASSWORD=rootpassword
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
```

### 🐋 Ejecución con Docker Compose

Levanta el backend, PostgreSQL y Redis juntos:

```bash
docker compose up --build
```

La API estará disponible en `http://localhost:8080`.

### 🖥️ Ejecución Local

Inicia solo las dependencias de infraestructura:

```bash
docker compose up db redis -d
```

Luego ejecuta la aplicación:

```bash
./mvnw spring-boot:run
```

---

## 🥤 Referencia de API

Todos los endpoints protegidos requieren una cookie HTTP-only `jwt_token` válida obtenida desde `/auth/google`. El filtro de seguridad inyecta automáticamente los headers `X-User-Id` y `X-User-email` tras validar el JWT — los controladores leen desde estos headers en lugar del token crudo.

### 🔒 Autenticación

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| `POST` | `/auth/google` | Público | Intercambia un Google ID token por una cookie de sesión |
| `POST` | `/auth/logout` | Cookie | Agrega el token actual a la lista negra y borra la cookie |

**POST /auth/google** — Cuerpo de la solicitud:
```json
{ "idToken": "<google_id_token>" }
```

Cuerpo de la respuesta (el token se establece como cookie HTTP-only, no se retorna en el JSON):
```json
{
  "status": "Success",
  "message": "Successful Log In",
  "data": { "name": "Juan Pérez", "email": "juan@ejemplo.com", "newUser": false }
}
```

---

### 👥 Usuarios

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| `GET` | `/api/users/me` | Cookie | Obtiene el usuario autenticado actualmente |
| `GET` | `/api/users/lookup?email=` | Cookie | Resuelve el UUID de un usuario por su email |
| `DELETE` | `/api/users/me` | Cookie | Elimina la cuenta del usuario actual |

---

### 🎼 Proyectos

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| `GET` | `/api/projects` | Cookie | Obtiene los proyectos propios y en los que colabora |
| `GET` | `/api/projects/{projectId}` | Cookie | Obtiene un proyecto (solo miembros) |
| `POST` | `/api/projects` | Cookie | Crea un nuevo proyecto |
| `DELETE` | `/api/projects/{projectId}` | Cookie | Elimina un proyecto (solo el propietario) |
| `PUT` | `/api/projects/{projectId}/collaborators/{email}` | Cookie | Agrega un colaborador por email |
| `DELETE` | `/api/projects/{projectId}/collaborators/{collaboratorId}` | Cookie | Elimina un colaborador (solo el propietario) |

**POST /api/projects** — Cuerpo de la solicitud:
```json
{ "name": "Mi Beat" }
```

---

### ✉️ Invitaciones

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| `POST` | `/api/invites` | Cookie | Crea un token de invitación para un proyecto (solo el propietario) |
| `POST` | `/api/invites/accept?token=` | Cookie | Acepta una invitación y se une al proyecto |

**POST /api/invites** — Cuerpo de la solicitud:
```json
{ "projectId": "<uuid>", "inviteeEmail": "colaborador@ejemplo.com" }
```

Los tokens de invitación se almacenan en Redis con un TTL de 48 horas.

---

### 🎚️ Rack y Canales

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| `GET` | `/api/rack/{projectId}` | Cookie | Carga el estado completo del rack |
| `POST` | `/api/rack/{projectId}/channels` | Cookie | Agrega un canal al rack |
| `PUT` | `/api/rack/{projectId}/channels/{channelId}` | Cookie | Actualiza las propiedades de un canal |
| `DELETE` | `/api/rack/{projectId}/channels/{channelId}` | Cookie | Elimina un canal |

**POST /api/rack/{projectId}/channels** — Cuerpo de la solicitud:
```json
{ "name": "Kick 1", "soundId": "<uuid>" }
```

**PUT /api/rack/{projectId}/channels/{channelId}** — Cuerpo de la solicitud:
```json
{ "name": "Kick 1", "soundId": "<uuid>", "volume": 0.8, "active": true }
```

> **Nota:** En sesiones colaborativas, la mayoría de las mutaciones del rack deben realizarse a través del protocolo WebSocket para que se propaguen en tiempo real. Los endpoints REST están disponibles para uso programático o no interactivo.

---

### 🎵 Sonidos

| Método | Ruta | Auth | Descripción |
|---|---|---|---|
| `GET` | `/api/sounds` | Cookie | Lista todos los presets de sonido |
| `GET` | `/api/sounds?category=KICK` | Cookie | Filtra por categoría |
| `GET` | `/api/sounds/{soundId}` | Cookie | Obtiene un preset de sonido específico |

Categorías disponibles: `KICK`, `SNARE`, `HIHAT`, `CLAP`, `SYNTH`, `SAMPLE`

---

## 🛰️ Protocolo WebSocket

Zwing utiliza **STOMP sobre SockJS**. La autenticación ocurre en la capa de handshake del WebSocket — la misma cookie `jwt_token` usada para REST se valida durante la actualización HTTP → WebSocket. No es necesario enviar el token en los frames STOMP.

### 🔌 Conexión

**Endpoint:** `ws://localhost:8080/ws` (con soporte de fallback SockJS)

```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stomp  = Stomp.over(socket);
stomp.connect({}, onConnected);
```

### 🪇 Sala de Proyecto

Luego de conectar, suscríbete al topic de presencia y envía el mensaje de ingreso:

```javascript
// Suscripciones
stomp.subscribe(`/topic/project/${projectId}/presence`, onPresence);
stomp.subscribe(`/user/queue/errors`, onError);

// Ingresar a la sala
stomp.send(`/app/project/${projectId}/join`, {}, '');

// Salir de la sala (enviar antes de desconectar)
stomp.send(`/app/project/${projectId}/leave`, {}, '');
```

**Estructura del mensaje de presencia:**
```json
{ "userId": "...", "email": "...", "status": "JOINED", "projectName": "Mi Beat" }
```

### 📨 Mensajes del Rack (Entrada)

Suscríbete al topic del rack de tu proyecto:

```javascript
stomp.subscribe(`/topic/rack/${projectId}`, onRackEvent);
stomp.subscribe(`/user/queue/rack/state`, onRackState); // personal: respuesta de carga del rack
```

| Destino | Payload | Descripción |
|---|---|---|
| `/app/rack/{projectId}/load` | _(ninguno)_ | Solicita el estado actual del rack |
| `/app/rack/{projectId}/channel/add` | `{ "name": "...", "soundId": "..." }` | Agrega un canal |
| `/app/rack/{projectId}/channel/{channelId}/remove` | _(ninguno)_ | Elimina un canal |
| `/app/rack/{projectId}/channel/{channelId}/step` | `{ "stepIndex": 3 }` | Alterna un paso en la cuadrícula de 16 pasos |
| `/app/rack/{projectId}/channel/{channelId}/update` | `{ "name", "soundId", "volume", "active" }` | Actualiza propiedades del canal |
| `/app/rack/{projectId}/channel/{channelId}/lock` | _(ninguno)_ | Adquiere el bloqueo de edición de un canal |
| `/app/rack/{projectId}/channel/{channelId}/unlock` | _(ninguno)_ | Libera el bloqueo de edición |
| `/app/rack/{projectId}/bpm/update` | `{ "bpm": 130 }` | Actualiza el BPM (limitado entre 40 y 240) |
| `/app/rack/{projectId}/playback/start` | _(ninguno)_ | Adquiere el bloqueo de reproducción |
| `/app/rack/{projectId}/playback/stop` | _(ninguno)_ | Libera el bloqueo de reproducción |

### 📣 Eventos del Rack (Salida)

Todos los eventos emitidos a `/topic/rack/{projectId}` comparten el mismo envoltorio:

```json
{
  "type": "CHANNEL_ADDED",
  "payload": { ... },
  "triggeredBy": "<userId>"
}
```

| `type` | Payload | Destino |
|---|---|---|
| `RACK_STATE` | Objeto `ChannelRack` completo | `/user/queue/rack/state` (personal) |
| `CHANNEL_ADDED` | Objeto `Channel` | Broadcast |
| `CHANNEL_REMOVED` | `{ "channelId": "..." }` | Broadcast |
| `CHANNEL_UPDATED` | Objeto `Channel` | Broadcast |
| `STEP_TOGGLED` | `{ "channelId", "stepIndex", "newValue" }` | Broadcast |
| `BPM_UPDATED` | `{ "bpm": 130 }` | Broadcast |
| `CHANNEL_LOCKED` | `{ "channelId", "lockedByUserId", "lockedByEmail" }` | Broadcast |
| `CHANNEL_UNLOCKED` | `{ "channelId" }` | Broadcast |
| `PLAYBACK_STARTED` | `{ "startedBy": "<userId>" }` | Broadcast |
| `PLAYBACK_STOPPED` | `{ "stoppedBy": "<userId>" }` | Broadcast |
| `USER_DISCONNECTED` | `{ "userId": "..." }` | Broadcast |
| `ERROR` | Mensaje de error (string) | `/user/queue/errors` (personal) |

---

## 📦 Modelo de Dominio

```
Usuario
 └── posee / colabora en → Proyecto
                             └── tiene uno → ChannelRack
                                              ├── bpm: Int
                                              └── canales: Canal[]
                                                            ├── soundId → PresetDeSonido
                                                            ├── pasos: boolean[16]
                                                            ├── volumen: float
                                                            └── activo: boolean
```

- Un `Proyecto` siempre tiene exactamente un `ChannelRack`, creado de forma atómica al momento de crear el proyecto.
- El estado en vivo del rack se mantiene en Redis durante una sesión activa y se vuelca a PostgreSQL cuando el último usuario se desconecta.
- Los registros de `PresetDeSonido` almacenan URLs de audio que apuntan a Azure Blob Storage. El backend nunca hace proxy del audio — el frontend lo obtiene directamente desde la URL del CDN.

---

## 🌊 Flujo de Autenticación

```
Frontend                    Backend                    Google
   │                           │                          │
   │── POST /auth/google ──────►│                          │
   │   { idToken }             │── verificar token ───────►│
   │                           │◄─ payload (sub/email) ────│
   │                           │
   │                           │  upsert Usuario en PostgreSQL
   │                           │  generar JWT interno
   │
   │◄── Set-Cookie: jwt_token ─│
   │    (HttpOnly, Secure,      │
   │     SameSite=None)         │
```

- El JWT contiene los claims `sub` (email) y `userId`.
- Al cerrar sesión, el token se agrega a una lista negra en Redis con un TTL de 24 horas.
- Al eliminar una cuenta, el `userId` es bloqueado para invalidar inmediatamente todos los tokens existentes de ese usuario.
- La autenticación WebSocket reutiliza la misma cookie, validada una única vez durante el handshake.

---

## 🔒 Modelo de Bloqueos

Zwing utiliza dos tipos de bloqueos respaldados por Redis para coordinar ediciones concurrentes:

**Bloqueo de Canal** — Por canal, TTL de 30 segundos (expira automáticamente ante caídas)
- Clave: `channel_lock:{projectId}:{channelId}` → `userId`
- Un usuario debe tener el bloqueo para modificar el `nombre` o el `soundId` de un canal.
- El volumen y el silencio (`active`) pueden modificarse sin tener el bloqueo.
- Los bloqueos se liberan explícitamente al desbloquear, al eliminar el canal o al desconectarse el usuario.

**Bloqueo de Reproducción** — A nivel de proyecto, TTL de seguridad de 5 minutos
- Clave: `playback_lock:{projectId}` → `userId`
- Mientras está activo, todas las mutaciones de canal (agregar, eliminar, actualizar, alternar pasos) quedan bloqueadas.
- Se libera explícitamente al detener la reproducción, o automáticamente cuando el titular del bloqueo se desconecta.

Ambos tipos se adquieren atómicamente usando Redis `SET NX`, evitando condiciones de carrera entre múltiples instancias del servidor.

---

## 🐘 Migraciones de Base de Datos

Flyway gestiona el esquema en `src/main/resources/db/migration/`:

| Versión | Descripción |
|---|---|
| V1 | Tablas `users`, `projects`, `project_collaborators` |
| V2 | Tablas `sound_presets`, `channel_racks`, `channels`; datos iniciales de sonidos |
| V3 | Migración a URLs actualizadas de presets; preservación de referencias de canales existentes |

Las migraciones se ejecutan automáticamente al iniciar la aplicación. El esquema se valida (`ddl-auto=validate`) — Hibernate nunca modifica el esquema directamente.

---

## 🎠 CI/CD

**CI** — Se activa en push a `develop` y en pull requests a `main`:

1. Levanta PostgreSQL 15 y Redis 7 como contenedores de servicio.
2. Ejecuta `mvn clean verify` (compila, pruebas unitarias e integración).
3. Construye la imagen Docker para verificar que el `Dockerfile` es válido.

**CD** — Se activa en push a `main`:

1. Construye y etiqueta la imagen Docker con el SHA del commit.
2. Hace push a Azure Container Registry.
3. Despliega la imagen etiquetada en Azure App Service.

Secrets requeridos en GitHub:

| Secret | Descripción |
|---|---|
| `JWT_SECRET` | Secreto de firma JWT |
| `GOOGLE_CLIENT_ID` | Client ID de Google OAuth |
| `ACR_LOGIN_SERVER` | Servidor de inicio de sesión de Azure Container Registry |
| `ACR_USERNAME` | Usuario de ACR |
| `ACR_PASSWORD` | Contraseña de ACR |
| `AZURE_CREDENTIALS` | JSON del service principal de Azure |
| `AZURE_APP_NAME` | Nombre del Azure App Service |

---

## 🧪 Ejecución de Pruebas

```bash
# Pruebas unitarias e integración (requiere PostgreSQL y Redis en ejecución)
./mvnw clean verify

# Solo build, sin pruebas
./mvnw clean package -DskipTests
```

El pipeline de CI provee instancias efímeras de PostgreSQL y Redis automáticamente. Para ejecución local, inicia primero la infraestructura con `docker compose up db redis -d`.